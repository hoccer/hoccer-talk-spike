package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;

import java.util.List;

@CLICommand(name = "cselect", description = "Select clients")
public class ClientSelect extends TalkToolCommand {

    @Parameter(description = "Select all clients", names = "-a")
    boolean pAll;
    @Parameter(description = "<name> ...")
    List<String> pName;

    @Override
    protected void run(TalkToolContext context) throws Exception {
        if (pAll) {
            context.setSelectedClients(context.getClients());
        } else {
            if (pName != null && !pName.isEmpty()) {
                context.setSelectedClients(context.getClientsBySelectors(pName));
            }
        }
    }
}
