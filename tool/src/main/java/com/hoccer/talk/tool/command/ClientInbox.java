package com.hoccer.talk.tool.command;


import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;
import java.util.List;

@CLICommand(name = "cinbox", description = "List unseen messages of clients")
public class ClientInbox extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {

        try {
            printMessages(client.getDatabase().findUnseenMessages(), client);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private void printMessages(List<TalkClientMessage> messages, TalkToolClient client) throws SQLException {
        Console.info("Listing messages for client with name: '" + client.getClient().getSelfContact().getName() +
                     "' (id: '" + client.getClientId() + "')...");
        if (messages.isEmpty()) {
            Console.info(" - no messages present");
        }

        for (int i = 0; i < messages.size(); ++i) {
            Console.info(" - message " + (i+1) + ": '" + messages.get(i).getText() + "'");
            Console.info("   (sent from: " + client.getDatabase().findClientContactById(messages.get(i).getConversationContact().getClientContactId()).getClientId() + ")");
        }
    }

}
