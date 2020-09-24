package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.BatchExporter;
import nuix.Case;
import nuix.Item;
import nuix.ItemEventCallback;
import nuix.ItemEventInfo;
import nuix.Utilities;

public class BasicExportExample {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger(BasicExportExample.class);

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
					File exportDirectory = new File("D:\\Exports\\MyNuixExport");
					
					Case nuixCase = null;
					
					try {
						// Attempt to open the case
						logger.info(String.format("Opening case: %s",caseDirectory.toString()));
						nuixCase = utilities.getCaseFactory().open(caseDirectory);
						logger.info("Case opened");
						
						BatchExporter exporter = utilities.createBatchExporter(exportDirectory);
						
						// We will using the same naming type for all products, possible choices:
						// - "document_id" (e.g. "ABC-000000001.pdf"), requires license feature  EXPORT_LEGAL
						// - "document_id_with_page" (e.g. "ABC-000000001_11.pdf"), requires license feature  EXPORT_LEGAL
						// - "page_only" (e.g. "0001.pdf"), requires license feature  EXPORT_LEGAL
						// - "full" (e.g. "ABC0010010001.pdf"), requires license feature  EXPORT_LEGAL
						// - "full_with_periods" (e.g. "ABC.001.001.0001.pdf"), requires license feature  EXPORT_LEGAL
						// - "item_name" (e.g. "original-name.pdf"), requires license feature  EXPORT_ITEMS
						// - "item_name_with_path" (e.g. "mailbox/inbox/original-name.pdf"), requires license feature  EXPORT_ITEMS
						// - "guid" (e.g. "04d/04dd8e72-f087-4f66-848a-6585bce732d5.pdf"), requires license feature  EXPORT_ITEMS
						// - "md5" (e.g. "790/79054025255fb1a26e4bc422aef54eb4.pdf"), requires license feature  EXPORT_ITEMS
						String productNaming = "guid";
						
						// ======================================
						// * Add and Configure Native Exporting *
						// ======================================
						
						logger.info("Configuring settings for NATIVES...");
						
						Map<String,Object> nativeSettings = new HashMap<String,Object>();
						nativeSettings.put("naming",productNaming);
						nativeSettings.put("path","NATIVES");
						nativeSettings.put("mailFormat","eml");
						nativeSettings.put("includeAttachments",true);
						
						// Add native product to our exporter
						exporter.addProduct("native", nativeSettings);
						
						// =========================================
						// * Add and Configure Text File Exporting *
						// =========================================
						
						logger.info("Configuring settings for TEXT...");
						
						Map<String,Object> textFileSettings = new HashMap<String,Object>();
						textFileSettings.put("naming",productNaming);
						textFileSettings.put("path","TEXT");
						
						// Add native product to our exporter
						exporter.addProduct("text", textFileSettings);
						
						// ===================================
						// * Add and Configure PDF Exporting *
						// ===================================
						
						logger.info("Configuring settings for PDFs...");
						
						Map<String,Object> pdfSettings = new HashMap<String,Object>();
						pdfSettings.put("naming",productNaming);
						pdfSettings.put("path","PDF");
						pdfSettings.put("regenerateStored",false);
						
						// Add native product to our exporter
						exporter.addProduct("pdf", pdfSettings);
						
						// ====================================
						// * Add and Configure TIFF Exporting *
						// ====================================
						
						logger.info("Configuring settings for TIFFs...");
						
						Map<String,Object> tiffSettings = new HashMap<String,Object>();
						tiffSettings.put("naming",productNaming);
						tiffSettings.put("path","TIFF");
						tiffSettings.put("regenerateStored",false);
						tiffSettings.put("multiPageTiff",false);
						tiffSettings.put("tiffDpi",300);
						tiffSettings.put("tiffFormat","MONOCHROME_CCITT_T6_G4");
						
						// Add native product to our exporter
						exporter.addProduct("tiff", tiffSettings);
						
						// ================================
						// * Add Concordance DAT Loadfile *
						// ================================
						
						logger.info("Configuring settings for Concordance DAT...");
						
						Map<String,Object> concordanceLoadfileSettings = new HashMap<String,Object>();
						concordanceLoadfileSettings.put("metadataProfile","Default");
						concordanceLoadfileSettings.put("encoding","UTF-8");
						
						exporter.addLoadFile("concordance",concordanceLoadfileSettings);
						
						// =============================
						// * Configure Worker Settings *
						// =============================
						
						// Now we are going to instruct the processor how many workers to use to load the data.  See API documentation for
						// ParallelProcessingConfigurable.setParallelProcessingSettings for list of settings and what they do.
						Map<String,Object> parallelProcessingSettings = new HashMap<String,Object>();
						int licenseWorkerCount = utilities.getLicence().getWorkers();
						parallelProcessingSettings.put("workerCount",licenseWorkerCount);
						parallelProcessingSettings.put("workerTemp","D:\\WorkerTemp");
						
						exporter.setParallelProcessingSettings(parallelProcessingSettings);
						
						// =============================
						// * Hook Up Progress Callback *
						// =============================
						
						logger.info("Setting up progress callback...");
						
						// Track error count
						AtomicInteger errorCount = new AtomicInteger();
						
						exporter.whenItemEventOccurs(new ItemEventCallback() {
							
							// Use this to track when we reported progress last
							long lastProgressMillis = System.currentTimeMillis();
							
							@Override
							public void itemProcessed(ItemEventInfo info) {
								// Report progress if it has been at least 5 seconds (5000 milliseconds) since
								// the last time we reported progress
								long currentTimeMillis = System.currentTimeMillis();
								if(currentTimeMillis - lastProgressMillis > 5 * 1000) {
									String progressMessage = String.format(
										"Stage: %s, Progress: %s, Errors: %s",
										info.getStage(), info.getStageCount(), errorCount.get());
									logger.info(progressMessage);
									lastProgressMillis = System.currentTimeMillis();
								}
								
								// If this particular item had an error we always record this
								Exception possibleItemException = info.getFailure();
								if(possibleItemException != null) {
									Item item = info.getItem();
									errorCount.incrementAndGet();
									String errorMessage = String.format(
										"Error while exporting item %s/%s: %s",
										item.getGuid(), item.getLocalisedName(), possibleItemException.getMessage()); 
									logger.error(errorMessage);
								}
							}
						});
						
						// ===================
						// * Begin Exporting *
						// ===================
						
						String itemsToExportQuery = "kind:email AND flag:top_level";
						
						logger.info(String.format("Searching: %s",itemsToExportQuery));
						List<Item> itemsToExport = nuixCase.search(itemsToExportQuery);
						logger.info(String.format("Responsive Items: %s",itemsToExport.size()));
						
						logger.info("Beginning export...");
						exporter.exportItems(itemsToExport);
						logger.info("Export completed");
						
						logger.info(String.format("Errors: %s",errorCount.get()));
						if(errorCount.get() > 0) {
							logger.info("Review logs for more details regarding export errors");
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
