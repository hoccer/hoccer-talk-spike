package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolClient;
import com.hoccer.talk.tool.TalkToolClientCommand;
import com.hoccer.talk.tool.TalkToolCommand;

@CLICommand(name = "stop", description = "Stop selected clients")
public class Stop extends TalkToolClientCommand {
}
