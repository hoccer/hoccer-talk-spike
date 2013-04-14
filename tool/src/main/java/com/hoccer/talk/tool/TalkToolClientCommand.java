package com.hoccer.talk.tool;

import com.beust.jcommander.Parameter;

public class TalkToolClientCommand extends TalkToolCommand {

    @Parameter(description = "Run on all clients", names = "-a")
    boolean pAllClients;

    @Parameter(description = "Run on specified client only", names = "-c")
    String pSpecificClient;

    @Override
    protected void run() throws Exception {
        if(pSpecificClient != null) {
            System.out.println("Specific client " + pSpecificClient);
            return;
        }
        if(pAllClients) {
            System.out.println("All clients");
            return;
        }
    }

    public void runOnClient(TalkToolClient client) {
        System.out.println("Command unimplemented");
    }

}
