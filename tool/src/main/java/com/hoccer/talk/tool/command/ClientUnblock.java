package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cunblock", description = "Unblock a client, use: cunblock -c <client_id> -t <client_id_to_be_unblocked>")
public class ClientUnblock extends TalkToolClientCommand {

    @Parameter(names = "-t", description = "Target to unblock - either by selector or clientId", required = true)
    String pTarget;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        String clientIdToBeUnblocked = context.getClientIdFromParam(pTarget);

        TalkClientContact contactToBeUnblocked = null;
        try {
            contactToBeUnblocked = client.getDatabase().findContactByClientId(clientIdToBeUnblocked, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (contactToBeUnblocked == null) {
            throw new Exception("Target not known to client.");
        }

        client.getClient().unblockContact(contactToBeUnblocked);
    }
}
