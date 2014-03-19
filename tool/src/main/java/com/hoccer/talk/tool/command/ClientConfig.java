package com.hoccer.talk.tool.command;

import better.cli.annotations.CLICommand;
import com.hoccer.talk.tool.TalkToolCommand;
import com.hoccer.talk.tool.TalkToolContext;

@CLICommand(name = "config", description = "Dumps important configuration settings")
public class ClientConfig extends TalkToolCommand {

    @Override
    protected void run(TalkToolContext context) throws Exception {
        System.out.println("- server: " + context.getServer());
        System.out.println("- store DB in file: " + context.isDbModeFile());
        System.out.println("- threadpool size: " + context.getThreadPoolSize());
        System.out.println("- upload directory: " + context.getUploadDir());
        System.out.println("- download directory: " + context.getDownloadDir());
    }

}
