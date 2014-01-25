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

    @Parameter(names = "-g", description = "Group to invite to", required = true)
    String pGroup;
    @Parameter(names = "-t", description = "Target of invitation", required = true)
    String pTarget;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        TalkToolClient target = context.getClientBySelector(pTarget);
        TalkClientContact group = null;
        try {
            group = client.getDatabase().findContactByGroupId(pGroup, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (target != null && group != null) {
            client.getClient().inviteClientToGroup(group.getGroupId(), target.getClientId());
        } else {
            throw new Exception("Need to provide valid target and group");
        }
    }

}
