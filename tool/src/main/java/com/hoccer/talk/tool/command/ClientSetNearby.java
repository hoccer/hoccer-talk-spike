package com.hoccer.talk.tool.command;


import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;

@CLICommand(name = "csetnearby", description = "Activate/deactivate nearby mode of a client")
public class ClientSetNearby extends TalkToolClientCommand {

    @Parameter(description = "<nearby-mode enabled flag (true|false)>")
    List<Boolean> pNearbyEnabled;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {
        client.setNearby(pNearbyEnabled.get(0));
    }
}
