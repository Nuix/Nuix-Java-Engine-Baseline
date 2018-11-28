package com.nuix.javaenginesimple;

import java.util.List;
import java.util.StringJoiner;

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
		StringJoiner message = new StringJoiner("\n");
		message.add("Reviewing third party dependency statuses:");
		List<ThirdPartyDependency> dependencies = utilities.getThirdPartyDependencies();
		for(ThirdPartyDependency dependency : dependencies) {
			ThirdPartyDependencyStatus status = dependency.performCheck();
			message.add(String.format(
					"[%s] '%s': %s",
					status.isAttentionRequired() ? " " : "X",
					dependency.getDescription(),
					status.getMessage()
			));
		}
		logger.info(message.toString());
	}
}
