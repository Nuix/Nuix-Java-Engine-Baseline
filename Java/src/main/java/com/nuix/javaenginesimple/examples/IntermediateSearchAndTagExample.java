package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.Case;
import nuix.Item;
import nuix.Utilities;

/***
 * This example builds upon the basic concepts demonstrated in BaseSearchAndTagExample by introducing a few new concepts:
 * - Optionally including family member items of responsive items
 * - Optionally removing excluded items before tagging step (API does not automatically remove them like the GUI does!)
 * 
 * See BasicInitializationExample for more details regarding the basic initialization steps being taken.
 * @author Jason Wells
 *
 */
public class IntermediateSearchAndTagExample {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger(IntermediateSearchAndTagExample.class);

	public static void main(String[] args) throws Exception {
		String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
		System.getProperties().put("nuix.logdir", logDirectory);
		
		Properties props = new Properties();
		InputStream log4jSettingsStream = IntermediateSearchAndTagExample.class.getResourceAsStream("/log4j.properties");
		props.load(log4jSettingsStream);
		PropertyConfigurator.configure(props);
		
		EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\8.4.2.466");
		
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
		
		// Specify if we want to include family members of items resposive to a search
		boolean includeFamilyMembers = true;
		
		// Specify if we want to remove excluded items before tagging
		boolean removeExcludedItems = true;
		
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
						
						// Nuix API methods do not automatically remove excluded items like the GUI does, so
						// we need to include logic for this ourselves if we wish to get a similar result as
						// we might in the GUI.  If we are going to be removing excluded items, lets get a
						// collection of the excluded items in this case that we can later remove from results.
						Set<Item> caseExcludedItems = null;
						if(removeExcludedItems) {
							caseExcludedItems = nuixCase.searchUnsorted("has-exclusion:1");	
						}
						
						// Iterate each key value pair in the searchAndTagData Map
						for(Map.Entry<String, String> searchAndTagEntry : searchAndTagData.entrySet()) {
							String query = searchAndTagEntry.getKey();
							String tag = searchAndTagEntry.getValue();
							logger.info(String.format("==== Tag: %s / Query: %s ====", tag, query));
							
							// Run our search, generally best to use one of the SearchUnsorted methods unless
							// we have reason not to.  This class of search methods returns an item collection
							// that will generally performs better when passed through one of the API's item
							// collection methods (for example ItemUtility.findFamilies).
							logger.info(String.format("Searching query: %s", query));
							Set<Item> items = nuixCase.searchUnsorted(query);
							logger.info(String.format("Hits: %s", items.size()));
							
							// If we want family members included, we use ItemUtility.findFamilies() to get
							// a new Set of items which includes the input items and their family members.
							if(includeFamilyMembers) {
								logger.info("Including family members...");
								items = utilities.getItemUtility().findFamilies(items);
								logger.info(String.format("Hits + Family Members: %s", items.size()));	
							}
							
							// If we want to remove excluded items, we difference (subtract) the excluded items we
							// obtained earlier from our items using ItemUtility.difference() which returns a new Set of
							// items representing the items of the first argument with the items in the second argument removed.
							if(removeExcludedItems) {
								logger.info("Removing excluded items...");
								items = utilities.getItemUtility().difference(items, caseExcludedItems);
								logger.info(String.format("(Hits + Family Members) - excluded: %s", items.size()));
							}
							
							logger.info(String.format("Applying tag '%s' to %s items...", tag, items.size()));
							utilities.getBulkAnnotater().addTag(tag, items);
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
			NuixDiagnostics.saveDiagnostics("C:\\EngineDiagnostics");
		}
	}
}

