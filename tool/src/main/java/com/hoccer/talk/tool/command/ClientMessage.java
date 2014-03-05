package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

// import java.security.Provider;
import java.security.Security;
import java.sql.SQLException;
import java.util.List;

@CLICommand(name = "cmessage", description = "Send a text message from one client to another.")
public class ClientMessage extends TalkToolCommand {

    private final String DEFAULT_MESSAGE = "Hello World";

    @Parameter(description = "Clients for message exchange")
    List<String> pClients;

    @Parameter(description = "Message being sent, defaults to '" + DEFAULT_MESSAGE + "'", names = "-m")
    String pMessage;

    // this is obviously needed for message encryption
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    protected void run(TalkToolContext context) throws Exception {
        List<TalkToolClient> clients = context.getClientsBySelectors(pClients);

        /*Provider[] provs = Security.getProviders();
        for(Provider prov: provs) {
            Console.info(prov.toString());
        }*/

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
            TalkClientMessage clientMessage = generator.getClient().composeClientMessage(recipientContact, messageText);
            generator.getClient().requestDelivery(clientMessage);
        }
    }
}
