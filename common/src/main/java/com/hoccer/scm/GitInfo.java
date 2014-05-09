package com.hoccer.scm;

import java.util.Properties;

public class GitInfo {

    public String commitId;
    public String branch;
    public String buildTime;
    public String commitTime;
    public String commitIdAbbrev;

    public GitInfo() {}

    public static GitInfo initializeFromProperties(Properties properties) {
        GitInfo gitInfo = new GitInfo();
        gitInfo.commitId = properties.getProperty("git.commit.id");
        gitInfo.commitIdAbbrev = properties.getProperty("git.commit.id.abbrev");
        gitInfo.branch = properties.getProperty("git.branch");
        gitInfo.commitTime = properties.getProperty("git.commit.time");
        gitInfo.buildTime = properties.getProperty("git.build.time");
        return gitInfo;
    }
}
