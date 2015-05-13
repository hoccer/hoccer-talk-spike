package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.*;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;
import java.util.List;

@CLICommand(name = "cstat", description = "Show status of selected clients")
public class ClientStatus extends TalkToolClientCommand {

    public void printInd(String msg, int indentation) {
        String defaultIndentation = "            "; // support indentation level 6 at max
        int indentationSize = 2;
        String ind = defaultIndentation.substring(0, indentation * indentationSize);
        System.out.println(ind + msg);
    }

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) {

        String clientId = client.getClientId();
        if (clientId != null) {
            printInd("client id: '" + clientId + "'", 1);
        } else {
            printInd("client id: not yet generated - register first!", 1);
        }

        printInd("state: '" + client.getClient().getState().toString() + "'", 1);

        try {
            List<TalkClientContact> contacts = client.getDatabase().findAllContacts();
            if (contacts.isEmpty()) {
                printInd("no contacts", 1);
            } else {
                if (contacts.size() == 1) {
                    printInd(contacts.size() + " contact", 1);
                } else {
                    printInd(contacts.size() + " contacts", 1);
                }
            }
            for (TalkClientContact contact : contacts) {
                if (contact.isSelf()) {
                    printInd("contact " + contact.getClientContactId() + ": (self) " + contact.getClientId(), 1);
                    TalkPresence presence = contact.getClientPresence();
                    if (presence != null) {
                        printInd("name: " + presence.getClientName() + ", " +
                                 "status: " + presence.getClientStatus() , 2);
                    } else {
                        printInd("no presence", 2);
                    }
                    TalkPrivateKey privKey = contact.getPrivateKey();
                    if (privKey != null) {
                        printInd("private key-id: " + privKey.getKeyId(), 2);
                    } else {
                        printInd("no private key", 2);
                    }
                    TalkKey pubKey = contact.getPublicKey();
                    if (pubKey != null) {
                        printInd("public key-id: " + pubKey.getKeyId(), 2);
                    } else {
                        printInd("no public key", 2);
                    }
                }
                if (contact.isClient()) {
                    printInd("contact " + contact.getClientContactId() + ": (client) " + contact.getClientId(), 1);
                    TalkRelationship relationship = contact.getClientRelationship();
                    if (relationship != null) {
                        printInd("relationship: " + relationship.getState(), 2);
                    } else {
                        printInd("no relationship", 2);
                    }
                    TalkPresence presence = contact.getClientPresence();
                    if (presence != null) {
                        printInd("name: " + presence.getClientName() + ", " +
                                 "status: " + presence.getClientStatus() + ", " +
                                 "connState: " + presence.getConnectionStatus(), 2);
                    } else {
                        printInd("no presence", 2);
                    }
                    TalkKey pubKey = contact.getPublicKey();
                    if (pubKey != null) {
                        printInd("public key-id " + pubKey.getKeyId(), 2);
                    } else {
                        printInd("no public key", 2);
                    }
                }
                if (contact.isGroup()) {
                    printInd("contact " + contact.getClientContactId() + ": (group) " + contact.getGroupId(), 1);
                    TalkGroupPresence groupPresence = contact.getGroupPresence();
                    if (groupPresence != null) {
                        printInd("name: " + groupPresence.getGroupName() + ", " +
                                 "state: " + groupPresence.getState() + ", " +
                                 "type: " + groupPresence.getGroupType(), 2);
                    } else {
                        printInd("no presence", 2);
                    }
                    TalkGroupMembership membership = contact.getGroupMembership();
                    if (membership != null) {
                        printInd("state: " + membership.getState() + ", " +
                                 "role: " + membership.getRole(), 2);
                    } else {
                        printInd("no membership", 2);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
