package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "cgjoin", description = "Join a group that you have been invited to")
public class ClientGroupJoin extends TalkToolClientCommand {
    @Override
    public void runOnClient(TalkToolClient client) {
    }
}
