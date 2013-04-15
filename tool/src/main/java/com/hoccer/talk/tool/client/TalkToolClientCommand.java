package com.hoccer.talk.tool.client;

import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;

import java.util.ArrayList;
import java.util.List;

public class TalkToolClientCommand extends TalkToolCommand {

    @Parameter(description = "Run on all clients", names = "-a")
    boolean pAllClients;

    @Parameter(description = "Run on specified client only", names = "-c")
    String pSpecificClient;

    private List<TalkToolClient> selectClients(TalkToolContext context) {
        if(pSpecificClient != null) {
            return new ArrayList<TalkToolClient>();
        }
        if(pAllClients) {
            return context.getClients();
        }
        return new ArrayList<TalkToolClient>();
    }

    @Override
    protected void run(TalkToolContext context) throws Exception {
        List<TalkToolClient> clients = selectClients(context);
        for(TalkToolClient client: clients) {
            runOnClient(client);
        }
    }

    public void runOnClient(TalkToolClient client) {
        System.out.println("Command unimplemented");
    }

}
