package com.aws.proserve.korea.event.generator.threading;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ActiveTasksThreadPoolExecutor extends ThreadPoolExecutor {
    private final ConcurrentHashMap<Runnable, Boolean> activeTasks = new ConcurrentHashMap<>();
	
	public ActiveTasksThreadPoolExecutor(
		int corePoolSize,
		int maximumPoolSize,
		long keepAliveTime,
		TimeUnit unit,
		BlockingQueue<Runnable> workQueue
	) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

	public ActiveTasksThreadPoolExecutor(
		int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory,
        RejectedExecutionHandler handler
	) {
		super(
			corePoolSize,
			maximumPoolSize,
			keepAliveTime,
			unit,
			workQueue,
			threadFactory,
			handler
		);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		activeTasks.put(r, Boolean.TRUE);
		super.beforeExecute(t, r);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		activeTasks.remove(r);
	}

	public ConcurrentHashMap<Runnable, Boolean> getActiveTasks() {
		return activeTasks;
	}
	
	public Set<Runnable> getActiveTasksSet() {
		// The returned set will not throw a ConcurrentModificationException.
		return activeTasks.keySet();
	}

}
