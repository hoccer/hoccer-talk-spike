package com.hoccer.webclient.backend.updates;

public class CommandMessage {
    private String mCommand;
    private String mPath;

    public String getCommand() {
        return mCommand;
    }

    public void setCommand(String command) {
        this.mCommand = command;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }
}
