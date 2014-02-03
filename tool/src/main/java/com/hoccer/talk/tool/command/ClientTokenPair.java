package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.util.List;

@CLICommand(name = "ctpair", description = "Pair with a token")
public class ClientTokenPair extends TalkToolClientCommand {

    @Parameter(description = "<token>...", required = true)
    List<String> pTokens;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        for (String token : pTokens) {
            System.out.println("client " + client.getId() + " token " + token);
            client.getClient().performTokenPairing(token);
        }
    }

}
