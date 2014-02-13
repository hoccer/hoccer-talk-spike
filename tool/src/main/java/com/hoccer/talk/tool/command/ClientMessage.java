package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Security;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@CLICommand(name = "cmessage", description = "Send a text message from one client to another.")
public class ClientMessage extends TalkToolCommand {

    private final String DEFAULT_MESSAGE = "Hello World";

    @Parameter(description = "Clients for message exchange")
    List<String> pClients;

    @Parameter(description = "Message being sent, defaults to '" + DEFAULT_MESSAGE + "'", names="-m")
    String pMessage;

    // this is obviously needed for message encryption
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    protected void run(TalkToolContext context) throws Exception {
        List<TalkToolClient> clients = context.getClientsBySelectors(pClients);
        if (clients.size() != 2) {
            throw new Exception("Clients mus be supplied in a pair (sender, recipient)");
        }
        if (pMessage == null || pMessage.isEmpty()) {
            pMessage = DEFAULT_MESSAGE;
            Console.warn("No message provided. Using default messageText.");
        }
        sendMessage(clients.get(0), clients.get(1), pMessage);
    }

    private void sendMessage(TalkToolClient generator, TalkToolClient consumer, String messageText) {
        Console.info("<ClientMessage::sendMessage> sender-id: '" + generator.getClientId() + "', recipient-id: '" + consumer.getClientId() + "', message: '" + messageText + "'");

        // check if relationship exists
        // XXX TODO: implement a relationship-check in XOClient,
        //           e.g. generator.isKnownRelationship(TalkToolClient consumer)
        TalkClientContact recipientContact;
        try {
            recipientContact = generator.getDatabase().findContactByClientId(consumer.getClientId(), false);
        } catch (SQLException e) {
            recipientContact = null;
            e.printStackTrace();
        }

        if (recipientContact == null) {
            Console.warn("<ClientMessage::sendMessage> the sender has no relationship to the recipient. Doing nothing.");
        } else {
            generator.getClient().requestDelivery(composeMessage(generator, consumer, messageText));
        }
    }

    private TalkClientMessage composeMessage(TalkToolClient sender, TalkToolClient recipient, String messageText) {
        XoClientDatabase db = sender.getDatabase();

        final TalkClientContact senderContact = sender.getClient().getSelfContact();
        TalkClientContact recipientContact;

        try {
            recipientContact = sender.getDatabase().findContactByClientId(recipient.getClientId(), false);
        } catch (SQLException e) {
            recipientContact = null;
            e.printStackTrace();
        }

        final TalkClientMessage clientMessage = new TalkClientMessage();
        final TalkMessage message = new TalkMessage();
        final TalkDelivery delivery = new TalkDelivery();

        final String messageTag = UUID.randomUUID().toString();

        message.setMessageTag(messageTag);
        message.setBody(messageText);

        delivery.setMessageTag(messageTag);

        if(recipientContact.isGroup()) {
            delivery.setGroupId(recipientContact.getGroupId());
        }
        if(recipientContact.isClient()) {
            delivery.setReceiverId(recipientContact.getClientId());
        }

        clientMessage.markAsSeen();
        clientMessage.setText(messageText);
        clientMessage.setMessageTag(messageTag);
        clientMessage.setConversationContact(recipientContact);
        clientMessage.setSenderContact(senderContact);
        clientMessage.setMessage(message);
        clientMessage.setOutgoingDelivery(delivery);

        try {
            db.saveMessage(message);
            db.saveDelivery(delivery);
            db.saveClientMessage(clientMessage);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Console.error("sql error -> exception: '" + e + "', " + sw.toString());
        }

        return clientMessage;
    }
}
