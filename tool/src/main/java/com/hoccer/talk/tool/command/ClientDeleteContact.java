package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cdeletecontact", description = "Deletes a contact by given clientId")
public class ClientDeleteContact extends TalkToolClientCommand {

    @Parameter(description = "clientId of the contact to be removed", names="-t")
    String contactClientId;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws SQLException {
        if (contactClientId.equals(client.getClientId())) {
            Console.warn("Contact to be deleted is self-contact. This is disallowed");
            return;
        }

        TalkClientContact contactToBeDeleted = client.getDatabase().findContactByClientId(contactClientId, false);
        if (contactToBeDeleted == null) {
            contactToBeDeleted = client.getDatabase().findGroupContactByGroupId(contactClientId, false);
        }

        // Validate that the client knows the contact
        if (contactToBeDeleted == null) {
            Console.info("The client '" + client.getClientId() + "' does not know a contact with clientId '" + contactClientId +  "'");
            return;
        }

        Console.info("Contact is of type: '" + contactToBeDeleted.getContactType() + "'");

        client.getClient().deleteContact(contactToBeDeleted);
    }
}
