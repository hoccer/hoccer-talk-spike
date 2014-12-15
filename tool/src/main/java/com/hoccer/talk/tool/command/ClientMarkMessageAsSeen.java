package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;
import java.util.List;

@CLICommand(name = "cmarkseen", description = "List unseen messages of clients")
public class ClientMarkMessageAsSeen extends TalkToolClientCommand {

    private static final int sDefaultIndex = 1;

    @Parameter(description = "Message number as indexed (1-based) by cinbox - default is '1'", names = "-m")
    private int pNumMessageIndex = sDefaultIndex;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws SQLException {
        if (pNumMessageIndex < 1) {
            throw new RuntimeException("Index must be > 0 (1-based)");
        }
        final List<TalkClientMessage> messages = client.getDatabase().findUnseenMessages();

        if (messages.size() < pNumMessageIndex) {
            throw new RuntimeException("Referenced Message index '" + pNumMessageIndex + "' does not exist!");
        }
        TalkClientMessage message = messages.get(pNumMessageIndex -1);
        Console.info(message.getMessageId() + " - " + message.getText());

        message.markAsSeen();
        client.getDatabase().saveClientMessage(message);
    }

}
