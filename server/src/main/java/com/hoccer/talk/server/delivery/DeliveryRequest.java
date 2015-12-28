package com.hoccer.talk.server.delivery;

import com.hoccer.talk.model.*;
import com.hoccer.talk.rpc.ITalkRpcClient;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.rpc.TalkRpcConnection;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Delivery requests encapsulate a delivery run for a given client
 * <p/>
 * Both incoming and outgoing deliveries are handled in one go.
 * If clients are not connected the request is passed on to the push agent.
 * Deliveries are rate-limited to one update every 5 seconds.
 */
public class DeliveryRequest {

    private static final Logger LOG = Logger.getLogger(DeliveryRequest.class);

    String mClientId;

    DeliveryAgent mAgent;

    TalkServer mServer;
    ITalkServerDatabase mDatabase;

    boolean mForceAll;

    public DeliveryRequest(DeliveryAgent agent, String clientId, boolean forceAll) {
        mClientId = clientId;
        mAgent = agent;
        mServer = mAgent.getServer();
        mDatabase = mServer.getDatabase();
        mForceAll = forceAll;
    }


    private boolean performIncoming(List<TalkDelivery> inDeliveries, ITalkRpcClient rpc, TalkRpcConnection connection) {
        boolean currentlyConnected = true;
        Date lastLogin = connection.getClient().getTimeLastLogin();
        if (lastLogin == null) {
            LOG.error("performIncoming: clientId: '" + mClientId + "has no last login time, assuming now");
            lastLogin = new Date();
        }
        for (TalkDelivery delivery : inDeliveries) {
            // we lost the connection somehow
            if (!currentlyConnected) {
                LOG.debug("performIncoming: clientId: '" + mClientId + "no longer connected");
                break;
            }

            delivery.ensureDates();
            if (!mForceAll && (delivery.getTimeUpdatedIn().getTime() > delivery.getTimeChanged().getTime())) {
                LOG.debug("performIncoming: clientId: '" + mClientId + ",delivery has not changed since last up'd in="+ delivery.getTimeUpdatedIn().getTime()+",changed="+delivery.getTimeChanged().getTime());
                continue;
            }

            synchronized (mServer.idLock(delivery.getMessageId())) {
                // get the matching message
                TalkMessage message = mDatabase.findMessageById(delivery.getMessageId());
                if (message == null) {
                    LOG.warn("message not found: " + delivery.getMessageId());
                    continue;
                }
                try {

                    TalkDelivery latestDelivery = mDatabase.findDelivery(delivery.getMessageId(), delivery.getReceiverId());
                    if (latestDelivery == null) {
                        throw new RuntimeException("delivery unexpectedly not found");
                    }
                    latestDelivery.ensureDates();

                    // remove for production build
//                    if (!latestDelivery.equals(delivery)) {
//                        LOG.trace("latestDelivery (in) has changed");
//                        LOG.trace("delivery:"+delivery.toString());
//                        LOG.trace("latestDelivery:"+latestDelivery.toString());
//                    }
                    boolean updatedInDuringThisLoginSession = latestDelivery.getTimeUpdatedIn().getTime() > lastLogin.getTime();
                    if (!mForceAll && (latestDelivery.getTimeUpdatedIn().getTime() > latestDelivery.getTimeChanged().getTime())
                            && updatedInDuringThisLoginSession) { // do not bail out here when last update has been in a previous login session
                        LOG.debug("performIncoming(2): clientId: '" + mClientId + ", delivery has not changed since last up'd in="+ delivery.getTimeUpdatedIn().getTime()+",changed="+delivery.getTimeChanged().getTime());
                        continue;
                    }

                    // post the delivery for the client

                    boolean recentlyDelivered = (latestDelivery.getTimeUpdatedIn() != null &&
                            updatedInDuringThisLoginSession &&
                            latestDelivery.getTimeUpdatedIn().getTime() + 15 * 1000 > new Date().getTime());

                    LOG.debug("performIncoming(2): clientId: '" + mClientId + ", updatedInDuringThisLoginSession="+ updatedInDuringThisLoginSession + ", recentlyDelivered="+recentlyDelivered);

                    if (!recentlyDelivered && (TalkDelivery.STATE_DELIVERING.equals(latestDelivery.getState()) /*|| mForceAll*/)) {
                        TalkDelivery filtered = new TalkDelivery();
                        filtered.updateWith(latestDelivery);
                        filtered.setTimeUpdatedIn(null);
                        filtered.setTimeUpdatedOut(null);
                        rpc.incomingDelivery(filtered, message);
                    } else {
                        TalkDelivery filtered = new TalkDelivery();
                        filtered.updateWith(delivery, TalkDelivery.REQUIRED_IN_UPDATE_FIELDS_SET);
                        rpc.incomingDeliveryUpdated(filtered);
                    }
                    latestDelivery.setTimeUpdatedIn(new Date());
                    mDatabase.saveDelivery(latestDelivery);
                } catch (Exception e) {
                    LOG.warn("Exception calling incomingDelivery() for clientId: '" + mClientId + "'", e);
                    //currentlyConnected = false; XXX do this when we can differentiate
                }
            }

            // check for disconnects
            if (!connection.isConnected()) {
                currentlyConnected = false;
            }
        }
        return currentlyConnected;
    }

    private boolean performOutgoing(List<TalkDelivery> outDeliveries, ITalkRpcClient rpc, TalkRpcConnection connection) {
        boolean currentlyConnected = true;
        for (TalkDelivery delivery : outDeliveries) {
            // we lost the connection somehow
            if (!currentlyConnected) {
                LOG.debug("performOutgoing: clientId: '" + mClientId + "no longer connected");
                break;
            }
            synchronized (mServer.idLock(delivery.getMessageId())) {

                delivery.ensureDates();

                if (delivery.isInStaleFor(30000)) {
                    LOG.debug("performOutgoing: clientId: '" + mClientId + ", delivery (state:"+delivery.getState()+","+delivery.getAttachmentState()+") is stale, up'd out="+ delivery.getTimeUpdatedOut().getTime()+",changed="+delivery.getTimeChanged().getTime());
                } else {
                    if (!mForceAll && delivery.isOutUpToDate()) {
                        LOG.debug("performOutgoing: clientId: '" + mClientId + ", delivery (state:"+delivery.getState()+","+delivery.getAttachmentState()+") has not changed since last up'd out="+ delivery.getTimeUpdatedOut().getTime()+",changed="+delivery.getTimeChanged().getTime());
                        continue;
                    }
                }

                TalkDelivery latestDelivery = mDatabase.findDelivery(delivery.getMessageId(), delivery.getReceiverId());
                if (latestDelivery == null) {
                    throw new RuntimeException("out delivery unexpectedly not found");
                }

                // remove for production build
                if (!latestDelivery.equals(delivery)) {
                    LOG.trace("latestDelivery (out) has changed");
                    LOG.trace("delivery:"+delivery.toString());
                    LOG.trace("latestDelivery:"+latestDelivery.toString());
                }

                if (delivery.isInStaleFor(30000)) {
                    LOG.debug("performOutgoing: clientId: '" + mClientId + ", delivery (state:"+delivery.getState()+","+delivery.getAttachmentState()+") is stale, up'd out="+ delivery.getTimeUpdatedOut().getTime()+",changed="+delivery.getTimeChanged().getTime());
                } else {
                    if (!mForceAll && delivery.isOutUpToDate()) {
                        LOG.debug("####### performOutgoing(2): clientId: '" + mClientId + ", delivery has not changed since last up'd out="+ delivery.getTimeUpdatedOut().getTime()+",changed="+delivery.getTimeChanged().getTime());
                        continue;
                    }
                }

                // notify it
                try {
                    TalkDelivery filtered = new TalkDelivery();
                    //LOG.info("Delivery orig:"+delivery.dump());
                    filtered.updateWith(delivery, TalkDelivery.REQUIRED_OUT_UPDATE_FIELDS_SET);
                    //LOG.info("Delivery filtered:"+delivery.dump());
                    //LOG.info("Delivery filtered: hasValidRecipient:"+filtered.hasValidRecipient()+", isExpandedGroupDelivery:"+filtered.isExpandedGroupDelivery());
                    if (filtered.hasValidRecipient() || filtered.isExpandedGroupDelivery()) {
                        rpc.outgoingDeliveryUpdated(filtered);
                        delivery.setTimeUpdatedOut(new Date());
                        mDatabase.saveDelivery(delivery);
                    } else {
                        mDatabase.deleteDelivery(delivery);
                        throw new RuntimeException("delivery is missing group and receiver, deleted");
                    }
                } catch (Exception e) {
                    LOG.warn("Exception calling outgoingDelivery() for clientId: '" + mClientId + "'", e);
                }
            }
            // check for disconnects
            if (!connection.isConnected()) {
                currentlyConnected = false;
            }
        }
        return currentlyConnected;
    }


    void perform() {
        LOG.debug("DeliverRequest.perform for clientId: '" + mClientId);
        boolean currentlyConnected = false;

        // determine if the client is currently connected
        TalkRpcConnection connection = mServer.getClientConnection(mClientId);
        ITalkRpcClient rpc = null;
        if (connection != null && connection.isConnected()) {
            currentlyConnected = true;
            rpc = connection.getClientRpc();
        }
        List<TalkDelivery> inDeliveries = new ArrayList<TalkDelivery>();

        LOG.debug("DeliverRequest.perform for clientId: '" + mClientId + ", currentlyConnected=" + currentlyConnected);
        boolean deliveryReady = false;
        if (currentlyConnected) {

            // get all outstanding deliveries for the client
            inDeliveries = mDatabase.findDeliveriesForClientInState(mClientId, TalkDelivery.STATE_DELIVERING);
            LOG.debug("clientId: '" + mClientId + "' has " + inDeliveries.size() + " incoming deliveries");
            if (!inDeliveries.isEmpty()) {
                // we will need to push if we don't succeed
                //needToNotify = true;
                // deliver one by one
                currentlyConnected = performIncoming(inDeliveries,rpc,connection);
            }

            if (currentlyConnected) {
                // get all deliveries for the client with not yet completed attachment transfers
                List<TalkDelivery> inAttachmentDeliveries =
                        mDatabase.findDeliveriesForClientInDeliveryAndAttachmentStates(mClientId, TalkDelivery.IN_ATTACHMENT_DELIVERY_STATES, TalkDelivery.IN_ATTACHMENT_STATES);
                LOG.debug("clientId: '" + mClientId + "' has " + inAttachmentDeliveries.size() + " incoming deliveries with relevant attachment states");
                if (!inAttachmentDeliveries.isEmpty()) {
                    // deliver one by one
                    currentlyConnected = performIncoming(inAttachmentDeliveries,rpc,connection);
                }
            }

            if (currentlyConnected) {
                List<TalkDelivery> outDeliveries =
                        mDatabase.findDeliveriesFromClientInStates(mClientId, TalkDelivery.OUT_STATES);
                LOG.debug("clientId: '" + mClientId + "' has " + outDeliveries.size() + " outgoing deliveries");
                if (!outDeliveries.isEmpty())      {
                    // deliver one by one
                    currentlyConnected = performOutgoing(outDeliveries, rpc, connection);
                }
            }

            if (currentlyConnected) {
                List<TalkDelivery> outDeliveries =
                        mDatabase.findDeliveriesFromClientInDeliveryAndAttachmentStates(mClientId, TalkDelivery.OUT_ATTACHMENT_DELIVERY_STATES, TalkDelivery.OUT_ATTACHMENT_STATES);
                LOG.debug("clientId: '" + mClientId + "' has " + outDeliveries.size() + " outgoing deliveries with relevant attachment states");
                if (!outDeliveries.isEmpty())      {
                    // deliver one by one
                    currentlyConnected = performOutgoing(outDeliveries,rpc, connection);
                }
            }
            mForceAll = false;
        } else {
            inDeliveries = mDatabase.findDeliveriesForClientInState(mClientId, TalkDelivery.STATE_DELIVERING);
            LOG.debug("unconnected clientId: '" + mClientId + "' has " + inDeliveries.size() + " incoming deliveries");
          }

        // initiate push delivery if needed
        if (!inDeliveries.isEmpty() && !currentlyConnected) {
            LOG.debug("check if push-notify " + mClientId);

            boolean needToNotify = false;

            for (TalkDelivery delivery : inDeliveries) {
                TalkRelationship relationship = mDatabase.findRelationshipBetween(delivery.getReceiverId(), delivery.getSenderId());
                if (relationship != null && TalkRelationship.NOTIFICATIONS_DISABLED.equals(relationship.getNotificationPreference())) {
                    LOG.debug("notifications disabled for sender "+delivery.getSenderId()+ " by " + mClientId);
                } else if (delivery.getGroupId() != null) {
                     TalkGroupMembership membership = mDatabase.findGroupMembershipForClient(delivery.getGroupId(), mClientId);
                    if (membership == null) {
                        LOG.debug("notifications: no membership for group " + delivery.getGroupId() + " by " + mClientId);
                    } else {
                        LOG.debug("notifications: preferences are '"+membership.getNotificationPreference()+"' for group " + delivery.getGroupId() + " by " + mClientId);
                    }
                     if (membership == null || TalkGroupMembership.NOTIFICATIONS_DISABLED.equals(membership.getNotificationPreference())) {
                         LOG.debug("notifications disabled for group "+delivery.getGroupId()+ " by " + mClientId);
                     } else {
                         LOG.debug("notifications not disabled for group "+delivery.getGroupId()+ " by " + mClientId);
                         needToNotify = true;
                         break;
                     }
                 } else {
                    LOG.debug("notifications not disabled for sender "+delivery.getSenderId()+ " by " + mClientId);
                    needToNotify = true;
                    break;
                }
            }
            if (needToNotify) {
                LOG.debug("pushing " + mClientId);
                performPush();
            }
        }
        if (currentlyConnected && inDeliveries.isEmpty()) {
            rpc.deliveriesReady();
        }
    }

    private void performPush() {
        // find client in database
        TalkClient client = mDatabase.findClientById(mClientId);
        // send push request
        if (client.isPushCapable()) {
            mServer.getPushAgent().submitRequest(client, false);
        } else {
            mServer.getPushAgent().submitRequest(client, false); // try push anyway to get push incapable stats
            LOG.warn("push unconfigured for " + mClientId);
        }
    }

}
