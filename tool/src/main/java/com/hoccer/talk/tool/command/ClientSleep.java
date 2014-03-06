package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;

import java.util.ArrayList;
import java.util.List;

@CLICommand(name = "sleep", description = "Sleeps the talk-tool for the specified amount of milliseconds, use: sleep <millisec>")
public class ClientSleep extends TalkToolCommand {

    @Parameter(description = "amount of milliseconds to sleep")
    private List<Long> pSleepMillis = new ArrayList<Long>();

    @Override
    protected void run(TalkToolContext context) throws Exception {
        if (pSleepMillis.size() != 1) {
            throw new Exception("Wrong number of arguments. Sleep duration must be defined as 1 parameter, e.g. sleep 1000.");
        }
        Thread.sleep(pSleepMillis.get(0));
    }
}
