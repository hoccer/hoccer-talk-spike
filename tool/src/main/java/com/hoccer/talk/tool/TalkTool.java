package com.hoccer.talk.tool;

import better.cli.CLIContext;
import better.cli.CommandLineApplication;
import better.cli.EntryPoint;
import better.cli.annotations.CLIEntry;
import better.cli.exceptions.CLIInitException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

@CLIEntry
public class TalkTool extends CommandLineApplication<TalkToolContext> {

    static String[] mRawArgs;

    private static final String DEFAULT_FILES_DIR = "/files";
    private static final String UPLOAD_DIR = "/upload";
    private static final String DOWNLOAD_DIR = "/download";
    private static final String TALK_TOOL_SERVER_URI = "wss://talkserver.talk.hoccer.de:8443/";

    TalkToolContext mContext;

    @Parameter(names = {"-s", "-server"},
            description = "Talkserver to use (complete uri)")
    private String server = TALK_TOOL_SERVER_URI;

    @Parameter(names = "-dbfile",
            description = "If true database is stored in a file. By default memory mode is used.",
            arity = 1)
    private boolean dbfile = false;

    @Parameter(names = "-filesdir",
            description = "Directory path where encrypted files for upload and download will be stored. Default is '/files'",
            arity = 1)
    private String filesdir = DEFAULT_FILES_DIR;

    @Parameter(names = "-sslenabled",
            description = "Enables ssl. By default is true.",
            arity = 1)
    private boolean sslenabled = true;

    public TalkTool() throws CLIInitException {
        super();
        JCommander commander = new JCommander(this, mRawArgs);
        if (this.isSslEnabled()) {
            mContext.setupSsl();
        }
    }

    public static void main(String[] args) {
        mRawArgs = args;
        EntryPoint.main(args);
    }

    @Override
    protected CLIContext createContext() {
        mContext = new TalkToolContext(this);
        return mContext;
    }

    @Override
    protected void shutdown() {
    }

    public Boolean isSslEnabled() {
        return sslenabled;
    }

    public String getUploadDir() {
        return filesdir + UPLOAD_DIR;
    }

    public String getDownloadDir() {
        return filesdir + DOWNLOAD_DIR;
    }

    public Boolean isDbModeFile() {
        return dbfile;
    }

    public String getServer() {
        return server;
    }
}
