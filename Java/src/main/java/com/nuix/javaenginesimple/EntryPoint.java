package com.nuix.javaenginesimple;

import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import nuix.Utilities;

/***
 * Entry point into the application (contains a void main method).
 * @author Jason Wells
 *
 */
public class EntryPoint {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger("EntryPoint");

	public static void main(String[] args) throws Exception {
		// Specify a custom location for our log files
		String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
		System.getProperties().put("nuix.logdir", logDirectory);
		
		// Configure log4j using properties file, note that this should be performed after
		// setting "nuix.logdir" to have that setting determine log output directory!
		Properties props = new Properties();
		InputStream log4jSettingsStream = EntryPoint.class.getResourceAsStream("/log4j.properties");
		props.load(log4jSettingsStream);
		PropertyConfigurator.configure(props);
				
		// Create an instance of engine wrapper, which will do the work of getting the Nuix bits initialized.
		// Engine wrapper will need to know what directory you engine release resides.
		EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\8.4.2.466");
		
		// LicenseFilter is used by EngineWrapper to select which license to obtain
		// from available licenses.
		LicenseFilter licenseFilter = wrapper.getLicenseFilter();
		licenseFilter.setMinWorkers(4);
		licenseFilter.addRequiredFeature("CASE_CREATION");
		
		try {
			// Attempt to initialize Nuix with a dongle based license
//			wrapper.withDongleLicense(new Consumer<Utilities>(){
//				public void accept(Utilities utilities) {
//					// Here's where we would begin to make use of the Nuix API for
//					// the more interesting things like opening a case, searching ,tagging, etc
//					logger.info("Looks like it worked! Now time to do something great.");
//					
//					//TODO: Use Nuix to do stuff
//				}
//			});
			
			// Attempt to initialize Nuix with a server based license
//			wrapper.trustAllCertificates();
//			wrapper.withServerLicense("127.0.0.1", "nuix", "nuixpassword", new Consumer<Utilities>(){
//				public void accept(Utilities utilities) {
//					// Here's where we would begin to make use of the Nuix API for
//					// the more interesting things like opening a case, searching ,tagging, etc
//					logger.info("Looks like it worked! Now time to do something great.");
//					//TODO: Use Nuix to do stuff
//				}
//			});
			
			wrapper.trustAllCertificates();
			wrapper.withCloudLicense("nuixuser", "nuixpassword", new Consumer<Utilities>() {
				public void accept(Utilities utilities) {
					// Here's where we would begin to make use of the Nuix API for
					// the more interesting things like opening a case, searching ,tagging, etc
					logger.info("Looks like it worked! Now time to do something great.");
					//TODO: Use Nuix to do stuff
				}
			});
			
		} catch (Exception e) {
			logger.error("Unhandled exception",e);
			// Lets dump a diagnostics file since something went wrong and having
			// this may be helpful for trouble shooting
			NuixDiagnostics.saveDiagnostics("C:\\EngineDiagnostics");
		}
	}
}
