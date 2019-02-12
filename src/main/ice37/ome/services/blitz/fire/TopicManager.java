/*
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.blitz.fire;

import java.lang.reflect.Method;

import omero.ApiUsageException;
import omero.InternalException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.zeroc.IceStorm.AlreadySubscribed;
import com.zeroc.IceStorm.BadQoS;
import com.zeroc.IceStorm.NoSuchTopic;

/**
 * Local dispatcher to {@link IceStorm.TopicManager}
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since December 2008
 */
public interface TopicManager extends ApplicationListener {

    /**
     * Enforces <em>no</em> security constraints. For the moment, that is the
     * responsibility of application code. WILL CHANGE>
     */
    public void register(String topicName, com.zeroc.Ice.ObjectPrx prx, boolean strict)
    throws omero.ServerError;

    public void unregister(String topicName, com.zeroc.Ice.ObjectPrx prx)
    throws omero.ServerError;

    public final static class TopicMessage extends ApplicationEvent {

        private final String topic;
        private final com.zeroc.Ice.ObjectPrxHelperBase base;
        private final String method;
        private final Object[] args;

        public TopicMessage(Object source, String topic,
                            com.zeroc.Ice.ObjectPrxHelperBase base, String method, Object... args) {
            super(source);
            this.topic = topic;
            this.base = base;
            this.method = method;
            this.args = args;
        }
    }

    public final static class Impl implements TopicManager {

        private final static Logger log = LoggerFactory.getLogger(Impl.class);

        private final com.zeroc.Ice.Communicator communicator;

        public Impl(com.zeroc.Ice.Communicator communicator) {
            this.communicator = communicator;
        }

        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof TopicMessage) {

                TopicMessage msg = (TopicMessage) event;
                try {

                    com.zeroc.IceStorm.TopicManagerPrx topicManager = managerOrNull();
                    if (topicManager == null) {
                        log.warn("No topic manager");
                        return; // EARLY EXIT
                    }

                    com.zeroc.Ice.ObjectPrx obj = publisherOrNull(msg.topic);
                    msg.base.__copyFrom(obj);
                    Method m = null;
                    for (Method check : msg.base.getClass().getMethods()) {
                        if (check.getName().equals(msg.method)) {
                            if (check.getParameterTypes().length == msg.args.length) {
                                if (m != null) {
                                    String err = String
                                            .format(
                                                    "More than one method named "
                                                            + "\"%s\" with %s arguments",
                                                    msg.method, msg.args);
                                    log.error(err);
                                } else {
                                    m = check;
                                }
                            }
                        }
                    }
                    if (m == null) {
                        log.error(String.format("No method named \"%s\" "
                                + "with %s arguments", msg.method, msg.args));
                    } else {
                        m.invoke(msg.base, msg.args);
                    }
                } catch (com.zeroc.Ice.NoEndpointException nee) {
                    // Most likely caused during testing.
                    log.debug("Ice.NoEndpointException");
                } catch (Exception e) {
                    log.error("Error publishing to topic:" + msg.topic, e);
                }
            }
        }

        public void register(String topicName, com.zeroc.Ice.ObjectPrx prx, boolean strict)
                throws omero.ServerError {
            String id = prx.ice_id();
            id = id.replaceFirst("_", "");
            id = id.replace("_", ".");
            id = id + "PrxHelper";
            Class<?> pubClass = null;
            try {
                pubClass = Class.forName(id);
            } catch (ClassNotFoundException e) {
                throw new ApiUsageException(null, null,
                        "Unknown type for proxy: " + prx.ice_id());
            }
            com.zeroc.IceStorm.TopicPrx topic = topicOrNull(topicName);

            while (topic != null) { // See 45.7.3 IceStorm Clients under HA
                // IceStorm
                try {
                    topic.subscribeAndGetPublisher(null, prx);
                } catch (com.zeroc.Ice.UnknownException ue) {
                    log.warn("Unknown exception on subscribeAndGetPublisher");
                    continue;
                } catch (AlreadySubscribed e) {
                    if (strict) {
                        throw new ApiUsageException(null, null,
                                "Proxy already subscribed: " + prx);
                    }
                } catch (BadQoS e) {
                    throw new InternalException(null, null,
                            "BadQos in TopicManager.subscribe");
                } catch (com.zeroc.Ice.UserException ue) {
                    // Actually IceStorm.InvalidSubscriber, for Ice 3.4/3.5 compatibility
                    log.warn("Invalid subscriber on subscribeAndGetPublisher");
                    continue;
                }
                break;
            }
        }

        public void unregister(String topicName, com.zeroc.Ice.ObjectPrx prx)
            throws omero.ServerError {

            try {
                com.zeroc.IceStorm.TopicPrx topic = topicOrNull(topicName);
                if (topic != null) {
                    topic.unsubscribe(prx);
                }
            } catch (Exception e) {
                log.warn(String.format("Error unregistering: %s from %s",
                        prx, topicName));
            }

        }

        // Helpers
        // =========================================================================

        protected com.zeroc.IceStorm.TopicManagerPrx managerOrNull() {

            com.zeroc.Ice.ObjectPrx objectPrx = communicator
                    .stringToProxy("IceGrid/Query");
            com.zeroc.Ice.ObjectPrx[] candidates = null;

            try {
                com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrxHelper
                        .checkedCast(objectPrx);
                candidates = query
                        .findAllObjectsByType("_IceStorm_TopicManager");
            } catch (com.zeroc.Ice.CommunicatorDestroyedException cde) {
                // Nothing we can do. Return null;
                return null;
            } catch (com.zeroc.Ice.NoEndpointException nee) {
                // Most likely caused during testing.
                log.debug("Ice.NoEndpointException");
            } catch (Exception e) {
                log.warn("Error querying for topic manager", e);
            }

            com.zeroc.IceStorm.TopicManagerPrx tm = null;

            if (candidates == null || candidates.length == 0) {
                log.warn("Found no topic manager");
            } else if (candidates.length > 1) {
                log.warn("Found wrong number of topic managers: "
                        + candidates.length);
            } else {
                try {
                    tm = com.zeroc.IceStorm.TopicManagerPrxHelper
                            .checkedCast(candidates[0]);
                } catch (Exception e) {
                    log.warn("Could not cast to TopicManager", e);
                }
            }
            return tm;
        }

        protected com.zeroc.IceStorm.TopicPrx topicOrNull(String name) {
            com.zeroc.IceStorm.TopicManagerPrx topicManager = managerOrNull();
            com.zeroc.IceStorm.TopicPrx topic = null;
            if (topicManager != null) {
                try {
                    topic = topicManager.create(name);
                } catch (com.zeroc.IceStorm.TopicExists ex2) {
                    try {
                        topic = topicManager.retrieve(name);
                    } catch (NoSuchTopic e) {
                        throw new RuntimeException(
                                "Race condition retriving topic: " + name);
                    }
                }
            }
            return topic;
        }

        protected com.zeroc.Ice.ObjectPrx publisherOrNull(String name) {
            IceStorm.TopicPrx topic = topicOrNull(name);
            com.zeroc.Ice.ObjectPrx pub = null;
            if (topic != null) {
                pub = topic.getPublisher().ice_oneway();
            }
            return pub;
        }
    }

}
