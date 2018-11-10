package com.nuix.javaenginesimple;

import java.util.List;

import org.apache.log4j.Logger;

import nuix.ThirdPartyDependency;
import nuix.ThirdPartyDependencyStatus;
import nuix.Utilities;

/***
 * Helper class for logging information about what third party dependencies are satisfied. 
 * @author Jason Wells
 *
 */
public class ThirdPartyDependencyChecker {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger("ThirdPartyDependencyChecker");
	
	/***
	 * Logs information about all Nuix third party dependencies
	 * @param utilities Needs an instance of Utilities to get access to third party dependency information
	 */
	public static void logAllDependencyInfo(Utilities utilities) {
		logger.info("Reviewing third party dependency statuses:");
		List<ThirdPartyDependency> dependencies = utilities.getThirdPartyDependencies();
		for(ThirdPartyDependency dependency : dependencies) {
			logDependencyInfo(dependency);
		}
	}
	
	/***
	 * Logs information about a single Nuix third party dependency
	 * @param dependency The third party dependency to log about
	 */
	public static void logDependencyInfo(ThirdPartyDependency dependency) {
		ThirdPartyDependencyStatus status = dependency.performCheck();
		logger.info(String.format(
				"[%s] '%s': %s",
				status.isAttentionRequired() ? " " : "X",
				dependency.getDescription(),
				status.getMessage()
		));
	}
}
