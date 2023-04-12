package com.nuix.javaenginesimple;

import java.util.List;
import java.util.ArrayList;
import java.util.StringJoiner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nuix.LicenceProperties;
import nuix.engine.AvailableLicence;

/***
 * Helper class for logging what features are present on a given license.
 * @author Jason Wells
 *
 */
public class LicenseFeaturesLogger {
	// Obtain a logger instance for this class
	private final static Logger logger = LogManager.getLogger("LicenseFeatures");

	// List of license features
	// List generated from 9.2 license profiles documentation found here:
	// https://download.nuix.com/releases/desktop/stable/docs/en/reference/licence-profiles.html
	private static String[] knownFeatures = new String[] {
		"ANALYSIS",
		"AOS_DATA",
		"AUTOMATIC_CLASSIFIER_EDITING",
		"AXS_ONE",
		"CASE_CREATION",
		"CUSTOM_NAMED_ENTITIES",
		"CYBER_CONTEXT",
		"DESKTOP",
		"ELASTIC_SEARCH",
		"EXCHANGE_WS",
		"EXPORT_CASE_SUBSET",
		"EXPORT_DISCOVER",
		"EXPORT_ITEMS",
		"EXPORT_LEGAL",
		"EXPORT_SINGLE_ITEM",
		"EXPORT_VIEW",
		"FAST_REVIEW",
		"FRONT_LOAD_METADATA",
		"GENERAL_DATA",
		"GRAPH",
		"GWAVA",
		"IMAP_POP",
		"LIGHT_SPEED",
		"LOG_STASH",
		"LOTUS_NOTES",
		"MAIL_XTENDER",
		"METADATA_IMPORT",
		"MICROSOFT_GRAPH",
		"MOBILE_DEVICE_IMAGING",
		"NETWORK_DATA",
		"OCR_PROCESSING",
		"OTHER_EMAIL",
		"OUTLOOK",
		"OUTLOOK_EXPRESS",
		"PARTIAL_LOAD",
		"PRODUCTION_SET",
		"SCRIPTING",
		"SYMANTEC_VAULT",
		"UNRESTRICTED_CASE_ACCESS",
		"WORKER",
		"WORKER_SCRIPTING",
		"ZANTAZ",
	};

	/***
	 * Returns a String array containing a list of known license features
	 * @return Array of known license features
	 */
	public static String[] getKnownFeatures() {
		return knownFeatures;
	}

	public static List<String> getLicenseFeatures(LicenceProperties license){
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < knownFeatures.length; i++) {
			String feature = knownFeatures[i];
			boolean hasFeature = license.hasFeature(feature);
			if(hasFeature) {
				result.add(feature);
			}
		}
		return result;
	}

	public static String summarizeLicense(AvailableLicence license) {
		String result = String.format("[ %s / %s / %s, %s, Count:%s, Workers: %s, Features: %s]",
				license.getSource().getLocation(),
				license.getSource().getType(),
				license.getShortName(),
				license.getDescription(),
				license.getCount(),
				((LicenceProperties)license).getWorkers(),
				String.join("; ", getLicenseFeatures(license))
				);
		return result;
	}

	/***
	 * Logs a listing of whether each feature is present or not on the provided license.
	 * @param license The license to log feature presence information about
	 */
	public static void logFeaturesOfLicense(LicenceProperties license) {
		StringJoiner message = new StringJoiner("\n");
		message.add("License Features:");
		for (int i = 0; i < knownFeatures.length; i++) {
			String feature = knownFeatures[i];
			boolean hasFeature = license.hasFeature(feature);
			message.add(String.format("[%s] %s", hasFeature ? "X":" ", feature));
		}
		logger.info(message.toString());
	}
}
