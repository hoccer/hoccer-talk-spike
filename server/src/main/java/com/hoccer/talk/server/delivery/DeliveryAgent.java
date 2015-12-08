package com.hoccer.talk.server.delivery;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.agents.NotificationDeferrer;

import java.util.ArrayList;

public class DeliveryAgent extends NotificationDeferrer {

    private static final ThreadLocal<ArrayList<Runnable>> context = new ThreadLocal<ArrayList<Runnable>>();

    private final TalkServer mServer;

    private final static String FORCE_ALL = "forceAll";
    private final static String NO_FORCE = "noForce";

    public DeliveryAgent(TalkServer server) {
        super(
                server.getConfiguration().getDeliveryAgentThreadPoolSize(),
                "delivery-agent"
        );
        mServer = server;
    }

    public TalkServer getServer() {
        return mServer;
    }

    public void requestDelivery(final String clientId, boolean forceAll) {
        if (clientId == null) {
            throw new IllegalArgumentException("no clientId");
        }
        final DeliveryRequest deliveryRequest = new DeliveryRequest(this, clientId, forceAll);

        Runnable notificationGenerator = new Runnable() {
            @Override
            public void run() {
                try {
                    TalkClient client = mServer.getDatabase().findClientById(deliveryRequest.mClientId);
                    if (client == null) {
                        throw new RuntimeException("requestDelivery: client "+deliveryRequest.mClientId+" not found");
                    }
                    if (client.isConnected() &&  !client.isReady()) {
                        LOG.debug("requestDelivery: client connected but not ready:'" + deliveryRequest.mClientId + ", not performing delivery");
                        return;
                    }

                    TalkServer.NonReentrantLock lock = mServer.idLockNonReentrant("deliveryRequest-"+deliveryRequest.mClientId);

                    boolean acquired = lock.tryLock();
                    LOG.debug("requestDelivery trylock for mClientId: '" + deliveryRequest.mClientId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+"acquired="+acquired+" waiting="+lock.getWaiting()+" withNoForce="+lock.getWaiting(NO_FORCE)+" withForceAll="+lock.getWaiting(FORCE_ALL));

                    if (!acquired && lock.getWaiting(FORCE_ALL) > 0) {
                        // we are sure that that there are other threads waiting to be performed
                        // with a forceAll so we can just throw away this request
                        LOG.debug("requestDelivery enough forceAll waiters, throwing away request for mClientId: '" + deliveryRequest.mClientId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+"acquired="+acquired+" waiting withForceAll="+lock.getWaiting(FORCE_ALL));
                        return;
                    }

                    if (!deliveryRequest.mForceAll && !acquired && lock.getWaiting(NO_FORCE) > 0) {
                        // we are sure that that there are other threads waiting to be performed
                        // so we can just throw away this request
                        LOG.debug("requestDelivery enough waiters, throwing away request for mClientId: '" + deliveryRequest.mClientId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+"acquired="+acquired+" waiting withNoForce="+lock.getWaiting(NO_FORCE));
                        return;
                    }

                    try {
                        String waiterType = deliveryRequest.mForceAll ? FORCE_ALL : NO_FORCE;
                        LOG.debug("requestDelivery will lock mClientId: '" + deliveryRequest.mClientId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+", waiterType="+waiterType);
                        if (!acquired) {
                            lock.lock(waiterType);
                        }
                        LOG.debug("requestDelivery acquired lock for mClientId: '" + deliveryRequest.mClientId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+", waiterType="+waiterType);
                        deliveryRequest.perform();
                        LOG.debug("requestDelivery ready for mClientId: '" + deliveryRequest.mClientId + "' with id " + lock + ", hash=" + lock.hashCode()+",thread="+Thread.currentThread()+", waiterType="+waiterType);
                    } catch (InterruptedException e) {
                        LOG.debug("requestDelivery: interrupted" + e);
                    } finally {
                        LOG.debug("requestDelivery releasing lock for mClientId: '" + deliveryRequest.mClientId + "' with id "+lock+", hash="+lock.hashCode()+",thread="+Thread.currentThread());
                        lock.unlock();
                    }

                } catch (Throwable t) {
                    LOG.error("caught and swallowed exception escaping runnable", t);
                }
            }
        };

        queueOrExecute(context, notificationGenerator);
    }

    public void setRequestContext() {
        setRequestContext(context);
    }

    public void clearRequestContext() {
        clearRequestContext(context);
    }
}
