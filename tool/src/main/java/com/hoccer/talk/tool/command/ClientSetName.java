package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;

@CLICommand(name = "csetname", description = "Set the name of a client")
public class ClientSetName extends TalkToolClientCommand {

    @Parameter(description = "Name to set")
    List<String> pName;

    @Override
    public void runOnClient(TalkToolClient client) {
        client.getClient().setClientName(pName.get(0));
    }
}
