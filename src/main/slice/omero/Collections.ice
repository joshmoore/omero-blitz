/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef OMERO_COLLECTIONS_ICE
#define OMERO_COLLECTIONS_ICE

#include <omero/model/Annotation.ice>
#include <omero/model/Experimenter.ice>
#include <omero/model/ExperimenterGroup.ice>
#include <omero/model/Event.ice>
#include <omero/model/EventLog.ice>
#include <omero/model/Session.ice>
#include <omero/model/Project.ice>
#include <omero/model/Dataset.ice>
#include <omero/model/Image.ice>
#include <omero/model/LogicalChannel.ice>
#include <omero/model/OriginalFile.ice>
#include <omero/model/Pixels.ice>
#include <omero/model/PixelsType.ice>
#include <omero/model/Roi.ice>
#include <omero/model/ScriptJob.ice>
#include <omero/model/Shape.ice>
#include <omero/model/ChecksumAlgorithm.ice>

#include <omero/SystemF.ice>
#include <omero/RTypes.ice>
#include <Ice/BuiltinSequences.ice>

/*
 * Defines various sequences and dictionaries used throughout
 * the OMERO API. Defining all of these in one central location
 * increases reuse and keeps the library sizes as small as possible.
 *
 * Some collections cannot be defined here since some types are not
 * yet defined.
 */
module omero {

    module api {

        // Forward definition (used in sequences)

        dictionary<string, omero::model::Annotation> SearchMetadata;

        //
        // Object lists
        //

        ["java:type:java.util.ArrayList"]
            sequence<SearchMetadata> SearchMetadataList;

        ["java:type:java.util.ArrayList<omero.model.Experimenter>:java.util.List<omero.model.Experimenter>"]
            sequence<omero::model::Experimenter> ExperimenterList;

        ["java:type:java.util.ArrayList<omero.model.ExperimenterGroup>:java.util.List<omero.model.ExperimenterGroup>"]
            sequence<omero::model::ExperimenterGroup> ExperimenterGroupList;

        ["java:type:java.util.ArrayList<omero.model.Event>:java.util.List<omero.model.Event>"]
            sequence<omero::model::Event> EventList;

        ["java:type:java.util.ArrayList<omero.model.EventLog>:java.util.List<omero.model.EventLog>"]
            sequence<omero::model::EventLog> EventLogList;

        ["java:type:java.util.ArrayList<omero.model.Annotation>:java.util.List<omero.model.Annotation>"]
            sequence<omero::model::Annotation> AnnotationList;

        ["java:type:java.util.ArrayList<omero.model.Session>:java.util.List<omero.model.Session>"]
            sequence<omero::model::Session> SessionList;

        ["java:type:java.util.ArrayList<omero.model.Project>:java.util.List<omero.model.Project>"]
            sequence<omero::model::Project> ProjectList;

        ["java:type:java.util.ArrayList<omero.model.Dataset>:java.util.List<omero.model.Dataset>"]
            sequence<omero::model::Dataset> DatasetList;

        ["java:type:java.util.ArrayList<omero.model.Image>:java.util.List<omero.model.Image>"]
            sequence<omero::model::Image> ImageList;

        ["java:type:java.util.ArrayList<omero.model.LogicalChannel>:java.util.List<omero.model.LogicalChannel>"]
            sequence<omero::model::LogicalChannel> LogicalChannelList;

        ["java:type:java.util.ArrayList<omero.model.OriginalFile>:java.util.List<omero.model.OriginalFile>"]
            sequence<omero::model::OriginalFile> OriginalFileList;

        ["java:type:java.util.ArrayList<omero.model.Pixels>:java.util.List<omero.model.Pixels>"]
            sequence<omero::model::Pixels> PixelsList;

        ["java:type:java.util.ArrayList<omero.model.PixelsType>:java.util.List<omero.model.PixelsType>"]
            sequence<omero::model::PixelsType> PixelsTypeList;

        ["java:type:java.util.ArrayList<omero.model.Roi>:java.util.List<omero.model.Roi>"]
            sequence<omero::model::Roi> RoiList;

        ["java:type:java.util.ArrayList<omero.model.ScriptJob>:java.util.List<omero.model.ScriptJob>"]
            sequence<omero::model::ScriptJob> ScriptJobList;

        ["java:type:java.util.ArrayList<omero.model.Shape>:java.util.List<omero.model.Shape>"]
            sequence<omero::model::Shape> ShapeList;

        ["java:type:java.util.ArrayList<omero.model.ChecksumAlgorithm>:java.util.List<omero.model.ChecksumAlgorithm>"]
            sequence<omero::model::ChecksumAlgorithm> ChecksumAlgorithmList;

        ["java:type:java.util.ArrayList<omero.sys.EventContext>:java.util.List<omero.sys.EventContext>"]
        sequence<omero::sys::EventContext> EventContextList;


        // Dictionaries

        dictionary<long,   omero::model::Pixels>       LongPixelsMap;
        dictionary<string, omero::model::Experimenter> UserMap;
        dictionary<string, omero::model::OriginalFile> OriginalFileMap;


        // Multimaps (dictionaries with sequence values)

        dictionary<string, ShapeList>                  StringShapeListMap;
        dictionary<long,   ShapeList>                  LongShapeListMap;
        dictionary<int,    ShapeList>                  IntShapeListMap;
        dictionary<long,   AnnotationList>             LongAnnotationListMap;

         dictionary<bool,   omero::sys::LongList>       BooleanIdListMap;

    };

};

#endif
