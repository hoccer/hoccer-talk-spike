package com.hoccer.talk.tool.client;

import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;

import java.util.List;

public class TalkToolClientCommand extends TalkToolCommand {

    @Parameter(description = "Run on all clients", names = "-a")
    boolean pAllClients;
    @Parameter(description = "Run on specified clients", names = "-c")
    List<String> pSpecificClients;

    private List<TalkToolClient> selectClients(TalkToolContext context) {
        if (pAllClients) {
            return context.getClients();
        }
        if (pSpecificClients != null) {
            return context.getClientsBySelectors(pSpecificClients);
        }
        return context.getSelectedClients();
    }

    @Override
    protected void run(TalkToolContext context) throws Exception {
        List<TalkToolClient> clients = selectClients(context);
        for (TalkToolClient client : clients) {
            System.out.println("client #" + client.getId());
            runOnClient(context, client);
        }
    }

    public void runOnClient(TalkToolContext context, TalkToolClient client) throws Exception {
        System.out.println("Command unimplemented");
    }

}
