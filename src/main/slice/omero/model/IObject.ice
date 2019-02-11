/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef CLASS_IOBJECT
#define CLASS_IOBJECT

#include <omero/RTypes.ice>
#include <omero/ModelF.ice>
#include <omero/System.ice>
#include <Ice/Current.ice>

module omero {
  module model {
    class Details;

    /**
     * Base class of all model types. On the
     * server, the interface ome.model.IObject
     * unifies the model. In Ice, interfaces have
     * a more remote connotation.
     **/
    ["protected"] class IObject
    {
      /**
       * The database id for this entity. Of RLong value
       * so that transient entities can have a null id.
       **/
      omero::RLong          id;

      /**
       * Internal details (permissions, owner, etc.) for
       * this entity. All entities have Details, and even
       * a newly created object will have a non-null
       * Details instance. (In the OMERO provided mapping!)
       **/
      omero::model::Details details;

      /**
       * An unloaded object contains no state other than id. An
       * exception will be raised if any field other than id is
       * accessed via the OMERO-generated methods. Unloaded objects
       * are useful as pointers or proxies to server-side state.
       **/
      bool loaded;

      // METHODS
      // =====================================================

      // Accessors

      omero::RLong getId();

      void setId(omero::RLong id);

      omero::model::Details getDetails();

      /**
       * Return another instance of the same type as this instance
       * constructed as if by: new InstanceI( this.id.val, false );
       **/
      IObject proxy();

      /**
       * Return another instance of the same type as this instance
       * with all single-value entities unloaded and all members of
       * collections also unloaded.
       **/
      IObject shallowCopy();

      /**
       * Sets the loaded boolean to false and empties all state
       * from this entity to make sending it over the network
       * less costly.
       **/
      void unload();

      /**
       * Each collection can also be unloaded, independently
       * of the object itself. To unload all collections, use:
       *
       *    object.unloadCollections();
       *
       * This is useful when it is possible that a collection no
       * longer represents the state in the database, and passing the
       * collections back to the server might delete some entities.
       *
       * Sending back empty collections can also save a significant
       * amount of bandwidth, when working with large data graphs.
       **/
      void unloadCollections();

      /**
       * As with collections, the objects under details can link
       * to many other objects. Unloading the details can same
       * bandwidth and simplify the server logic.
       **/
      void unloadDetails();

      /**
       * Tests for unloadedness. If this value is false, then
       * any method call on this instance other than getId
       * or setId will result in an exception.
       **/
      bool isLoaded();

      // INTERFACE METHODS
      // =====================================================
      // The following methods are a replacement for interfaces
      // so that all language bindings have access to the type
      // safety available in Java. Making these into IObject
      // subclasses would not work, since slice does not support
      // multiple inheritance.

      /**
       * Marker interface which means that special rules apply
       * for both reading and writing these instances.
       **/
      bool isGlobal();

      /**
       * A link between two other types.
       * Methods provided:
       *
       *   - getParent()
       *   - getChild()
       **/
      bool isLink();

      /**
       * The server will persist changes made to these types.
       * Methods provided:
       *
       *   - getVersion()
       *   - setVersion()
       *
       **/
      bool isMutable();

      /**
       * Allows for the attachment of any omero.model.Annotation
       * subclasses. Methods provided are:
       *
       *   - linkAnnotation(Annotation)
       *   -
       **/
      bool isAnnotated();


    };
  };
};

#include <omero/model/Event.ice>
#include <omero/model/Experimenter.ice>
#include <omero/model/ExperimenterGroup.ice>
#include <omero/model/ExternalInfo.ice>
#include <omero/model/Permissions.ice>
module omero {
  module model {

    /**
     * Embedded component of every OMERO.blitz type. Since this is
     * not an IObject subclass, no attempt is made to hide the state
     * of this object, since it cannot be ""unloaded"".
     **/
    ["protected"] class Details
    {

      //  ome.model.meta.Experimenter owner;
      omero::model::Experimenter owner;
      omero::model::Experimenter getOwner();
      void setOwner(omero::model::Experimenter theOwner);

      //  ome.model.meta.ExperimenterGroup group;
      omero::model::ExperimenterGroup group;
      omero::model::ExperimenterGroup getGroup();
      void setGroup(omero::model::ExperimenterGroup theGroup);

      //  ome.model.meta.Event creationEvent;
      omero::model::Event creationEvent;
      omero::model::Event getCreationEvent();
      void setCreationEvent(omero::model::Event theCreationEvent);

      //  ome.model.meta.Event updateEvent;
      omero::model::Event updateEvent;
      omero::model::Event getUpdateEvent();
      void setUpdateEvent(omero::model::Event theUpdateEvent);

      //  ome.model.internal.Permissions permissions;
      omero::model::Permissions permissions;
      omero::model::Permissions getPermissions();
      void setPermissions(omero::model::Permissions thePermissions);

      //  ome.model.meta.ExternalInfo externalInfo;
      omero::model::ExternalInfo externalInfo;
      omero::model::ExternalInfo getExternalInfo();
      void setExternalInfo(omero::model::ExternalInfo theExternalInfo);

      //
      // Context parameters
      //

      /**
       * Context which was active during the call which
       * returned this object. This context is set as
       * the last (optional) argument of any remote
       * Ice invocation. This is used to change the
       * user, group, share, etc. of the current session.
       **/
      Ice::Context call;
    };
  };
};
module omero {
  module api {
    ["java:type:java.util.ArrayList<omero.model.IObject>:java.util.List<omero.model.IObject>"]
    sequence<omero::model::IObject> IObjectList;
    dictionary<string, IObjectList> IObjectListMap;
    dictionary<long, IObjectList> LongIObjectListMap;
  };
};

#endif
