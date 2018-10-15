package com.finplant.cryptoharvester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);
	
	public static void logError(String msg, Exception e) {
		LOG.error(msg, e);
		System.exit(1);
	}
	
	public static void logThrowable(String msg, Throwable e) {
		LOG.error(msg, e);
		System.exit(1);
	}
}
