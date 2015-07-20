package com.hoccer.talk.tool.command;


import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
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

    private static void printMessages(List<TalkClientMessage> messages, TalkToolClient client) throws SQLException {
        Console.info("Listing messages for client with name: '" + client.getClient().getSelfContact().getNickname() +
                     "' (id: '" + client.getClientId() + "')...");

        for (int i = 0; i < messages.size(); ++i) {
            Console.info(" - message " + (i+1) + ": '" + messages.get(i).getText() + "'");
            TalkClientDownload attachment = messages.get(i).getAttachmentDownload();
            if (attachment != null) {
                Console.info("   with attachment (download url: " + attachment.getDownloadUrl() + ")");
            }

            // determine recipient of message
            int conversationContactId = messages.get(i).getConversationContact().getClientContactId();
            // search own contacts for recipient-contact
            TalkClientContact senderContact = client.getDatabase().findContactById(conversationContactId);
            String senderId;
            if (senderContact.isGroup()) {
                senderId = senderContact.getGroupId();
            } else {
                senderId = senderContact.getClientId();
            }
            Console.info("   (sent from " + senderContact.getContactType() + ": " + senderId + ")");
        }
        Console.info("In total " + messages.size() + " messages present.");
    }

}
