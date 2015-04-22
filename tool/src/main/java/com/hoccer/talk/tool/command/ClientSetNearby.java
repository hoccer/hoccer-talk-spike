package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;

@CLICommand(name = "csetnearby", description = "Activate/disconnect nearby mode of a client, " +
        "use: csetnearby -c <client-id> -f <*true|false> -r <true|*false>")
public class ClientSetNearby extends TalkToolClientCommand {

    @Parameter(description = "<nearby-mode enabled flag (true|false)> - default: 'true'", names = "-f")
    List<Boolean> pNearbyEnabled;

    @Parameter(description = "<nearby-mode regularly update flag (true|false) - default: 'false'", names = "-r")
    List<Boolean> pScheduleNearby;

    @Override
    public void runOnClient(final TalkToolContext context, final TalkToolClient client) {
        Boolean nearbyEnabled = true;
        if (pNearbyEnabled != null && !pNearbyEnabled.isEmpty()) {
            nearbyEnabled = pNearbyEnabled.get(0);
        }

        Boolean scheduleEnabled = false;
        if (pScheduleNearby != null && !pScheduleNearby.isEmpty()) {
            scheduleEnabled = pScheduleNearby.get(0);
        }

        client.setNearby(nearbyEnabled, scheduleEnabled);
    }
}
