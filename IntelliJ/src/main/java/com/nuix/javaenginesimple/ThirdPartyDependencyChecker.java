package com.nuix.javaenginesimple;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nuix.ThirdPartyDependency;
import nuix.ThirdPartyDependencyStatus;
import nuix.Utilities;

/***
 * Helper class for logging information about what third party dependencies are satisfied. 
 * @author Jason Wells
 */
public class ThirdPartyDependencyChecker {
	// Obtain a logger instance for this class
	private final static Logger logger = LogManager.getLogger("ThirdPartyDependencyChecker");
	
	/***
	 * Logs information about all Nuix third party dependencies
	 * @param utilities Needs an instance of Utilities to get access to third party dependency information
	 */
	public static void logAllDependencyInfo(Utilities utilities) {
		logger.info("Reviewing third party dependency statuses:");
		try {
			List<ThirdPartyDependency> dependencies = utilities.getThirdPartyDependencies();
			for(ThirdPartyDependency dependency : dependencies) {
				try {
					ThirdPartyDependencyStatus status = dependency.performCheck();
					logger.info(String.format(
							"[%s] '%s': %s",
							status.isAttentionRequired() ? " " : "X",
							dependency.getDescription(),
							status.getMessage()
					));
				} catch (Exception e) {
					logger.error(String.format(
							"[!] '%s': Error Checking Status: %s",
							dependency.getDescription(),
							e.getMessage()
					));
				}
			}
		} catch (Exception e) {
			logger.error("Error while fetching list of third party dependencies",e);
		}
	}
}
