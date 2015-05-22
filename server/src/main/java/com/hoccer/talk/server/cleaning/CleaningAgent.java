package com.hoccer.talk.server.cleaning;

import com.hoccer.talk.model.*;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.filecache.FilecacheClient;
import com.hoccer.talk.util.NamedThreadFactory;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cleaning agent
 * <p/>
 * This agent takes care of database garbage. We need it for several
 * reasons, the most important being multi-destination delivery.
 * <p/>
 * It is even more justified on mongodb where there is no transactionality.
 * <p/>
 * The various parts of it run in background and in other beneficial situations.
 */
public class CleaningAgent {

    private final static Logger LOG = Logger.getLogger(CleaningAgent.class);

    @SuppressWarnings("FieldCanBeLocal")
    private final TalkServer mServer;
    private final TalkServerConfiguration mConfig;
    private final ITalkServerDatabase mDatabase;
    private final ScheduledExecutorService mExecutor;

    private static final long SECONDS = 1000;
    private static final long MINUTES = SECONDS * 60;
    private static final long HOURS = MINUTES * 60;
    private static final long DAYS = HOURS * 24;
    private static final long WEEKS = DAYS * 7;
    private static final long MONTHS = DAYS * 30;

    // Clients should do a full sync at least once per month, so we retain tombstones for 2 months
    private static final long UNUSED_KEY_LIFE_TIME = 2 * MONTHS;
    private static final long NO_RELATIONSHIP_LIFE_TIME = 2 * MONTHS;
    private static final long DELETED_CLIENT_LIFE_TIME = 2 * MONTHS;
    private static final long UNUSED_CLIENT_LIFE_TIME = 6 * MONTHS;
    private static final long DELETED_GROUP_MEMBER_LIFE_TIME = 2 * MONTHS;
    private static final long DELETED_GROUP_PRESENCE_LIFE_TIME = 2 * MONTHS;
    private static final long UNFINISHED_DELIVERY_LIFE_TIME = 3 * MONTHS;


    private boolean firstRunDone = false;

    public CleaningAgent(TalkServer server) {
        mServer = server;
        mConfig = mServer.getConfiguration();
        mDatabase = mServer.getDatabase();
        mExecutor = Executors.newScheduledThreadPool(
                mConfig.getCleaningAgentThreadPoolSize(),
                new NamedThreadFactory("cleaning-agent")
        );
 /*
       LOG.info("Cleaning clients scheduling will start in '" + mConfig.getCleanupAllClientsDelay() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    scheduleCleanAllClients();
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        }, mConfig.getCleanupAllClientsDelay(), TimeUnit.SECONDS);

        LOG.info("Cleaning deliveries scheduling will start in '" + mConfig.getCleanupAllDeliveriesDelay() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    scheduleCleanAllDeliveries();
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        }, mConfig.getCleanupAllDeliveriesDelay(), TimeUnit.SECONDS);
*/
        LOG.info("Cleaning scheduling will start in '" + mConfig.getCleanupAllClientsDelay() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    scheduleCleanAll();
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        }, mConfig.getCleanupAllClientsDelay(), TimeUnit.SECONDS);


    }

    public static boolean timeAgo(Date when, long milliSeconds) {
        long ago = new Date().getTime() - when.getTime();
        return ago >= milliSeconds;
    }

    private void cleanClientData(final String clientId) {
        LOG.debug("*** cleaning client " + clientId);

        if (!mDatabase.isDeletedClient(clientId)) {
            TalkClient client = mDatabase.findClientById(clientId);
            LOG.debug("client last time login '" + client.getTimeLastLogin());


            if (client != null) {
                Date lastLogin = client.getTimeLastLogin();
                if (lastLogin == null) {
                    lastLogin = new Date(0);
                }
                if (timeAgo(lastLogin, UNUSED_CLIENT_LIFE_TIME)) {
                    // delete client that has not been active for UNUSED_CLIENT_LIFE_TIME months
                    LOG.info("deleting unused client id '" + clientId + "'");
                    client.setSrpVerifier("");
                    client.setReasonDeleted("unused-lifetime-expired");
                    mDatabase.markClientDeleted(client);
                    mServer.getUpdateAgent().performAccountDeletion(clientId);
                } else {
                    LOG.debug("keeping used client id '" + clientId + "', just cleaning keys and tokens");
                    doCleanKeysForClient(clientId);
                    doCleanTokensForClient(clientId);
                }
            }
        } else {
            // check for deleted clients expiry

            String originalClientId = mDatabase.beforeDeletedId(clientId);
            TalkClient client = mDatabase.findClientById(clientId);

            if (client != null && timeAgo(client.getTimeDeleted(), DELETED_CLIENT_LIFE_TIME)) {
                // finally remove deleted client
                LOG.info("removing deleted expired client id '" + clientId + "'");
                mDatabase.deleteClient(client);

                TalkPresence presence = mDatabase.findPresenceForClient(originalClientId);
                if (presence != null) {
                    mDatabase.deletePresence(presence);
                } else {
                    LOG.info("no presence for expired client id '" + clientId + "'");
                }

                TalkClientHostInfo clientHostInfo = mDatabase.findClientHostInfoForClient(originalClientId);
                if (clientHostInfo != null) {
                    mDatabase.deleteClientHostInfo(clientHostInfo);
                }
                mServer.getFilecacheClient().deleteAccount(originalClientId);
            } else {
                LOG.debug("keeping deleted expired client id '" + clientId + "', has been marked for deletion on "+client.getTimeDeleted());
            }
        }
    }
    /*
    private void scheduleCleanAllDeliveries() {
        LOG.info("scheduling deliveries cleanup in '" + mConfig.getCleanupAllDeliveriesInterval() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    doCleanAllFinishedDeliveries();
                    scheduleCleanAllDeliveries();
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        }, mConfig.getCleanupAllDeliveriesInterval(), TimeUnit.SECONDS);
    }

    private void scheduleCleanAllClients() {
        LOG.info("scheduling client cleanup in '" + mConfig.getCleanupAllClientsInterval() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    doCleanAllClients();
                    doCleanGroups();
                    doCleanRelationships();
                    scheduleCleanAllClients();
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        } , mConfig.getCleanupAllClientsInterval(), TimeUnit.SECONDS);
    }
    */
    private void scheduleCleanAll() {

        int cleanUpInInterval = firstRunDone ? mConfig.getCleanupAllClientsInterval() : 1;
        LOG.info("scheduling cleanup in '" + mConfig.getCleanupAllClientsInterval() + "' seconds.");
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    doCleanAllDeliveries();
                    doCleanAllClients();
                    doCleanGroups();
                    doCleanRelationships();
                    firstRunDone = true;
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                } finally {
                    scheduleCleanAll();
                }
            }
        } , cleanUpInInterval, TimeUnit.SECONDS);
    }


    public void cleanFinishedDelivery(final TalkDelivery finishedDelivery) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doCleanFinishedDelivery(finishedDelivery);
                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        });
    }

    private void doCleanAllClients() {
        long startTime = System.currentTimeMillis();
        LOG.info("Cleaning all clients. Determining list of clients...");
        List<TalkClient> allClients = mDatabase.findAllClients();
        LOG.info("Cleaning '" + allClients.size() + "' clients...");
        long current = 0;
        for (TalkClient client : allClients) {
            if (current++ % 10000 == 0) {
                LOG.info("Clean client " + current + "/" + allClients.size() + " clients...");
            }
            cleanClientData(client.getClientId());
        }
        long endTime = System.currentTimeMillis();
        LOG.info("Cleaning of " + allClients.size() + " clients done (took " + (endTime - startTime) + " ms')");
    }

    private void doCleanAllDeliveries() {
        long startTime = System.currentTimeMillis();
        LOG.info("Cleaning all deliveries...");

        long counter = 0;
        Date oldDate = new Date(new Date().getTime() - UNFINISHED_DELIVERY_LIFE_TIME);

        List<TalkDelivery> expiredDeliveries = mDatabase.findDeliveriesAcceptedBefore(oldDate);
        LOG.info("cleanup found " + expiredDeliveries.size() + " expired unfinished deliveries");
        counter = 0;
        for (TalkDelivery delivery : expiredDeliveries) {
            if (!delivery.isFinished()) {
                delivery.expireDelivery();
                mDatabase.saveDelivery(delivery);
            }
            if (counter++ % 100 == 0) {
                LOG.info("expiring unfinished deliveries: "+counter+ "/" + expiredDeliveries.size() + " done");
            }
        }

        List<TalkDelivery> finishedDeliveries = mDatabase.findDeliveriesInStatesAndAttachmentStates(TalkDelivery.FINAL_STATES, TalkDelivery.FINAL_ATTACHMENT_STATES);
        LOG.info("cleanup found " + finishedDeliveries.size() + " finished deliveries");
        counter = 0;
        for (TalkDelivery delivery : finishedDeliveries) {
            doCleanFinishedDelivery(delivery);
            if (counter++ % 100 == 0) {
                LOG.info("cleanup finished deliveries: "+counter+ "/" + finishedDeliveries.size() + " done");
            }
        }

        // we need to clean failed deliveries regardless of attachment state
        List<TalkDelivery> finalFailedDeliveries = mDatabase.findDeliveriesInStates(TalkDelivery.FINAL_FAILED_STATES);
        LOG.info("cleanup found " + finalFailedDeliveries.size() + " final failed deliveries");
        counter = 0;
        for (TalkDelivery delivery : finalFailedDeliveries) {
            doCleanFinishedDelivery(delivery);
            if (counter++ % 100 == 0) {
                LOG.info("cleanup failed deliveries: "+counter+ "/" + finalFailedDeliveries.size() + " done");
            }
        }

        int totalDeliveriesCleaned = finishedDeliveries.size() + finalFailedDeliveries.size();

        long endTime = System.currentTimeMillis();
        LOG.info("Cleaning of '" + totalDeliveriesCleaned + "' deliveries done (took '" + (endTime - startTime) + "ms'). rescheduling next run...");
    }

    private void doCleanFinishedDelivery(TalkDelivery finishedDelivery) {
        synchronized (mServer.idLock(finishedDelivery.getMessageId())) {
            TalkDelivery delivery = mDatabase.findDelivery(finishedDelivery.getMessageId(), finishedDelivery.getReceiverId());
            if (delivery != null) {
                String messageId = delivery.getMessageId();
                TalkMessage message = mDatabase.findMessageById(messageId);
                if (message != null) {
                    if (message.getNumDeliveries() == 1) {
                        // if we have only one delivery then we can safely delete the msg now
                        doDeleteMessage(message);
                    } else {
                        // else we need to determine the state of the message in detail
                        doCleanDeliveriesForMessage(messageId, message);
                    }
                } else {
                    doCleanDeliveriesForMessage(messageId, null);
                }
                // always delete the ACKed delivery
                //LOG.debug("doCleanFinishedDelivery: Deleting delivery with state '" + delivery.getState() + "' and attachmentState '" + delivery.getAttachmentState() + "', messageId: " + messageId + ", receiverId:" + delivery.getReceiverId());
                mDatabase.deleteDelivery(delivery);
            } else {
                //LOG.debug("doCleanFinishedDelivery: Delivery already deleted, messageId: " + finishedDelivery.getMessageId() + ", receiverId:" + finishedDelivery.getReceiverId());
            }
        }
    }

    private void doCleanDeliveriesForMessage(String messageId, TalkMessage message) {
        boolean keepMessage = false;
        List<TalkDelivery> deliveries = mDatabase.findDeliveriesForMessage(messageId);
        //LOG.debug("Found " + deliveries.size() + " deliveries for messageId: " + messageId);
        for (TalkDelivery delivery : deliveries) {
            // confirmed and failed deliveries can always be deleted
            if (delivery.isFinished()) {
                //LOG.debug("Deleting delivery with state '" + delivery.getState() + "' and attachmentState '" + delivery.getAttachmentState() + "', messageId: " + messageId + ", receiverId:" + delivery.getReceiverId());
                mDatabase.deleteDelivery(delivery);
                continue;
            }
            //LOG.debug("Keeping delivery with state '" + delivery.getState() + "' and attachmentState '" + delivery.getAttachmentState() + "', messageId: " + messageId + ", receiverId:" + delivery.getReceiverId());
            keepMessage = true;
        }
        if (message != null && !keepMessage) {
            doDeleteMessage(message);
        }
    }

    private void doCleanKeysForClient(String clientId) {
        LOG.debug("cleaning keys for client " + clientId);

        TalkPresence presence = mDatabase.findPresenceForClient(clientId);

        List<TalkKey> keys = mDatabase.findKeys(clientId);
        for (TalkKey key : keys) {
            if (presence != null && key.getKeyId().equals(presence.getKeyId())) {
                LOG.debug("keeping " + key.getKeyId() + " because it is used");
                continue;
            }
            if (!timeAgo(key.getTimestamp(), UNUSED_KEY_LIFE_TIME)) {
                LOG.debug("keeping " + key.getKeyId() + " because it is recent");
                continue;
            }
            LOG.debug("deleting key " + key.getKeyId());
            mDatabase.deleteKey(key);
        }
    }

    private void doCleanTokensForClient(String clientId) {
        LOG.debug("cleaning tokens for client " + clientId);

        Date now = new Date();
        int numSpent = 0;
        int numExpired = 0;
        List<TalkToken> tokens = mDatabase.findTokensByClient(clientId);
        for (TalkToken token : tokens) {
            if (token.getState().equals(TalkToken.STATE_SPENT)) {
                numSpent++;
                mDatabase.deleteToken(token);
                continue;
            }
            if (token.getExpiryTime().before(now)) {
                numExpired++;
                mDatabase.deleteToken(token);
            }
        }
        if (numSpent > 0) {
            LOG.debug("deleted " + numSpent + " spent tokens");
        }
        if (numExpired > 0) {
            LOG.debug("deleted " + numExpired + " expired tokens");
        }
    }

    private void doDeleteMessage(TalkMessage message) {
        //LOG.debug("doDeleteMessage: deleting message with id " + message.getMessageId());

        // delete attached file if there is one
        String fileId = message.getAttachmentFileId();
        if (fileId != null) {
            FilecacheClient filecache = mServer.getFilecacheClient();
            if (filecache == null) {
                throw new RuntimeException("cant get filecache");
            }
            LOG.debug("doDeleteMessage: deleting file with id " + fileId);
            filecache.deleteFile(fileId);
        }

        // delete the message itself
        mDatabase.deleteMessage(message);
    }

    private void doCleanGroups() {
        Date oldGroupDate = new Date(new Date().getTime() - DELETED_GROUP_PRESENCE_LIFE_TIME);
        LOG.info("doCleanGroups: cleaning expired groups deleted before "+ oldGroupDate);

        int deleted = mDatabase.deleteGroupPresencesWithStateChangedBefore(TalkGroupPresence.STATE_DELETED, oldGroupDate);
        LOG.info("doCleanGroups: deleted "+deleted+" group presences");

        Date oldGroupMemberDate = new Date(new Date().getTime() - DELETED_GROUP_MEMBER_LIFE_TIME);

        LOG.info("doCleanGroups: cleaning expired group memberships deleted before "+ oldGroupMemberDate);

        deleted = mDatabase.deleteGroupMembershipsWithStatesChangedBefore(
                new String[]{TalkGroupMembership.STATE_GROUP_REMOVED, TalkGroupMembership.STATE_NONE}, oldGroupMemberDate);

        LOG.info("doCleanGroups: deleted "+deleted+" group members");
        // TODO: clean dangling nearby and worldwide groups and memberships without environments (there shouldn't be any,but we should check)
    }

    private void doCleanRelationships() {
        LOG.info("doCleanRelationships");
        Date oldDate = new Date(new Date().getTime() - NO_RELATIONSHIP_LIFE_TIME);
        LOG.info("doCleanRelationships: cleaning expired relationships deleted before "+ oldDate);
        int deleted = mDatabase.deleteRelationshipsWithStatesAndNotNotificationsDisabledChangedBefore(new String[]{TalkRelationship.STATE_NONE}, oldDate);
        LOG.info("doCleanRelationships: deleted "+deleted+" relationships");
        // TODO: Cleaning might cause unidirectional relationships - we should handle that properly
    }

}
