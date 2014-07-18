package com.hoccer.talk.client.model;

import com.hoccer.talk.client.XoTransferAgent;

/**
 * Created by jacob on 17.07.14.
 */
public interface IXoTransferObject {
    public void start(XoTransferAgent agent);
    public void pause(XoTransferAgent agent);
    public void cancel(XoTransferAgent agent);
}
