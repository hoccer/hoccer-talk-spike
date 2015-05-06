package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;

@CLICommand(name = "csetworldwide", description = "activate/deactivate worldwide mode of client, " +
        "use: csetworldwide -c <client-id> -f <*true|false> -t [string]")
public class ClientSetWorldwide extends TalkToolClientCommand {
    @Parameter(description = "<worldwide-mode enabled flag (true|false)> - default: 'true'", names = "-f")
    List<Boolean> pWorldwideEnabled;

    @Parameter(description = "<worldwide-mode tag [string] - default: empty", names = "-t")
    List<String> pWorldwideTag;

    @Override
    public void runOnClient(final TalkToolContext context, final TalkToolClient client) {
        Boolean worldwideEnabled = true;
        if (pWorldwideEnabled != null && !pWorldwideEnabled.isEmpty()) {
            worldwideEnabled = pWorldwideEnabled.get(0);
        }

        String worldwideTag = "";
        if (pWorldwideTag != null && !pWorldwideTag.isEmpty()) {
            worldwideTag = pWorldwideTag.get(0);
        }

        client.setWorldwide(worldwideEnabled, worldwideTag);
    }
}