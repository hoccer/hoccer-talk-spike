package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolClientCommand;
import com.hoccer.talk.tool.TalkToolCommand;

@CLICommand(name = "start", description = "Start selected clients")
public class Start extends TalkToolClientCommand {
}
