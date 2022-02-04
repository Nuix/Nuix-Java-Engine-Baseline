package com.nuix.javaenginesimple.examples;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.nuix.javaenginesimple.EngineWrapper;
import com.nuix.javaenginesimple.LicenseFilter;
import com.nuix.javaenginesimple.NuixDiagnostics;

import nuix.EvidenceContainer;
import nuix.ItemProcessedCallback;
import nuix.ProcessedItem;
import nuix.Processor;
import nuix.ProcessorCleaningUpCallback;
import nuix.SimpleCase;
import nuix.Utilities;

/***
 * An example demonstrating creation of a new Nuix simple case and then loading some data into that case.
 * 
 * See BasicInitializationExample for more details regarding the basic initialization steps being taken.
 * @author Jason Wells
 *
 */
public class LoadDataIntoCaseExample {
	private static Logger logger = null;

	public static void main(String[] args) throws Exception {
		// Specify a custom location for our log files
		String logDirectory = String.format("%s/%s",System.getProperty("nuix.logDir"),DateTime.now().toString("YYYYMMDD_HHmmss"));

		// Create an instance of engine wrapper, which will do the work of getting the Nuix bits initialized.
		// Engine wrapper will need to know what directory your engine release resides.
		EngineWrapper wrapper = new EngineWrapper(System.getProperty("nuix.engineDir"), logDirectory);

		// Relying on log4j2 initializations in EngineWrapper creation, so we wait until after that to fetch our logger
		logger = LogManager.getLogger(LoadDataIntoCaseExample.class);
		
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
					File caseDirectory = new File("D:\\Cases\\LoadDataExample");
					
					// Specify additional settings to use when creating case
					Map<String,Object> caseSettings = new HashMap<String,Object>();
					caseSettings.put("compound", false);
					caseSettings.put("name","Load Data Example");
					caseSettings.put("description", "Data loaded using the Java Engine API");
					caseSettings.put("investigator", "Investigator Name Here");
					
					SimpleCase nuixCase = null;
					
					try {
						logger.info(String.format("Creating case: %s",caseDirectory.toString()));
						// Note that since createProcessor method is not available to CompoundCase objects, we need to
						// cast the more generic Case object returned by the create method to a SimpleCase object.
						nuixCase = (SimpleCase)utilities.getCaseFactory().create(caseDirectory,caseSettings);
						logger.info("Case created");
						
						// Create a Processor object for this case
						Processor processor = nuixCase.createProcessor();
						
						// ==========================
						// Create Evidence Containers
						// ==========================
						
						// Evidence container name
						String evidenceName01 = "Evidence 01";
						
						// Map of additional evidence container settings
						Map<String,Object> evidenceSettings01 = new HashMap<String,Object>();
						
						// Provide a longer description (optional)
						evidenceSettings01.put("description", "Description for Evidence 01");
						
						// Provide a default time zone to be used when data does not inherently provide one (optional),
						// Nuix will default to machine time zone if not provided.
						// List of supported time zone IDs can be obtained via java.util.TimeZone.getAvailableIDs, see docs here:
						// https://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html#getAvailableIDs--
						evidenceSettings01.put("timeZone", "America/Los_Angeles");
						
						// (OPTIONAL) Provide a default encoding for items which do not inherently provide one,
						// if not provided the machine's default encoding will be used (often CP1252 on Windows machines).
						// List of supported encodings can be obtained via java.nio.charset.Charset.availableCharsets, see docs here:
						// https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html#availableCharsets--
						evidenceSettings01.put("encoding","UTF8");
						
						// (OPTIONAL) Provide a default locale to be used when data does not inherently provide one,
						// List of supported locales can be obtained via java.util.Locale.getAvailableLocales, see docs here:
						// https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html#getAvailableLocales--
						evidenceSettings01.put("locale","en-US");
						
						// With our settings defined, we now create our evidence container
						EvidenceContainer evidenceContainer01 = processor.newEvidenceContainer(evidenceName01, evidenceSettings01);
						
						// Add some files/directories to be processed
						evidenceContainer01.addFile("D:\\Natives\\DirectoryWithData");
						evidenceContainer01.addFile("D:\\Natives\\SpecificFile.zip");
						
						// And now we call save method, this step is important, if you forget this step, when you start processing later
						// it will be as though this evidence container does not exist!
						evidenceContainer01.save();
					
						// Add another evidence container
						String evidenceName02 = "Evidence 02";
						Map<String,Object> evidenceSettings02 = new HashMap<String,Object>();
						evidenceSettings02.put("description", "Description for Evidence 02");
						evidenceSettings02.put("timeZone", "America/Los_Angeles");
						evidenceSettings02.put("encoding","UTF8");
						evidenceSettings02.put("locale","en-US");
						EvidenceContainer evidenceContainer02 = processor.newEvidenceContainer(evidenceName02, evidenceSettings02);
						evidenceContainer02.addFile("D:\\Natives\\AnotherDirectoryWithData");
						evidenceContainer02.save();
						
						// =============================
						// Configure Processing Settings
						// =============================
						
						// We've added some evidence to process, lets configure the processing settings we will use.
						// Note that we could have also configured these settings before adding evidence if we wanted to.
						// See API docs for Processor.setProcessingSettings for a list of settings, what they do and the
						// defaults Nuix will use if you do not explicitly configure them.
						Map<String,Object> processingSettings = new HashMap<String,Object>();
						processingSettings.put("processText",true);
						processingSettings.put("traversalScope","full_traversal");
						processingSettings.put("analysisLanguage","en");
						processingSettings.put("stopWords",false);
						processingSettings.put("stemming",false);
						processingSettings.put("enableExactQueries",false);
						processingSettings.put("extractNamedEntities",false);
						processingSettings.put("extractNamedEntitiesFromText",false);
						processingSettings.put("extractNamedEntitiesFromProperties",false);
						processingSettings.put("extractNamedEntitiesFromTextStripped",false);
						processingSettings.put("extractShingles",true);
						processingSettings.put("processTextSummaries",true);
						processingSettings.put("detectFaces",false);
						processingSettings.put("classifyImagesWithDeepLearning",false);
						processingSettings.put("imageClassificationModelUrl",null);
						processingSettings.put("extractFromSlackSpace",false);
						processingSettings.put("carveFileSystemUnallocatedSpace",false);
						processingSettings.put("carveUnidentifiedData",false);
						processingSettings.put("carvingBlockSize",null);
						processingSettings.put("recoverDeletedFiles",true);
						processingSettings.put("extractEndOfFileSlackSpace",false);
						processingSettings.put("smartProcessRegistry",true);
						processingSettings.put("identifyPhysicalFiles",true);
						processingSettings.put("createThumbnails",true);
						processingSettings.put("skinToneAnalysis",false);
						processingSettings.put("calculateAuditedSize",true);
						processingSettings.put("storeBinary",true);
						processingSettings.put("maxStoredBinarySize",250000000);
						processingSettings.put("maxDigestSize",250000000);
						processingSettings.put("digests",new String[] {"MD5"});
						processingSettings.put("addBccToEmailDigests",false);
						processingSettings.put("addCommunicationDateToEmailDigests",false);
						processingSettings.put("reuseEvidenceStores",true);
						processingSettings.put("processFamilyFields",false);
						processingSettings.put("hideEmbeddedImmaterialData",false);
						processingSettings.put("performOcr",false);
						processingSettings.put("ocrProfileName","Default");
						processingSettings.put("createPrintedImage",false);
						processingSettings.put("imagingProfileName","Processing Default");
						// Instruct the processor to make use of the settings we defined above
						processor.setProcessingSettings(processingSettings);
						
						// =========================
						// Configure Worker Settings
						// =========================
						
						// Now we are going to instruct the processor how many workers to use to load the data.  See API documentation for
						// ParallelProcessingConfigurable.setParallelProcessingSettings for list of settings and what they do.
						Map<String,Object> parallelProcessingSettings = new HashMap<String,Object>();
						int licenseWorkerCount = utilities.getLicence().getWorkers();
						parallelProcessingSettings.put("workerCount",licenseWorkerCount);
						parallelProcessingSettings.put("workerTemp","D:\\WorkerTemp");
						
						processor.setParallelProcessingSettings(parallelProcessingSettings);
						
						// ========================
						// Setup Progress Callbacks
						// ========================
						
						// We will want to have a sense of how things are moving along once processing has begun.  For that we are going to provide
						// a callback to the Processor which it will call as items are processed.  In turn we will collect and report information about
						// the progress that has been made and periodically log that information out.
						
						// Thread safe long integer used to track when we last reported progress
						AtomicLong lastTimeProgressLogged = new AtomicLong(System.currentTimeMillis());
						
						// Thread safe long integer used to record how many items have been processed
						AtomicLong processedItemCount = new AtomicLong(0); 
						
						// Here we provide our basic callback which will keep track of how many items
						// have been processed.  If it has been at least 5 seconds since the last time
						// we logged a progress message, we log a progress message and then reset our variable
						// tracking when we last reported progress.
						processor.whenItemProcessed(new ItemProcessedCallback() {
							@Override
							public void itemProcessed(ProcessedItem item) {
								// As each item is processed, this callback will be invoked, allowing our code
								// to take some actions.  It is important to note that this code can be called
								// from different threads, so we should take steps to be thread safe in here.
								
								long currentItemCount = processedItemCount.addAndGet(1);
								
								long millisSinceLastUpdate = System.currentTimeMillis() - lastTimeProgressLogged.get();
								long secondsSinceLastUpdate = millisSinceLastUpdate / 1000;
								if (secondsSinceLastUpdate > 5) {
									String progressMessage = String.format("Items Processed: %s", currentItemCount);
									logger.info(progressMessage);
									// Reset last progress time
									lastTimeProgressLogged.set(System.currentTimeMillis());
								}
							}
						});
						
						// We're also going to provide a callback for when processing is "cleaning up", effectively when processing
						// it finishing things up.
						processor.whenCleaningUp(new ProcessorCleaningUpCallback() {
							@Override
							public void cleaningUp() {
								// Let report that processing is cleaning up and then report the item
								// count one last time.
								logger.info("Processor is cleaning up....");
								String progressMessage = String.format("Final Item Count: %s", processedItemCount.get());
								logger.info(progressMessage);
							}
						});
						
						// With the various aspects of configuration done, its time to start processing!
						// This is a blocking call and it will not return until processing has completed.
						processor.process();
						
						logger.info("Processing Completed");
						
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
			NuixDiagnostics.saveDiagnosticsToDirectory("C:\\EngineDiagnostics");
		} finally {
			wrapper.close();
		}
	}
}

