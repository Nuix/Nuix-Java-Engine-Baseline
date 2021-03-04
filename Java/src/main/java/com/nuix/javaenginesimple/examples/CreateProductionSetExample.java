package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.Case;
import nuix.Item;
import nuix.ProductionSet;
import nuix.ProductionSetItem;
import nuix.Utilities;

public class CreateProductionSetExample {
	// Obtain a logger instance for this class
		private final static Logger logger = Logger.getLogger(CreateProductionSetExample.class);

		public static void main(String[] args) throws Exception {
			String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
			System.getProperties().put("nuix.logdir", logDirectory);
			
			Properties props = new Properties();
			InputStream log4jSettingsStream = OpenCaseExample.class.getResourceAsStream("/log4j.properties");
			props.load(log4jSettingsStream);
			PropertyConfigurator.configure(props);
			
			EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\9.0.1.325");
			
			LicenseFilter licenseFilter = wrapper.getLicenseFilter();
			licenseFilter.setMinWorkers(4);
			licenseFilter.addRequiredFeature("CASE_CREATION");
			licenseFilter.addRequiredFeature("EXPORT_ITEMS");
			
			String licenseUserName = System.getProperty("License.UserName");
			String licensePassword = System.getProperty("License.Password");
			
			if(licenseUserName != null && !licenseUserName.trim().isEmpty()) {
				logger.info(String.format("License username was provided via argument -DLicense.UserName: %s",licenseUserName));
			}
			
			if(licensePassword != null && !licensePassword.trim().isEmpty()) {
				logger.info("License password was provided via argument -DLicense.Password");
			}
			
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
							
							// Create our production set
							ProductionSet productionSet = nuixCase.newProductionSet("TestProductionSet_"+System.currentTimeMillis());
							
							// Define our numbering options
							Map<String,Object> numberingOptions = new HashMap<String,Object>();
							numberingOptions.put("prefix", "ABC-");
							
							Map<String,Object> docIdOptions = new HashMap<String,Object>();
							docIdOptions.put("minWidth", 9);
							docIdOptions.put("startAt", 1);
							numberingOptions.put("documentId", docIdOptions);
							
							productionSet.setNumberingOptions(numberingOptions);
							
							// Obtain items that we will be adding to the production set
							List<Item> items = nuixCase.search("flag:audited");
							
							// Add these items to the production set
							productionSet.addItems(items);
							
							// Lets report the first and last number in this production set
							List<ProductionSetItem> productionSetItems = productionSet.getProductionSetItems();
							String firstNumber = productionSetItems.get(0).getDocumentNumber().toString();
							String lastNumber = productionSetItems.get(productionSetItems.size()-1).getDocumentNumber().toString();
							logger.info(String.format("Production Set Created with Nubmers %s to %s", firstNumber, lastNumber));
							
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
