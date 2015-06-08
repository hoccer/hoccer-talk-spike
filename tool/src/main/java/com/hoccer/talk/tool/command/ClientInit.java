package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cinit", description = "Initialize the database of the client - normally not necessary.")
public class ClientInit extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {
        try {
            client.getDatabaseBackend().initializeDb();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
