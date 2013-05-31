package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "cginvite", description = "Invite another client to a group")
public class ClientGroupInvite extends TalkToolClientCommand {
    @Override
    public void runOnClient(TalkToolClient client) {
    }
}
