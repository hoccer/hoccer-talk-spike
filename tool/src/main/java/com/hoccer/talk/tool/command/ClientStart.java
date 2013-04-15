package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "cstart", description = "Start selected clients")
public class ClientStart extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolClient client) {
        client.start();
    }
}
