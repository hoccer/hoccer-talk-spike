package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;
import java.util.List;

@CLICommand(name = "cstat", description = "Show status of selected clients")
public class ClientStatus extends TalkToolClientCommand {

    @Override
    public void runOnClient(TalkToolClient client) {
        System.out.println("client #" + client.getId());

        String clientId = client.getClientId();
        if(clientId != null) {
            System.out.println("  client id " + clientId);
        }

        System.out.println("  state " + client.getClient().getStateString());

        try {
            List<TalkClientContact> contacts = client.getDatabase().findAllContacts();
            if(contacts.isEmpty()) {
                System.out.println("  no contacts");
            } else {
                System.out.println("  " + contacts.size() + " contacts");
            }
            for(TalkClientContact contact: contacts) {
                if(contact.isSelf()) {
                    System.out.println("  contact self " + contact.getClientId());
                    TalkPresence presence = contact.getClientPresence();
                    if(presence != null) {
                        System.out.println("    name \"" + presence.getClientName() + "\""
                                            + " status \"" + presence.getClientStatus() + "\"");
                    }
                }
                if(contact.isClient()) {
                    System.out.println("  contact client " + contact.getClientId());
                    TalkRelationship relationship = contact.getClientRelationship();
                    if(relationship != null) {
                        System.out.println("    relationship " + relationship.getState());
                    }
                    TalkPresence presence = contact.getClientPresence();
                    if(presence != null) {
                        System.out.println("    name \"" + presence.getClientName() + "\""
                                            + " status \"" + presence.getClientStatus() + "\""
                                            + " connState " + presence.getConnectionStatus());
                    }
                }
                if(contact.isGroup()) {
                    System.out.println("  contact group " + contact.getGroupId());
                    TalkGroup group = contact.getGroupPresence();
                    if(group != null) {
                        System.out.println("    name \"" + group.getGroupName() + "\""
                                            + " state " + group.getState());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
