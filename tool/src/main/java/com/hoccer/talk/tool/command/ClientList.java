package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.utils.PrintUtils;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;

import java.util.List;

@CLICommand(name = "clist", description = "List clients")
public class ClientList extends TalkToolCommand {

    static final String[] COLUMN_NAMES = new String[]{
        "id", "state", "isNearby", "isWorldwide", "clientId"
    };

    @Override
    protected void run(final TalkToolContext context) throws Exception {
        final List<TalkToolClient> clients = context.getClients();
        final int numLines = clients.size() + 1;
        final String[][] rows = new String[numLines][];
        rows[0] = COLUMN_NAMES;
        for (int i = 1; i < numLines; i++) {
            final TalkToolClient client = clients.get(i - 1);
            final String[] columns = new String[COLUMN_NAMES.length];
            // id
            columns[0] = Integer.toString(client.getId());
            // state
            columns[1] = client.getClient().getState().toString();
            // isNearby
            TalkClientContact nearbyGroup = client.getClient().getCurrentNearbyGroup();
            columns[2] = (nearbyGroup == null) ? "*inactive*" : nearbyGroup.getGroupId();
            // isWorldwide
            TalkClientContact worldwideGroup = client.getClient().getCurrentWorldwideGroup();
            columns[3] = (worldwideGroup == null) ? "*inactive*" : worldwideGroup.getGroupId();
            // clientId
            final String c = client.getDatabase().findSelfContact(true).getClientId();
            columns[4] = (c == null) ? "" : c;
            rows[i] = columns;
        }
        PrintUtils.printTable(rows);
    }

}
