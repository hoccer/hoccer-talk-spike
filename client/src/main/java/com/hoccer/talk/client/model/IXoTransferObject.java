package com.hoccer.talk.client.model;

import com.hoccer.talk.client.XoTransferAgent;


public interface IXoTransferObject {
    public void start(XoTransferAgent agent);
    public void pause(XoTransferAgent agent);
    public void cancel(XoTransferAgent agent);
    public void hold(XoTransferAgent agent);
}
