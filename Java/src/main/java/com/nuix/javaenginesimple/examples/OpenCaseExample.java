package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.Case;
import nuix.Utilities;

/***
 * An example demonstrating opening an existing Nuix case.
 * 
 * See BasicInitializationExample for more details regarding the basic initialization steps being taken.
 * @author Jason Wells
 *
 */
public class OpenCaseExample {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger(OpenCaseExample.class);

	public static void main(String[] args) throws Exception {
		String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
		System.getProperties().put("nuix.logdir", logDirectory);
		
		Properties props = new Properties();
		InputStream log4jSettingsStream = OpenCaseExample.class.getResourceAsStream("/log4j.properties");
		props.load(log4jSettingsStream);
		PropertyConfigurator.configure(props);
		
		EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\8.8.1.131");
		
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
					File caseDirectory = new File("D:\\Cases\\MyNuixCase");
					Case nuixCase = null;
					
					try {
						// Attempt to open the case
						logger.info(String.format("Opening case: %s",caseDirectory.toString()));
						nuixCase = utilities.getCaseFactory().open(caseDirectory);
						logger.info("Case opened");
						
						// *** Do things with the case here ***
						long emailCount = nuixCase.count("kind:email");
						logger.info(String.format("Email Item Count: %s",emailCount));
						
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

