package com.hoccer.talk.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a named threads named after the following pattern:
 * <p/>
 * <name>-pool-<#>-thread-<#>
 * <p/>
 * for a given name.
 * Intended for usage in Executors like e.g.
 * <p/>
 * <pre>
 *     myExecutor = Executors.newScheduledThreadPool(4, new NamedThreadFactory("worker-agent"));
 * </pre>
 * <p/>
 * *Note:* Code is currently copied from java.util.concurrent.DefaultThreadFactory
 * since DefaultThreadFactory is a package-private static nested class
 */

public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public NamedThreadFactory(String name) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = name + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);

        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}