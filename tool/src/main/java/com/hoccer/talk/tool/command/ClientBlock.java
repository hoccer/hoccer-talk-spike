package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cblock", description = "Block a client, use: cblock -c <client_id> -t <client_id_to_be_blocked>")
public class ClientBlock extends TalkToolClientCommand {

    @Parameter(names = "-t", description = "Target to block - either by selector or clientId", required = true)
    String pTarget;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        String clientIdToBeBlocked = context.getClientIdFromParam(pTarget);

        TalkClientContact contactToBeBlocked = null;
        try {
            contactToBeBlocked = client.getDatabase().findContactByClientId(clientIdToBeBlocked, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (contactToBeBlocked == null) {
            throw new Exception("Target not known to client.");
        }

        client.getClient().blockContact(contactToBeBlocked);
    }
}
