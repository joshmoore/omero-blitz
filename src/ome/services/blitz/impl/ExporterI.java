/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.blitz.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.xml.transform.TransformerException;

import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import ome.api.IQuery;
import ome.api.RawPixelsStore;
import ome.conditions.ApiUsageException;
import ome.conditions.InternalException;
import ome.services.blitz.util.BlitzExecutor;
import ome.services.blitz.util.BlitzOnly;
import ome.services.blitz.util.ServiceFactoryAware;
import ome.services.blitz.util.UnregisterServantMessage;
import ome.services.db.DatabaseIdentity;
import ome.services.formats.OmeroReader;
import ome.services.util.Executor;
import ome.system.ServiceFactory;
import ome.util.messages.InternalMessage;
import ome.xml.DOMUtil;
import ome.xml.OMEXMLNode;
import omero.ServerError;
import omero.api.AMD_Exporter_addImage;
import omero.api.AMD_Exporter_generateTiff;
import omero.api.AMD_Exporter_generateXml;
import omero.api.AMD_Exporter_getBytes;
import omero.api.AMD_StatefulServiceInterface_activate;
import omero.api.AMD_StatefulServiceInterface_close;
import omero.api.AMD_StatefulServiceInterface_getCurrentEventContext;
import omero.api.AMD_StatefulServiceInterface_passivate;
import omero.api._ExporterOperations;
import omero.model.Image;
import omero.model.ImageI;
import omero.model.Pixels;
import omero.util.IceMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

import Ice.Current;

/**
 * Implementation of the Exporter service. This class uses a simple state
 * machine.
 * 
 * <pre>
 *  START -&gt; waiting -&gt; config -&gt; output -&gt; waiting -&gt; config ...
 * </pre>
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 4.1
 */
public class ExporterI extends AbstractAmdServant implements
        _ExporterOperations, ServiceFactoryAware, BlitzOnly {

    private final static Log log = LogFactory.getLog(ExporterI.class);

    private final static int MAX_SIZE = 1024 * 1024;

    /**
     * Utility enum for asserting the state of Exporter instances.
     */
    private enum State {
        config, output, waiting;
        static State check(ExporterI self) {

            if (self.file != null && self.retrieve != null) {
                throw new InternalException("Doing 2 things at once");
            }

            if (self.retrieve != null) {
                return config;
            }

            if (self.file != null) {
                return self.offset < 0 ? waiting : output;
            }

            return waiting;
        }
    }

    private/* final */ServiceFactoryI factory;

    private volatile OmeroMetadata retrieve;

    /**
     * Reference to the temporary file which is currently being output. If null,
     * then no generate method has been called.
     */
    private volatile File file;

    /**
     * Offset into the file which we are currently reading. This field. is only
     * valid when the file field is non-null. If it is less than zero, then the
     * current file has been completely read.
     */
    private volatile long offset = 0;

    /**
     * Encapsulates the logic for creating new LSIDs and comparing existing ones
     * to the internal value for this DB.
     */
    private final DatabaseIdentity databaseIdentity;

    public ExporterI(BlitzExecutor be, DatabaseIdentity databaseIdentity) {
        super(null, be);
        this.databaseIdentity = databaseIdentity;
    }

    public void setServiceFactory(ServiceFactoryI sf) throws ServerError {
        this.factory = sf;
    }

    // Interface methods
    // =========================================================================

    public void addImage_async(AMD_Exporter_addImage __cb, final long id,
            Current __current) throws ServerError {

        State state = State.check(this);
        ServerError se = assertConfig(state);
        if (se != null) {
            __cb.ice_exception(se);
        }

        retrieve.addImage(new ImageI(id, false));
        __cb.ice_response();
        return;

    }

    /**
     * Generate XML and return the length
     */
    public void generateXml_async(AMD_Exporter_generateXml __cb,
            Current __current) throws ServerError {

        State state = State.check(this);
        ServerError se = assertConfig(state);
        if (se != null) {
            __cb.ice_exception(se);
        }
        // sets retrieve, then file, then unsets retrieve
        do_xml(__cb);
        return;
    }

    /**
     * 
     */
    public void generateTiff_async(AMD_Exporter_generateTiff __cb,
            Current __current) throws ServerError {

        State state = State.check(this);
        ServerError se = assertConfig(state);
        if (se != null) {
            __cb.ice_exception(se);
        }
        // sets retrieve, then file, then unsets retrieve
        do_tiff(__cb);
        return;
    }

    public void getBytes_async(AMD_Exporter_getBytes __cb, int size,
            Current __current) throws ServerError {

        State state = State.check(this);
        switch (state) {
        case waiting:
            __cb.ice_response(new byte[] {});
            return;
        case config:
            omero.ApiUsageException aue = new omero.ApiUsageException(null,
                    null, "Call a generate method first");
            __cb.ice_exception(aue);
            return;
        case output:
            try {
                __cb.ice_response(read(size));
            } catch (Exception e) {
                omero.InternalException ie = new omero.InternalException(null,
                        null, "Error during read");
                IceMapper.fillServerError(ie, e);
                __cb.ice_exception(ie);
            }
            return;
        default:
            throw new InternalException("Unknown state: " + state);
        }
    }

    // State methods
    // =========================================================================

    /**
     * Transition from waiting to config
     */
    private ServerError assertConfig(State state) {
        switch (state) {
        case waiting:
            startConfig(); // Transitions to config, so fall through
        case config:
            return null;
        case output:
            return new omero.ApiUsageException(null, null,
                    "Cannot configure during output");
        default:
            return new omero.InternalException(null, null, "Unknown state: "
                    + state);
        }
    }

    /**
     * Transition from waiting to config
     */
    private void startConfig() {
        if (file != null) {
            file.delete();
            file = null;
        }
        retrieve = new OmeroMetadata(databaseIdentity);
        offset = 0;
    }

    /**
     * Transitions from config to output.
     */
    private void do_xml(final AMD_Exporter_generateXml __cb) {
        try {
            factory.executor.execute(factory.principal,
                    new Executor.SimpleWork(this, "generateXml") {
                        @Transactional(readOnly = true)
                        public Object doWork(Session session, ServiceFactory sf) {
                            retrieve.initialize(session);
                            IMetadata xmlMetadata = convertXml(retrieve);
                            if (xmlMetadata != null) {
                                Object root = xmlMetadata.getRoot();
                                if (root instanceof OMEXMLNode) {
                                    OMEXMLNode node = (OMEXMLNode) root;

                                    try {

                                        file = File.createTempFile(
                                                "__omero_export__", ".ome.xml");
                                        file.deleteOnExit();
                                        FileOutputStream fos = new FileOutputStream(
                                                file);
                                        DOMUtil.writeXML(fos, node
                                                .getDOMElement()
                                                .getOwnerDocument());
                                        fos.close();
                                        retrieve = null;
                                        offset = 0;
                                        __cb.ice_response(file.length());
                                        return null; // ONLY VALID EXIT

                                    } catch (IOException ioe) {
                                        log.error(ioe);

                                    } catch (TransformerException e) {
                                        log.error(e);
                                    }

                                }
                            }
                            return null;
                        }
                    });
        } catch (Exception e) {
            omero.InternalException ie = new omero.InternalException();
            IceMapper.fillServerError(ie, e);
            __cb.ice_exception(ie);
        }
    }

    /**
     * Transitions from config to output.
     */
    private void do_tiff(final AMD_Exporter_generateTiff __cb) {
        try {
            factory.executor.execute(factory.principal,
                    new Executor.SimpleWork(this, "generateTiff") {
                        @Transactional(readOnly = true)
                        public Object doWork(Session session, ServiceFactory sf) {
                            retrieve.initialize(session);

                            int num = retrieve.sizeImages();
                            if (num != 1) {
                                omero.ApiUsageException a = new omero.ApiUsageException(
                                        null, null,
                                        "Only one image supported for TIFF, not "+num);
                                __cb.ice_exception(a);
                                return null;
                            }

                            RawPixelsStore raw = null;
                            OmeroReader reader = null;
                            ImageWriter writer = null;
                            try {

                                Image image = retrieve.getImage(0);
                                Pixels pix = image.getPixels(0);

                                file = File.createTempFile("__omero_export__",
                                        ".ome.tiff");
                                file.deleteOnExit();

                                raw = sf.createRawPixelsStore();
                                raw.setPixelsId(pix.getId().getValue(), true);
                                
                                reader = new OmeroReader(raw, pix);
                                reader.setId("OMERO");
                                
                                writer = new ImageWriter();
                                writer.setMetadataRetrieve(retrieve);
                                writer.setId(file.getAbsolutePath());

                                int planeCount = reader.planes;
                                int planeSize = raw.getPlaneSize();
                                byte[] plane = new byte[planeSize];
                                for (int i = 0; i < planeCount; i++) {
                                    reader.openBytes(i, plane);
                                    writer
                                            .saveBytes(plane,
                                                    i == planeCount - 1);
                                }
                                retrieve = null;
                            } catch (Exception e) {
                                omero.InternalException ie = new omero.InternalException(
                                        null, null,
                                        "Error during TIFF generation");
                                IceMapper.fillServerError(ie, e);
                                __cb.ice_exception(ie);
                                return null;
                            } finally {
                                cleanup(raw, reader, writer);
                            }

                            __cb.ice_response(file.length());
                            return null; // ONLY VALID EXIT
                        }

                        private void cleanup(RawPixelsStore raw,
                                OmeroReader reader, ImageWriter writer) {
                            try {
                                if (raw != null) {
                                    raw.close();
                                }
                            } catch (Exception e) {
                                log.error("Error closing pix", e);
                            }
                            try {
                                if (reader != null) {
                                    reader.close();
                                }
                            } catch (Exception e) {
                                log.error("Error closing reader", e);
                            }
                            try {
                                if (writer != null) {
                                    writer.close();
                                }
                            } catch (Exception e) {
                                log.error("Error closing writer", e);
                            }
                        }
                    });
        } catch (Exception e) {
            omero.InternalException ie = new omero.InternalException();
            IceMapper.fillServerError(ie, e);
            __cb.ice_exception(ie);
        }
    }

    /**
     * Read size bytes, and transition to "waiting" If any exception is thrown,
     * the offset for the current file will not be updated.
     */
    private byte[] read(int size) {
        if (size > MAX_SIZE) {
            throw new ApiUsageException("Max read size is: " + MAX_SIZE);
        }

        byte[] buf = new byte[size];

        RandomAccessFile ra = null;
        try {
            ra = new RandomAccessFile(file, "r");
            ra.seek(offset);
            int read = ra.read(buf);

            // Handle end of file
            if (read < 0) {
                offset = read; // Transition to waiting
            } else if (read < size) {
                offset = -1; // Transition to waiting
                byte[] newBuf = new byte[read];
                System.arraycopy(buf, 0, newBuf, 0, read);
                buf = newBuf;
            } else {
                // This should be fairly unlikely, but if the last read
                // brought us to the end of the file, then we should go
                // ahead and reset so that the next call doesn't block.
                if ((offset + read) == ra.length()) {
                    offset = -2; // Transition to waiting
                } else {
                    offset += read;
                }
            }

        } catch (IOException io) {
            throw new RuntimeException(io);
        } finally {

            if (ra != null) {
                try {
                    ra.close();
                } catch (IOException e) {
                    log.warn("IOException on file close");
                }
            }

        }

        return buf;
    }

    // XML Generation (public for testing)
    // =========================================================================

    public static IMetadata convertXml(MetadataRetrieve retrieve) {
        try {
            IMetadata xmlMeta = MetadataTools.createOMEXMLMetadata();
            xmlMeta.createRoot();
            MetadataTools.convertMetadata(retrieve, xmlMeta);
            return xmlMeta;
        } catch (ClassCastException cce) {
            return null;
        }
    }

    public static String generateXml(MetadataRetrieve retrieve) {
        IMetadata xmlMeta = convertXml(retrieve);
        return MetadataTools.getOMEXML(xmlMeta);
    }

    // Stateful interface methods
    // =========================================================================

    public void activate_async(AMD_StatefulServiceInterface_activate __cb,
            Current __current) {
        // Do nothing for the moment
    }

    public void passivate_async(AMD_StatefulServiceInterface_passivate __cb,
            Current __current) {
        // Do nothing for the moment
    }

    public void close_async(AMD_StatefulServiceInterface_close __cb,
            Current __current) {

        try {
            retrieve = null;
            if (file != null) {
                file.delete();
                file = null;
            }
    
            InternalMessage msg = new UnregisterServantMessage(this,
                    factory.principal.getName(), __current);
            factory.context.publishEvent(msg);
            
            __cb.ice_response();
        } catch (Exception e) {
            __cb.ice_exception(e);
        }
        
    }

    public void getCurrentEventContext_async(
            AMD_StatefulServiceInterface_getCurrentEventContext __cb,
            Current __current) throws ServerError {
        callInvokerOnRawArgs(__cb, __current);

    }
}
