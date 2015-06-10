package com.hoccer.talk.tool;

import better.cli.Command;
import better.cli.CommandResult;
import com.beust.jcommander.Parameter;

public abstract class TalkToolCommand extends Command<TalkToolContext> {

    @Parameter(description = "Show command help", names = {"-h"}, help = true)
    private boolean pHelp;

    @Override
    protected CommandResult innerExecute(final TalkToolContext context) {
        if (pHelp) {
            usage();
        } else {
            try {
                run(context);
            } catch (final Exception ex) {
                ex.printStackTrace();
                return CommandResult.ERROR;
            }
        }
        return CommandResult.OK;
    }

    protected void run(@SuppressWarnings("UnusedParameters") final TalkToolContext context) throws Exception {
        System.out.println("Command not implemented");
    }

}
