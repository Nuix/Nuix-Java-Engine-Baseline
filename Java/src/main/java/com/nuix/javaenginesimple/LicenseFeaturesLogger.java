package com.nuix.javaenginesimple;

import org.apache.log4j.Logger;

import nuix.LicenceProperties;

/***
 * Helper class for logging what features are present on a given license.
 * @author Jason Wells
 *
 */
public class LicenseFeaturesLogger {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger("LicenseFeatures");
	
	// List of license features
	// List generated from 7.6 license profiles documentation found here:
	// https://download.nuix.com/releases/desktop/stable/docs/en/reference/licence-profiles.html
	private static String[] knownFeatures = new String[] {
		"OTHER_EMAIL",
		"LOG_STASH",
		"NETWORK_DATA",
		"OUTLOOK",
		"GENERAL_DATA",
		"DESKTOP",
		"OUTLOOK_EXPRESS",
		"LOTUS_NOTES",
		"GRAPH",
		"IMAP_POP",
		"CYBER_CONTEXT",
		"ANALYSIS",
		"SCRIPTING",
		"EXPORT_SINGLE_ITEM",
		"EXPORT_ITEMS",
		"EXPORT_VIEW",
		"OCR_PROCESSING",
		"WORKER_SCRIPTING",
		"MOBILE_DEVICE_IMAGING",
		"CASE_CREATION",
		"PRODUCTION_SET",
		"PARTIAL_LOAD",
		"EXPORT_LEGAL",
		"ELASTIC_SEARCH",
		"EXPORT_CASE_SUBSET",
		"EXCHANGE_WS",
		"FAST_REVIEW",
		"AUTOMATIC_CLASSIFIER_EDITING",
		"WORKER",
		"METADATA_IMPORT",
		"UNRESTRICTED_CASE_ACCESS",
		"SYMANTEC_VAULT",
		"MAIL_XTENDER",
		"AXS_ONE",
		"ZANTAZ",
		"SOCIAL_MEDIA",
		"LIGHT_SPEED",
		"GWAVA",
		"AOS_DATA",
	};

	/***
	 * Returns a String array containing a list of known license features
	 * @return Array of known license features
	 */
	public static String[] getKnownFeatures() {
		return knownFeatures;
	}
	
	/***
	 * Logs a listing of whether each feature is present or not on the provided license.
	 * @param license The license to log feature presence information about
	 */
	public static void logFeaturesOfLicense(LicenceProperties license) {
		logger.info("License Features:");
		for (int i = 0; i < knownFeatures.length; i++) {
			String feature = knownFeatures[i];
			boolean hasFeature = license.hasFeature(feature);
			logger.info(String.format("[%s] %s", hasFeature ? "X":" ", feature));
		}
	}
}
