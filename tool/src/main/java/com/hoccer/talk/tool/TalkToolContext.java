package com.hoccer.talk.tool;

import better.cli.CLIContext;
import better.cli.CommandLineApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TalkToolContext extends CLIContext {

    ObjectMapper mMapper;

    ScheduledExecutorService mExecutor;

    public TalkToolContext(CommandLineApplication<? extends CLIContext> app) {
        super(app);
        mExecutor = Executors.newScheduledThreadPool(8);
        mMapper = new ObjectMapper();
    }

    public ScheduledExecutorService getExecutor() {
        return mExecutor;
    }

    public ObjectMapper getMapper() {
        return mMapper;
    }

}
