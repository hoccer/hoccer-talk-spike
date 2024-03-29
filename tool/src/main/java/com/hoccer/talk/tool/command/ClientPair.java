package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;

import java.util.List;

@CLICommand(name = "cpair", description = "Pair clients using automatic token exchange, use: cpair <client1_id> <client2_id>")
public class ClientPair extends TalkToolCommand {

    @Parameter(description = "Clients to pair")
    List<String> pClients;

    @Override
    protected void run(TalkToolContext context) throws Exception {
        List<TalkToolClient> clients = context.getClientsBySelectors(pClients);
        if ((clients.size() % 2) != 0) {
            throw new Exception("Clients must be supplied in pairs");
        }
        for (int i = 0; i < clients.size(); i += 2) {
            pairClients(clients.get(i), clients.get(i + 1));
        }
    }

    private void pairClients(TalkToolClient generator, TalkToolClient consumer) {
        String token = generator.getClient().generatePairingToken();
        consumer.getClient().performTokenPairing(token);
    }

}
