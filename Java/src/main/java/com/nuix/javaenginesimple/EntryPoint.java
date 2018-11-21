package com.nuix.javaenginesimple;

import java.util.function.Consumer;

import org.apache.log4j.Logger;

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
		
		// Create an instance of engine wrapper, which will do the work of getting the Nuix bits initialized.
		// Engine wrapper will need to know what directory you engine release resides.
		EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\7.6.5");
		
		try {
			// Attempt to initialize Nuix with a dongle based license
			wrapper.withDongleLicense(new Consumer<Utilities>(){
				public void accept(Utilities utilities) {
					// Here's where we would begin to make use of the Nuix API for
					// the more interesting things like opening a case, searching ,tagging, etc
					logger.info("Looks like it worked! Now time to do something great.");
					
					//TODO: Use Nuix to do stuff
				}
			});
			
			// Attempt to initialize Nuix with a server based license
//			wrapper.withServerLicense("username", "password", new Consumer<Utilities>(){
//				public void accept(Utilities utilities) {
//					// Here's where we would begin to make use of the Nuix API for
//					// the more interesting things like opening a case, searching ,tagging, etc
//					logger.info("Looks like it worked! Now time to do something great.");
//					
//					//TODO: Use Nuix to do stuff
//				}
//			});
			
		} catch (Exception e) {
			logger.error("Unhandled exception",e);
			// Lets dump a diagnostics file since something went wrong and having this may be helpful for trouble shooting
			NuixDiagnostics.saveDiagnostics("C:\\EngineDiagnostics");
		}
	}
}
