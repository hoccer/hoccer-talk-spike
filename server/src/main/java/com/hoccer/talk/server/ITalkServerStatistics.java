package com.hoccer.talk.server;

import better.jsonrpc.core.JsonRpcConnection;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common interface for server statistics
 * <p/>
 * This is here so the crappy in-memory statistics
 * can be replaced with Metrics easily.
 */
public interface ITalkServerStatistics {

    void signalClientRegisteredSucceeded();

    void signalClientRegisteredFailed();

    void signalClientLoginSRP1Succeeded();

    void signalClientLoginSRP1Failed();

    void signalClientLoginSRP2Succeeded();

    void signalClientLoginSRP2Failed();

    void signalMessageAcceptedSucceeded();

    void signalMessageConfirmedSucceeded();

    void signalMessageAcknowledgedSucceeded();

    void signalRequest();
    void signalResponse();
    //void signalNotificationSent();
    void signalNotificationReceived();

    com.codahale.metrics.Timer.Context signalRequestStart(JsonRpcConnection connection, ObjectNode request);

    long signalRequestStop(JsonRpcConnection connection, ObjectNode request, Timer.Context timerContext);
}
