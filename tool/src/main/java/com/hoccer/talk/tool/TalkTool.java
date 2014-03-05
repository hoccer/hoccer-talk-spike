package com.hoccer.talk.tool;

import better.cli.CLIContext;
import better.cli.CommandLineApplication;
import better.cli.EntryPoint;
import better.cli.annotations.CLIEntry;
import better.cli.exceptions.CLIInitException;
import com.beust.jcommander.JCommander;

@CLIEntry
public class TalkTool extends CommandLineApplication<TalkToolContext> {

    static String[] mRawArgs;

    public TalkTool() throws CLIInitException {
        super();
    }

    public static void main(String[] args) {
        mRawArgs = args;
        EntryPoint.main(args);
    }

    @Override
    protected CLIContext createContext() {
        TalkToolContext context = new TalkToolContext(this);
        JCommander commander = new JCommander(context, mRawArgs);
        return context;
    }

    @Override
    protected void shutdown() {
    }

}
