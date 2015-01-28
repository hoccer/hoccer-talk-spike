package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.crypto.CryptoUtils;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.security.Security;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@CLICommand(name = "cmessage", description = "Send a text message from one client to another, " +
        "use: cmessage <sender_id> <recipient_id> " +
        "-m <message_string> " +
        "-f <path_to_file> " +
        // the -n option is currently broken when running in non-ssl mode,
        // see: https://github.com/hoccer/scrum/issues/139
        "-n <number_of_messages_to_send>")
public class ClientMessage extends TalkToolCommand {

    private static final String DEFAULT_MESSAGE = "Hello World";
    private static final String ATTACHMENT_CLONES_PATH = "files/clones";

    private final Executor mExecutor = Executors.newScheduledThreadPool(16);

    @Parameter(description = "<sender-id> <recipient-id>")
    List<String> pClients;

    @Parameter(description = "Message being sent, defaults to '" + DEFAULT_MESSAGE + "'", names = "-m")
    String pMessage;

    @Parameter(description = "Path to file to be attached to the message (optional)", names = "-f")
    String pAttachmentPath;

    @Parameter(description = "Number of messages being send (optional, default is 1)", names = "-n")
    int pNumMessages = 1;

    @Parameter(description = "perform delivery async (default true)", names = "-a")
    boolean pAsyncFlag = true;

    // this is obviously needed for message encryption
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    protected void run(TalkToolContext context) throws Exception {
        if (pClients.size() != 2) {
            throw new Exception("Clients must be supplied in a pair (sender, recipient)");
        }
        if (pMessage == null || pMessage.isEmpty()) {
            pMessage = DEFAULT_MESSAGE;
            Console.warn("WARN <ClientMessage::run> No message provided. Using default messageText.");
        }

        final TalkToolClient sender = context.getClientBySelector(pClients.get(0));
        final String recipientId = context.getClientIdFromParam(pClients.get(1));

        for (int i = 0; i < pNumMessages; ++i) {
            final int x = i;

            if (pAsyncFlag) {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        TalkClientUpload attachmentUpload = null;
                        if (!(pAttachmentPath == null || pAttachmentPath.isEmpty())) {
                            attachmentUpload = createAttachment(retrieveFile(x));
                        }
                        sendMessage(sender, recipientId, pMessage, attachmentUpload);
                    }
                });
            } else {
                TalkClientUpload attachmentUpload = null;
                if (!(pAttachmentPath == null || pAttachmentPath.isEmpty())) {
                    attachmentUpload = createAttachment(retrieveFile(x));
                }
                sendMessage(sender, recipientId, pMessage, attachmentUpload);
            }

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
            Console.debug("<ClientMessage::retrieveFile> File (" + newFile.getAbsolutePath() + ") already exists. Continuing anyway.");
            return newFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getContentHmac(String contentDataUrl) {
        try {
            return CryptoUtils.computeHmac(contentDataUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static TalkClientUpload createAttachment(File fileToUpload) {
        if (fileToUpload == null) {
            return null;
        } else {
            Console.info("<ClientMessage::createAttachment> Creating attachment for file: '" + fileToUpload.getAbsolutePath() + "'");
            String url = fileToUpload.getAbsolutePath();
            String fileName = fileToUpload.getName();
            String contentType = "image/*"; // XXX TODO: calculate filetype
            String mediaType = "image"; // seems to be only needed in android
            double aspectRatio = 1.0; // XXX TODO: calculate ((float)fileWidth) / ((float)fileHeight)
            int contentLength = (int)fileToUpload.length();
            String contentHmac = getContentHmac("file://" + url);

            TalkClientUpload attachmentUpload = new TalkClientUpload();
            attachmentUpload.initializeAsAttachment(fileName, url, url, contentType, mediaType, aspectRatio, contentHmac);
            return attachmentUpload;
        }
    }

    // TODO: put this into XOClient?!
    private static TalkClientContact getContactForClient(TalkToolClient client, String clientOrGroupId) {
        TalkClientContact contact = null;
        try {
            contact = client.getDatabase().findContactByClientId(clientOrGroupId, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // if nothing found as client it might be a group
        if (contact == null) {
            try {
                contact = client.getDatabase().findGroupContactByGroupId(clientOrGroupId, false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return contact;
    }

    private static void sendMessage(TalkToolClient sender, String recipientId, String messageText, TalkClientUpload attachment) {
        Console.info("<ClientMessage::sendMessage> sender-id: '" + sender.getClientId() + "', recipient-id: '" + recipientId + "', message: '" + messageText + "'");

        TalkClientContact recipientContact = getContactForClient(sender, recipientId);
        if (recipientContact == null) {
            Console.warn("WARN <ClientMessage::sendMessage> The sender doesn't know the recipient. Doing nothing.");
        } else {
            TalkClientMessage clientMessage = sender.getClient().composeClientMessage(recipientContact, messageText, attachment);
            sender.getClient().sendMessage(clientMessage.getMessageTag());
        }
    }
}
