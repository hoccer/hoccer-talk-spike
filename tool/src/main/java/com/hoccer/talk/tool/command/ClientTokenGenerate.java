package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "ctgen", description = "Generate a token")
public class ClientTokenGenerate extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        String token = client.getClient().generatePairingToken();
        System.out.println("client " + client.getId() + " token " + token);
    }

}
