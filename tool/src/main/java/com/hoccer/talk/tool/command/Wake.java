package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolClient;
import com.hoccer.talk.tool.TalkToolClientCommand;
import com.hoccer.talk.tool.TalkToolCommand;

@CLICommand(name = "wake", description = "Wake selected clients")
public class Wake extends TalkToolClientCommand {
}
