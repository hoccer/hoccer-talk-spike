package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cgjoin", description = "Join a group that you have been invited to")
public class ClientGroupJoin extends TalkToolClientCommand {

    @Parameter(names = "-g", description = "groupId to join", required = true)
    String pGroup;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        TalkClientContact group = null;
        try {
            group = client.getDatabase().findGroupContactByGroupId(pGroup, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (group != null) {
            client.getClient().joinGroup(group.getGroupId());
        } else {
            throw new Exception("Need to provide valid group");
        }
    }
}
