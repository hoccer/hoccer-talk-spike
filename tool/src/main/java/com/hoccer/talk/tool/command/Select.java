package com.hoccer.talk.tool.command;

import better.cli.CommandResult;
import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;

import java.util.List;

@CLICommand(name = "select", description = "Select client instances")
public class Select extends TalkToolCommand {

    @Parameter(description = "Select all clients", names = "-a")
    boolean pAll;

    @Parameter(description = "<name> ...")
    List<String> pName;

}
