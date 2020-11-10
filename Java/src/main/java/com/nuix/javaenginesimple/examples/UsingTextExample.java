package com.nuix.javaenginesimple.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
import nuix.ReaderReadLogic;
import nuix.Text;
import nuix.Utilities;

public class UsingTextExample {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger(BasicSearchAndTagExample.class);

	public static void main(String[] args) throws Exception {
		String logDirectory = String.format("C:\\NuixEngineLogs\\%s",DateTime.now().toString("YYYYMMDD_HHmmss"));
		System.getProperties().put("nuix.logdir", logDirectory);
		
		Properties props = new Properties();
		InputStream log4jSettingsStream = BasicSearchAndTagExample.class.getResourceAsStream("/log4j.properties");
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
		
		String query = "flag:audited AND content:*";
		
		// Contrived example where we will iterate each line of the item's text and when a given
		// line is blank after trimming whitespace, we add 1 to our blank line count, ultimately
		// returning the number of blank lines we encountered.
		ReaderReadLogic<Integer> textOperation = new ReaderReadLogic<Integer>() {
			@Override
			public Integer withReader(Reader reader) throws IOException {
				int blankLineCount = 0;
				BufferedReader buffer = new BufferedReader(reader);
				String line;
				while((line = buffer.readLine()) != null) {
					if(line.trim().isEmpty()) {
						blankLineCount++;
					}
				}
				return blankLineCount;
			}
		};
		
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
						
						Set<Item> items = nuixCase.searchUnsorted(query);
						for(Item item : items) {
							Text itemTextObject = item.getTextObject();
							// Have our text operation do something with the items text.  Since this operation is handed a
							// Reader rather than attempting to construct one solitary string in memory, this operation should
							// behave better when an item has an especially large text value.
							int blankLineCount = itemTextObject.usingText(textOperation);
							
							// Record the number of blank lines we encountered as custom metadata
							item.getCustomMetadata().putInteger("ContentBlankLines", blankLineCount);
							
							logger.info(String.format("%s has %s blank lines in its content text", item.getGuid(), blankLineCount));
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
