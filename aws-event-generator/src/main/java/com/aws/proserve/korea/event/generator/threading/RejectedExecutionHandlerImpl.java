package com.aws.proserve.korea.event.generator.threading;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.aws.proserve.korea.event.generator.utils.LoggerUtils;

public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		LoggerUtils.getLogger().debug(r.toString() + " is rejected");
	}

}
