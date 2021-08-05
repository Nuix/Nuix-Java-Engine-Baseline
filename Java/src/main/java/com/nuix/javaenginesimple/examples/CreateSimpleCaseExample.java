package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.Case;
import nuix.Utilities;

/***
 * An example demonstrating creation of a new Nuix simple case.
 * 
 * See BasicInitializationExample for more details regarding the basic initialization steps being taken.
 * @author Jason Wells
 *
 */
public class CreateSimpleCaseExample {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger(CreateSimpleCaseExample.class);

	public static void main(String[] args) throws Exception {
		String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
		EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\9.2.4.392",logDirectory);
		
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
		
		try {
			wrapper.trustAllCertificates();
			wrapper.withCloudLicense(licenseUserName, licensePassword, new Consumer<Utilities>() {
				public void accept(Utilities utilities) {
					File caseDirectory = new File("D:\\Cases\\MyNuixSimpleCase");
					
					// Specify additional settings to use when creating case
					Map<String,Object> caseSettings = new HashMap<String,Object>();
					caseSettings.put("compound", false);
					caseSettings.put("name","My Nuix Simple Case");
					caseSettings.put("description", "A Nuix case created using the Java Engine API");
					caseSettings.put("investigator", "Investigator Name Here");
					
					Case nuixCase = null;
					
					try {
						logger.info(String.format("Creating case: %s",caseDirectory.toString()));
						nuixCase = utilities.getCaseFactory().create(caseDirectory,caseSettings);
						logger.info("Case created");
						
						// *** Do things with the case here ***
						
						// Note that nuixCase is closed in finally block below
					} catch (IOException exc) {
						logger.error(String.format("Error while creating case: %s",caseDirectory.toString()),exc);
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
		} finally {
			wrapper.close();
		}
	}
}

