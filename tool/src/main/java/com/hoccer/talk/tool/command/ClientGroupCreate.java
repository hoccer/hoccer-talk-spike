package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import better.cli.console.Console;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.talk.tool.TalkToolContext;
import com.hoccer.talk.tool.client.TalkToolClient;
import com.hoccer.talk.tool.client.TalkToolClientCommand;

import java.sql.SQLException;

@CLICommand(name = "cgcreate", description = "Creates a new group")
public class ClientGroupCreate extends TalkToolClientCommand /*implements IXoContactListener*/ {

    @Parameter(description = "Name of group, defaults to random UUID", names = "-n")
    String pGroupName;

    String mGroupTag;

    @Override
    public void runOnClient(TalkToolContext context, TalkToolClient client) throws SQLException {
        TalkClientContact newGroup = TalkClientContact.createGroupContact();
        mGroupTag = newGroup.getGroupTag();

        TalkGroupPresence groupPresence = new TalkGroupPresence();
        groupPresence.setGroupTag(newGroup.getGroupTag());
        newGroup.updateGroupPresence(groupPresence);

        if (pGroupName == null || pGroupName.isEmpty()) {
            pGroupName = newGroup.getGroupTag();
        }
        Console.info("group name is: '" + pGroupName + "'");
        newGroup.getGroupPresence().setGroupName(pGroupName);

        //client.getClient().registerContactListener(this);

        client.getClient().createGroup(newGroup);
        // The actual group creation is now async and we have no way to *wait* for it here => rely on Listeners.
    }

    /*
    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (mGroupTag == null) {
            Console.warn("Group was created before we issued our createGroup rpc call... IGNORING");
            return;
        }

        if (mGroupTag == contact.getGroupTag()) {
            Console.info("Group was created with id '" + contact.getGroupId() + "'");
        } else {
            Console.info("An unrelated group with id '" + contact.getGroupId() + "' was created - IGNORING");
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {

    }
    */
}
