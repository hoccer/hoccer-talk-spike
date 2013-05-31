package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cinit", description = "Initialize the database of the client")
public class ClientInit extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolClient client) {
        try {
            client.getDatabaseBackend().initializeDb();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
