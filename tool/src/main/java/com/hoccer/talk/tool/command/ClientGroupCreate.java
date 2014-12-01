package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;
import java.util.UUID;

@CLICommand(name = "cgcreate", description = "Creates a new group")
public class ClientGroupCreate extends TalkToolClientCommand {

    @Parameter(description = "Name of group, defaults to random UUID", names = "-n")
    String pGroupName;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws SQLException {
        Console.info("group name is: '" + pGroupName + "'");
        client.getClient().createGroup(pGroupName);
    }
}
