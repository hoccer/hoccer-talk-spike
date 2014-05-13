package com.hoccer.talk.tool.command;


import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;

@CLICommand(name = "csetnearby", description = "Activate/deactivate nearby mode of a client, " +
                                               "use: csetnearby -c <client-id> <true|false>")
public class ClientSetNearby extends TalkToolClientCommand {

    @Parameter(description = "<nearby-mode enabled flag (true|false)> - default: 'true'", names = "-f")
    List<Boolean> pNearbyEnabled;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {
        Boolean nearbyEnabled = true;
        if (!pNearbyEnabled.isEmpty()) {
            nearbyEnabled = pNearbyEnabled.get(0);
        }
        client.setNearby(nearbyEnabled);
    }
}
