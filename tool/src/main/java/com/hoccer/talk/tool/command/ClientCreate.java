package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;

@CLICommand(name = "ccreate", description = "Create clients")
public class ClientCreate extends TalkToolCommand {

    @Parameter(description = "Number of clients to create",
                names = "-n")
    int pNumClients = 1;

    @Override
    protected void run(TalkToolContext context) throws Exception {
        Console.info("Creating " + pNumClients + " clients");
        for(int i = 0; i < pNumClients; i++) {
            TalkToolClient client = new TalkToolClient(context);
            context.addClient(client);
        }
    }

}
