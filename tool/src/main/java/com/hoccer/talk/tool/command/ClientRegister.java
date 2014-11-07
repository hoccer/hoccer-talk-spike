package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

@CLICommand(name = "cregister", description = "Register client with server")
public class ClientRegister extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        client.getClient().getSelfContact().getSelf().confirmRegistration();
        client.getClient().register();
    }

}
