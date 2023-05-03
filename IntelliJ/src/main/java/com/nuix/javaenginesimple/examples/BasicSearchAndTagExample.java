package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.enginebaseline.NuixDiagnostics;

import nuix.Case;
import nuix.Utilities;

/***
 * An example demonstrating a very basic search and tag workflow in which we create a Map where each key is a search query
 * and each associated value is a tag applied to the responsive items.
 * 
 * See BasicInitializationExample for more details regarding the basic initialization steps being taken.
 * @author Jason Wells
 *
 */
public class BasicSearchAndTagExample {
	private static Logger logger = null;

	public static void main(String[] args) throws Exception {
		// Specify a custom location for our log files
		String logDirectory = String.format("%s/%s",System.getProperty("nuix.logDir"),DateTime.now().toString("YYYYMMDD_HHmmss"));

		// Create an instance of engine wrapper, which will do the work of getting the Nuix bits initialized.
		// Engine wrapper will need to know what directory your engine release resides.
		EngineWrapper wrapper = new EngineWrapper(System.getProperty("nuix.engineDir"), logDirectory);

		// Relying on log4j2 initializations in EngineWrapper creation, so we wait until after that to fetch our logger
		logger = LogManager.getLogger(BasicSearchAndTagExample.class);
		
		LicenseFilter licenseFilter = wrapper.getLicenseFilter();
		licenseFilter.setMinWorkers(4);
		licenseFilter.addRequiredFeature("CASE_CREATION");
		
		String licenseUserName = System.getProperty("License.UserName");
		String licensePassword = System.getProperty("License.Password");
		
		if(licenseUserName != null && !licenseUserName.trim().isEmpty()) {
			logger.info(String.format("License username was provided via argument -DLicense.UserName: %s",licenseUserName));
		}
		
		if(licensePassword != null && !licensePassword.trim().isEmpty()) {
			logger.info("License password was provided via argument -DLicense.Password");
		}
		
		// We are going to use a Map<String,String> in which the key is the search we will run and the associated
		// value is the tag we will apply to the results.
		Map<String,String> searchAndTagData = new HashMap<String,String>();
		searchAndTagData.put("cat", "Animals|Cat");
		searchAndTagData.put("dog", "Animals|Dog");
		searchAndTagData.put("mouse", "Animals|Mouse");
		
		try {
			wrapper.trustAllCertificates();
			wrapper.withCloudLicense(licenseUserName, licensePassword, new Consumer<Utilities>() {
				public void accept(Utilities utilities) {
					File caseDirectory = new File("D:\\Cases\\MyNuixCase");
					Case nuixCase = null;
					
					try {
						// Attempt to open the case
						logger.info(String.format("Opening case: %s",caseDirectory.toString()));
						nuixCase = utilities.getCaseFactory().open(caseDirectory);
						logger.info("Case opened");
						
						// Iterate each key value pair in the searchAndTagData Map
						for(Map.Entry<String, String> searchAndTagEntry : searchAndTagData.entrySet()) {
							String query = searchAndTagEntry.getKey();
							String tag = searchAndTagEntry.getValue();
							logger.info(String.format("==== Tag: %s / Query: %s ====", tag, query));
							
							logger.info(String.format("Searching query: %s", query));
							Set<nuix.Item> hits = nuixCase.searchUnsorted(query);
							logger.info(String.format("Hits: %s", hits.size()));
							
							logger.info(String.format("Applying tag '%s' to %s items...", tag, hits.size()));
							utilities.getBulkAnnotater().addTag(tag, hits);
							logger.info("Tag applied to items");
						}
						
						// Note that nuixCase is closed in finally block below
					} catch (IOException exc) {
						logger.error(String.format("Error while opening case: %s",caseDirectory.toString()),exc);
					} finally {
						// Make sure we close the case
						if(nuixCase != null) {
							logger.info(String.format("Closing case: %s",caseDirectory.toString()));
							nuixCase.close();
						}
					}
				}
			});
			
		} catch (Exception e) {
			logger.error("Unhandled exception",e);
			NuixDiagnostics.saveDiagnosticsToDirectory("C:\\EngineDiagnostics");
		} finally {
			wrapper.close();
		}
	}
}

