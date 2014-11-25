package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cginvite", description = "Invite another client to a group")
public class ClientGroupInvite extends TalkToolClientCommand {

    @Parameter(names = "-g", description = "GroupId of group to invite to", required = true)
    String pGroup;

    @Parameter(names = "-t", description = "Target of invitation - either by selector or clientId", required = true)
    String pTarget;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        String inviteeClientId = context.getClientIdFromParam(pTarget);

        TalkClientContact group = null;
        try {
            group = client.getDatabase().findGroupContactByGroupId(pGroup, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (group == null) {
            throw new Exception("Need to provide valid target and group");
        }

        client.getClient().inviteClientToGroup(group.getGroupId(), inviteeClientId);
    }
}
