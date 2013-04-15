package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "cstat", description = "Show status of selected clients")
public class ClientStatus extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolClient client) {
    }

}
