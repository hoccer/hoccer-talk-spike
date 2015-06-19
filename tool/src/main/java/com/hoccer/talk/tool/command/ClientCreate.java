package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import java.util.ArrayList;

@CLICommand(name = "ccreate", description = "Create clients")
public class ClientCreate extends TalkToolCommand {
    @Parameter(description = "Number of clients to create", names = "-n")
    int pNumClients = 1;
    @Parameter(description = "Select clients after creation", names = "-s")
    boolean pSelectClients = false;
    @Parameter(description = "Client name - batchname if -n > 1, e.g. '<batchname>_#[i-n]' for n clients to be created. Default: not set!", names="--name")
    String pName = null;

    @Override
    protected void run(TalkToolContext context) throws Exception {
        Console.info("Creating " + pNumClients + " clients");
        ArrayList<TalkToolClient> clients = new ArrayList<TalkToolClient>();
        for (int i = 0; i < pNumClients; i++) {
            TalkToolClient client = new TalkToolClient(context);
            context.addClient(client);
            clients.add(client);
            client.getClient().setEncryptedDownloadDirectory(context.getApplication().getDownloadDir());
            if (pName != null) {
                final String name;
                if (pNumClients == 1) {
                    name = pName;
                } else {
                    name = pName + "_" + (i + 1);
                }
                client.getClient().getSelfContact().getSelf().setRegistrationName(name);
            }
        }
        if (pSelectClients) {
            context.setSelectedClients(clients);
        }
    }

}
