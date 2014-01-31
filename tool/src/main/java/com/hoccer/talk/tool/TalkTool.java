package com.hoccer.talk.tool;

import better.cli.CLIContext;
import better.cli.CommandLineApplication;
import better.cli.EntryPoint;
import better.cli.annotations.CLIEntry;
import better.cli.exceptions.CLIInitException;

@CLIEntry
public class TalkTool extends CommandLineApplication<TalkToolContext> {

    public TalkTool() throws CLIInitException {
        super();
    }

    public static void main(String[] args) {
        EntryPoint.main(args);
    }

    @Override
    protected CLIContext createContext() {
        return new TalkToolContext(this);
    }

    @Override
    protected void shutdown() {
    }

}
