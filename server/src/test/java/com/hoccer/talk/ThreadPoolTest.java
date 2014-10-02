
package com.hoccer.talk;

import com.hoccer.talk.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolTest {

    private static final int TASKS = 5;
    private static final int THREADS = 5;
    private final AtomicInteger ready = new AtomicInteger(0);
    private Executor mExecutor;

    @Test
    public void threadPoolTest() throws Exception {
        System.out.println("ThreadPoolTest starting");

        mExecutor = Executors.newScheduledThreadPool(
                THREADS,
                new NamedThreadFactory("ThreadPoolTest")
        );

        for (int i = 0; i < TASKS; ++i) {
            mExecutor.execute(newTask(i));
        }

        while (ready.get() != TASKS) {
            System.out.println("ThreadPoolTest ready="+ready.get()+",thread="+Thread.currentThread());
            synchronized (this) {
                wait(1000);
            }
        }
        System.out.println("done test");
    }

    class TestTask {
        final ReentrantLock mLock;
        final Condition mCondition;
        final int mTaskId;
        boolean mSignaled;

        TestTask(final int taskId) {
            mTaskId = taskId;
            mLock = new ReentrantLock();
            mCondition = mLock.newCondition();
            mSignaled = false;
        }

        void performConditionWait() {
            System.out.println("performConditionWait task="+mTaskId+" starting on Thread "+Thread.currentThread()+", this="+this);
            mLock.lock();
            try {
                System.out.println("performConditionWait task="+mTaskId+" waiting on Thread "+Thread.currentThread()+", this="+this);
                mCondition.await(1000, TimeUnit.MILLISECONDS);
                System.out.println("performConditionWait task="+mTaskId+" wakeup on Thread "+Thread.currentThread()+", this="+this);
                if (!mSignaled) {
                    throw new RuntimeException("timeout");
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                mLock.unlock();
            }
      }

        void performWakeup() {
            System.out.println("performWakeup task="+mTaskId+" starting on Thread "+Thread.currentThread()+", this="+this);
            mLock.lock();
            try {
                System.out.println("actually performing wakeup task="+mTaskId+" on Thread "+Thread.currentThread()+", this="+this);
                mSignaled = true;
                mCondition.signalAll();
            }
            finally {
                mLock.unlock();
            }
            System.out.println("performWakeup task="+mTaskId+" done on Thread "+Thread.currentThread()+", this="+this);
        }

        void performTimedWait() {
            System.out.println("performTimedWait task="+mTaskId+" starting on Thread "+Thread.currentThread()+", this="+this);
            try {
                synchronized (this) {
                    this.wait(500);
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            } catch (final Throwable t) {
                System.out.println("Has thrown t="+t);
            }
            System.out.println("performTimedWait task="+mTaskId+" done on Thread "+Thread.currentThread()+", this="+this);

        }
    }

    Runnable newTask(final int id) {
        return new Runnable() {
            @Override
            public void run() {

                final TestTask task = new TestTask(id);
                System.out.println("testRunnable id +"+id+" starting on Thread "+Thread.currentThread()+", this="+this);

                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                        task.performTimedWait();
                        task.performWakeup();
                        } catch (final Throwable t) {
                            System.out.println("Wait/signal on task "+id+" has thrown t="+t);
                        }
                    }
                });

                try {
                    task.performConditionWait();
                } catch (final Throwable t) {
                    System.out.println("Wait on task "+id+" has thrown t="+t);
                }
                System.out.println("testRunnable id +"+id+" finished on Thread "+Thread.currentThread());
                ready.incrementAndGet();
            }
        };
    }



}
