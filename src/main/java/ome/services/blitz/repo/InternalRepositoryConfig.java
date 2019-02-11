/*
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.blitz.repo;

import ome.services.blitz.fire.Registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple configuration class which is created via repository.xml Spring
 * configuration file after the OMERO.repository context is loaded. Used as a
 * factory for creating the InternalRepository, similar to BlitzConfiguration.
 *
 * @since Beta4.2
 */
public class InternalRepositoryConfig {

    private final static Logger log = LoggerFactory
            .getLogger(InternalRepositoryConfig.class);

    private final com.zeroc.Ice.InitializationData id;

    private final com.zeroc.Ice.Communicator ic;

    private final com.zeroc.Ice.ObjectAdapter oa;

    private final Registry reg;

    public InternalRepositoryConfig(String repoDir) throws Exception {

        //
        // Ice Initialization
        //
        id = new com.zeroc.Ice.InitializationData();
        id.properties = com.zeroc.Ice.Util.createProperties();
        String ICE_CONFIG = System.getProperty("ICE_CONFIG");
        if (ICE_CONFIG != null) {
            id.properties.load(ICE_CONFIG);
        }
        ic = com.zeroc.Ice.Util.initialize(id);

        reg = new Registry.Impl(ic);
        oa = ic.createObjectAdapter("RepositoryAdapter");
        oa.activate();

        /*
         * String serverId =
         * ic.getProperties().getProperty("Ice.Admin.ServerId"); Ice.Identity id
         * = Ice.Util.stringToIdentity(serverId);
         */

    }

    public com.zeroc.Ice.Communicator getCommunicator() {
        return ic;
    }

    public com.zeroc.Ice.ObjectAdapter getObjectAdapter() {
        return oa;
    }

    public Registry getRegistry() {
        return reg;
    }

}
