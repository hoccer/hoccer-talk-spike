package com.hoccer.talk.util;

import de.flapdoodle.embed.process.io.IStreamProcessor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Created by martin on 14.04.14.
 */
public class MongoStreamLogger implements IStreamProcessor {

    private final Priority logLevel;
    private final Logger log;

    public MongoStreamLogger(String name, Priority level) {
        logLevel = level;
        log = Logger.getLogger(name);
    }

    @Override
    public void process(String s) {
        log.log(logLevel, s);
    }

    @Override
    public void onProcessed() {

    }
}