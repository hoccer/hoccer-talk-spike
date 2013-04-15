package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.util.PrintUtils;

import java.util.List;

@CLICommand(name = "clist", description = "List clients")
public class ClientList extends TalkToolCommand {

    final int      COLUMN_COUNT = 3;
    final String[] COLUMN_NAMES = new String[] {
        "ID", "State", "CID"
    };

    @Override
    protected void run(TalkToolContext context) throws Exception {
        List<TalkToolClient> clients = context.getClients();
        int numLines = clients.size() + 1;
        String[][] rows = new String[numLines][];
        rows[0] = COLUMN_NAMES;
        for(int i = 1; i < numLines; i++) {
            TalkToolClient client = clients.get(i-1);
            String[] columns = new String[COLUMN_COUNT];
            columns[0] = Integer.toString(client.getId());
            columns[1] = client.getClient().getStateString();
            columns[2] = client.getDatabase().getClient().getClientId();
            rows[i] = columns;
        }
        PrintUtils.printTable(rows);
    }

}
