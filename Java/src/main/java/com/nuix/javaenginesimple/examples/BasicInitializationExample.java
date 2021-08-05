package com.nuix.javaenginesimple.examples;

import java.util.function.Consumer;

import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.Utilities;

/***
 * Basic example of using the EngineWrapper class to get a session of Nuix initialized and licensed.  For more detail
 * on the process of getting the Nuix engine setup and licensed, see the code for the EngineWrapper class.
 * @author Jason Wells
 *
 */
public class BasicInitializationExample {
	public static void main(String[] args) throws Exception {
		// =========================================================================================
		// * Create instance of EngineWrapper which we will delegate much of the initialization to *
		// =========================================================================================
		
		// Specify a custom location for our log files
		String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
		
		// Create an instance of engine wrapper, which will do the work of getting the Nuix bits initialized.
		// Engine wrapper will need to know what directory you engine release resides.
		EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\9.2.4.392", logDirectory);
		
		
		// =========================================================================================================
		// * Create LicenseFilter instance which will instruct EngineWrapper on how to choose an available license *
		// =========================================================================================================
		
		// LicenseFilter is used by EngineWrapper to select which license to obtain
		// from available licenses.
		LicenseFilter licenseFilter = wrapper.getLicenseFilter();
		licenseFilter.setMinWorkers(4);
		licenseFilter.addRequiredFeature("CASE_CREATION");
		
		
		// =================================================================================
		// * Example of getting license authentication details from command line arguments *
		// =================================================================================
		
		// Provided via: -DLicense.UserName=username
		String licenseUserName = System.getProperty("License.UserName");
		// Provided via: -DLicense.Password=password
		String licensePassword = System.getProperty("License.Password");
		
		if(licenseUserName != null && !licenseUserName.trim().isEmpty()) {
			wrapper.logger.info(String.format("License username was provided via argument -DLicense.UserName: %s",licenseUserName));
		}
		
		if(licensePassword != null && !licensePassword.trim().isEmpty()) {
			wrapper.logger.info("License password was provided via argument -DLicense.Password");
		}
		
		// =========================================================================================================
		// * Use EnginerWrapper to obtain a licensed Utilities instance (thus allowing you to use Nuix engine API) *
		// =========================================================================================================
		
		try {
			// Attempt to initialize Nuix with a dongle based license
//			wrapper.withDongleLicense(new Consumer<Utilities>(){
//				public void accept(Utilities utilities) {
//					// Here's where we would begin to make use of the Nuix API for
//					// the more interesting things like opening a case, searching ,tagging, etc
//					wrapper.logger.info("Looks like it worked! Now time to do something great.");
//					//TODO: Use Nuix to do stuff
//				}
//			});
			
			// Attempt to initialize Nuix with a server based license
			wrapper.trustAllCertificates();
			wrapper.withServerLicense("127.0.0.1", licenseUserName, licensePassword, new Consumer<Utilities>(){
				public void accept(Utilities utilities) {
					// Here's where we would begin to make use of the Nuix API for
					// the more interesting things like opening a case, searching ,tagging, etc
					wrapper.logger.info("Looks like it worked! Now time to do something great.");
					//TODO: Use Nuix to do stuff
				}
			});
			
			// Attempt to initialize Nuix with a cloud based license
//			wrapper.trustAllCertificates();
//			wrapper.withCloudLicense(licenseUserName, licensePassword, new Consumer<Utilities>() {
//				public void accept(Utilities utilities) {
//					// Here's where we would begin to make use of the Nuix API for
//					// the more interesting things like opening a case, searching ,tagging, etc
//					wrapper.logger.info("Looks like it worked! Now time to do something great.");
//					//TODO: Use Nuix to do stuff
//				}
//			});
			
		} catch (Exception e) {
			wrapper.logger.error("Unhandled exception",e);
			// Lets dump a diagnostics file since something went wrong and having
			// this may be helpful for trouble shooting
			NuixDiagnostics.saveDiagnostics("C:\\EngineDiagnostics");
		} finally {
			wrapper.close();
		}
	}
}

