package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

// import java.security.Provider;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.security.Security;
import java.sql.SQLException;
import java.util.List;

@CLICommand(name = "cmessage", description = "Send a text message from one client to another, use: cmessage <sender_id> <recipient_id> -m <message_string> -f <path_to_file> -n <number_of_messages_to_send>")
public class ClientMessage extends TalkToolCommand {

    private final String DEFAULT_MESSAGE = "Hello World";
    private final String ATTACHMENT_CLONES_PATH = "files/clones";

    @Parameter(description = "Clients for message exchange")
    List<String> pClients;

    @Parameter(description = "Message being sent, defaults to '" + DEFAULT_MESSAGE + "'", names = "-m")
    String pMessage;

    @Parameter(description = "Path to file to be attached to the message (optional)", names = "-f")
    String pAttachmentPath;

    @Parameter(description = "Number of messages being send (optional, default is 1)", names = "-n")
    int pNumMessages = 1;

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
            throw new Exception("Clients must be supplied in a pair (sender, recipient)");
        }
        if (pMessage == null || pMessage.isEmpty()) {
            pMessage = DEFAULT_MESSAGE;
            Console.warn("WARN <ClientMessage::run> No message provided. Using default messageText.");
        }

        TalkClientUpload attachmentUpload = null;
        for (int i = 0; i < pNumMessages; ++i) {
            if (!(pAttachmentPath == null || pAttachmentPath.isEmpty())) {
                attachmentUpload = createAttachment(retrieveFile(i));
            }
            sendMessage(clients.get(0), clients.get(1), pMessage, attachmentUpload);
        }
    }

    private File retrieveFile(int fileId) {
        File clonesDir = new File(ATTACHMENT_CLONES_PATH);
        clonesDir.mkdirs();

        File originalFile = new File(pAttachmentPath);
        File newFile = new File(ATTACHMENT_CLONES_PATH + "/" + fileId + "_" + originalFile.getName());
        try {
            Files.copy(originalFile.toPath(), newFile.toPath());
            return newFile;
        } catch (FileAlreadyExistsException e) {
            Console.warn("WARN <ClientMessage::retrieveFile> File (" + newFile.getAbsolutePath() + ") already exists. Continuing anyway.");
            return newFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private TalkClientUpload createAttachment(File fileToUpload) {
        if (fileToUpload == null) {
            return null;
        } else {
            Console.info("<ClientMessage::createAttachment> Creating attachment for file: '" + fileToUpload.getAbsolutePath() + "'");
            String url = fileToUpload.getAbsolutePath();
            String contentUrl = url; // in android this makes a difference
            String contentType = "image/*"; // XXX TODO: calculate filetype
            String mediaType = "image"; // seems to be only needed in android
            double aspectRatio = 1.0; // XXX TODO: calculate ((float)fileWidth) / ((float)fileHeight)
            int contentLength = (int)fileToUpload.length();

            TalkClientUpload attachmentUpload = new TalkClientUpload();
            attachmentUpload.initializeAsAttachment(contentUrl, url, contentType, mediaType, aspectRatio, contentLength);
            return attachmentUpload;
        }
    }

    private void sendMessage(TalkToolClient sender, TalkToolClient recipient, String messageText, TalkClientUpload attachment) {
        Console.info("<ClientMessage::sendMessage> sender-id: '" + sender.getClientId() + "', recipient-id: '" + recipient.getClientId() + "', message: '" + messageText + "'");

        // check if relationship exists
        // XXX TODO: implement a relationship-check in XOClient,
        //           e.g. sender.isKnownRelationship(TalkToolClient recipient)
        TalkClientContact recipientContact;
        try {
            recipientContact = sender.getDatabase().findContactByClientId(recipient.getClientId(), false);
        } catch (SQLException e) {
            recipientContact = null;
            e.printStackTrace();
        }

        if (recipientContact == null) {
            Console.warn("WARN <ClientMessage::sendMessage> The sender has no relationship to the recipient. Doing nothing.");
        } else {
            TalkClientMessage clientMessage = sender.getClient().composeClientMessage(recipientContact, messageText, attachment);
            sender.getClient().requestDelivery(clientMessage);
        }
    }
}
