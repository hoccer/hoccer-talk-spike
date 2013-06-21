package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "ctpair", description = "Pair with a token")
public class ClientTokenPair extends TalkToolClientCommand {

    @Parameter(description = "<Token>", required = true)
    String pToken;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        System.out.println("client " + client.getId() + " token " + pToken);
        client.getClient().performTokenPairing(pToken);
    }

}
