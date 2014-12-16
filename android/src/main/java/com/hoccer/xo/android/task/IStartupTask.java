package com.hoccer.xo.android.task;

import com.hoccer.xo.android.XoApplication;

/*
 * Interface of a task run on startup. Derived classes need a default constructor for correct instantiation.
 * @see StartupTasks
 */
public interface IStartupTask {
    public void execute(XoApplication application);
}
