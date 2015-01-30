package com.hoccer.webclient.backend;

import com.beust.jcommander.Parameter;

public class CommandLineParameters {
    @Parameter(names = {"-c"}, description = "Configuration file to use.")
    private final String mConfigFile = null;

    public String getConfigFile() {
        return mConfigFile;
    }
}
