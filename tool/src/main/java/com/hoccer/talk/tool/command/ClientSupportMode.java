package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;


@CLICommand(name = "csupportmode", description = "Set support-mode and support-tag of clients, use: csupportmode -c <client-id> <true|false> [-t <support-tag>]")
public class ClientSupportMode extends TalkToolClientCommand {

    @Parameter(description = "<support-mode enabled flag (true|false)>")
    List<Boolean> pSupportModeEnabled;

    @Parameter(names = "-t", description = "support tag (true|false), optional")
    String pSupportTag;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {
        if (pSupportModeEnabled == null && pSupportTag == null) {
            Console.info("No support-mode flag or tag specified in command. Current configuration is:");
            Console.info("   - support-mode enabled: " + client.getSupportMode());
            Console.info("   - support-tag: " + client.getSupportTag());
        } else {
            String supportTag = null;
            if (pSupportTag != null && !pSupportTag.isEmpty()) {
                supportTag = pSupportTag;
            }
            Boolean supportMode = null;
            if (pSupportModeEnabled != null && pSupportModeEnabled.size() > 0) {
                supportMode = pSupportModeEnabled.get(0);
            }
            client.updateSupportMode(supportMode, supportTag);
        }
    }

}
