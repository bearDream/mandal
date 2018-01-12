package cn.ching.mandal.common;

import com.sun.org.apache.regexp.internal.RE;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2018/1/12
 * named thread factory, easy to manage
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class NamedThreadFactory implements ThreadFactory{

    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    private final AtomicInteger mThreadNum = new AtomicInteger(1);

    private final String mPrefix;

    private final boolean mDaemon;

    private final ThreadGroup mGroup;

    public NamedThreadFactory(){
        this("pool-" + POOL_SEQ.getAndIncrement(), false);
    }

    public NamedThreadFactory(String prefix){
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean daemon){
        mPrefix = prefix + "-thread-";
        mDaemon = daemon;
        SecurityManager manager = System.getSecurityManager();
        mGroup = (Objects.isNull(manager)) ? Thread.currentThread().getThreadGroup() : manager.getThreadGroup();
    }

    public ThreadGroup getThreadGroup() {
        return mGroup;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = mPrefix + mThreadNum.getAndIncrement();
        Thread t = new Thread(mGroup, r, name, 0);
        t.setDaemon(mDaemon);
        return t;
    }
}
