import com.nuix.enginebaseline.NuixEngine;
import nuix.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicTests extends CommonTestFunctionality {
    @Test
    public void GetLicenseAutomaticCleanup() throws Exception {
        AtomicBoolean licenseWasObtained = new AtomicBoolean(false);
        NuixEngine nuixEngine = constructNuixEngine();
        // run method will call close before returning
        nuixEngine.run((utilities -> {
            licenseWasObtained.set(true);
        }));
        assertTrue(licenseWasObtained.get());
    }

    @Test
    public void GetLicenseTryWithResourcesCleanup() throws Exception {
        // Create engine instance using try-with-resources, get utilities, use, closes
        // at end of try-with-resources
        try (NuixEngine nuixEngine = constructNuixEngine()) {
            Utilities utilities = nuixEngine.getUtilities();
            utilities.getItemTypeUtility().getAllTypes();
        }
    }

    @Test
    public void GetLicenseManualCleanup() throws Exception {
        // Create engine instance, get utilities, use, then manually close
        NuixEngine nuixEngine = constructNuixEngine();
        Utilities utilities = nuixEngine.getUtilities();
        utilities.getItemTypeUtility().getAllTypes();
        nuixEngine.close();
    }

    @Test
    public void CreateAndOpenSimpleCase() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "CreateAndOpenSimpleCase");
        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "CreateAndOpenSimpleCase",
                    "description", "A Nuix case created using the Nuix Java Engine API",
                    "investigator", "Test"
            );
            Case nuixCase = utilities.getCaseFactory().create(caseDirectory, caseSettings);
            nuixCase.count("");
            nuixCase.close();

            // Open an existing case (the one we just created)
            Case existingNuixCase = utilities.getCaseFactory().open(caseDirectory);
            existingNuixCase.count("");
            existingNuixCase.close();
        }));
    }

    @Test
    public void LoadDataIntoSimpleCase() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "LoadDataIntoSimpleCase_Case");
        File textFilesDirectory = TestData.getTestDataTextFilesDirectory();
        Map<String, Long> termCounts = TestData.getTestDataTextFileTermCounts();

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "LoadDataIntoSimpleCase",
                    "description", "A Nuix case created using the Nuix Java Engine API",
                    "investigator", "Test"
            );
            SimpleCase nuixCase = (SimpleCase) utilities.getCaseFactory().create(caseDirectory, caseSettings);

            log.info("Queuing data for processing...");
            Processor processor = nuixCase.createProcessor();
            EvidenceContainer evidenceContainer = processor.newEvidenceContainer("SearchTestData");
            evidenceContainer.addFile(textFilesDirectory);
            evidenceContainer.save();
            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            log.info("Validating search counts...");
            for (Map.Entry<String, Long> termCount : termCounts.entrySet()) {
                String term = termCount.getKey();
                Long count = termCount.getValue() + 1; // Add 1 for hit on term counts JSON
                long hitCount = nuixCase.count(term);
                assertEquals(count, hitCount, String.format("For term %s, expect %s but got %s",
                        term, count, hitCount));
            }

            log.info("Closing case");
            nuixCase.close();
        }));
    }

    @Test
    public void SearchAndTag() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "SearchAndTag_Case");
        File textFilesDirectory = TestData.getTestDataTextFilesDirectory();
        Map<String, Long> termCounts = TestData.getTestDataTextFileTermCounts();

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "SearchAndTag",
                    "description", "A Nuix case created using the Nuix Java Engine API",
                    "investigator", "Test"
            );
            SimpleCase nuixCase = (SimpleCase) utilities.getCaseFactory().create(caseDirectory, caseSettings);

            log.info("Queuing data for processing...");
            Processor processor = nuixCase.createProcessor();
            EvidenceContainer evidenceContainer = processor.newEvidenceContainer("SearchTestData");
            evidenceContainer.addFile(textFilesDirectory);
            evidenceContainer.save();

            // Periodically log progress
            final long[] lastProgressTime = {0};
            int updateIntervalSeconds = 10;
            AtomicLong itemCount = new AtomicLong(0);
            processor.whenItemProcessed(info -> {
                long currentItemCount = itemCount.addAndGet(1);
                if (System.currentTimeMillis() - lastProgressTime[0] > updateIntervalSeconds * 1000) {
                    lastProgressTime[0] = System.currentTimeMillis();
                    log.info(String.format("%s items processed", currentItemCount));
                }
            });

            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            log.info("Applying Tags...");
            for (Map.Entry<String, Long> termCount : termCounts.entrySet()) {
                String term = termCount.getKey();
                String tag = "Terms|" + term;
                Set<Item> responsiveItems = nuixCase.searchUnsorted(term);
                log.info(String.format("Tagging %s items with tag '%s'",
                        responsiveItems.size(), tag));
                utilities.getBulkAnnotater().addTag(tag, responsiveItems);
            }

            log.info("Validating tag counts...");
            for (Map.Entry<String, Long> termCount : termCounts.entrySet()) {
                String term = termCount.getKey();
                Long count = termCount.getValue() + 1; // Add 1 for hit on term counts JSON
                String tag = "Terms|" + term;
                String query = "tag:\"" + tag + "\"";
                long hitCount = nuixCase.count(query);
                assertEquals(count, hitCount, String.format("For term %s, expect %s tagged items, but got %s",
                        term, count, hitCount));
            }

            log.info("Closing case");
            nuixCase.close();
        }));
    }

    @Test
    public void CreateProductionSet() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "CreateProductionSet_Case");
        File textFilesDirectory = TestData.getTestDataTextFilesDirectory();
        Map<String, Long> termCounts = TestData.getTestDataTextFileTermCounts();

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "CreateProductionSet",
                    "description", "A Nuix case created using the Nuix Java Engine API",
                    "investigator", "Test"
            );
            SimpleCase nuixCase = (SimpleCase) utilities.getCaseFactory().create(caseDirectory, caseSettings);

            log.info("Queuing data for processing...");
            Processor processor = nuixCase.createProcessor();
            EvidenceContainer evidenceContainer = processor.newEvidenceContainer("SearchTestData");
            evidenceContainer.addFile(textFilesDirectory);
            evidenceContainer.save();

            // Periodically log progress
            final long[] lastProgressTime = {0};
            int updateIntervalSeconds = 10;
            AtomicLong itemCount = new AtomicLong(0);
            processor.whenItemProcessed(info -> {
                long currentItemCount = itemCount.addAndGet(1);
                if (System.currentTimeMillis() - lastProgressTime[0] > updateIntervalSeconds * 1000) {
                    lastProgressTime[0] = System.currentTimeMillis();
                    log.info(String.format("%s items processed", currentItemCount));
                }
            });

            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            // Create and configure our production set
            ProductionSet productionSet = nuixCase.newProductionSet("TestProductionSet_" + System.currentTimeMillis());
            Map<String, Object> numberingOptions = Map.of(
                    "prefix", "ABC-",
                    "documentId", Map.of(
                            "minWidth", 9,
                            "startAt", 1
                    )
            );
            productionSet.setNumberingOptions(numberingOptions);

            // Obtain items that we will be adding to the production set
            List<Item> items = nuixCase.search("flag:audited");

            // Add these items to the production set
            productionSet.addItems(items);

            // Let's report the first and last number in this production set
            List<ProductionSetItem> productionSetItems = productionSet.getProductionSetItems();
            String firstNumber = productionSetItems.get(0).getDocumentNumber().toString();
            String lastNumber = productionSetItems.get(productionSetItems.size() - 1).getDocumentNumber().toString();
            log.info(String.format("Production Set Created with Numbers %s to %s", firstNumber, lastNumber));

            log.info("Closing case");
            nuixCase.close();
        }));
    }

    @Test
    public void Export() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "ExportTest_Case");
        File textFilesDirectory = TestData.getTestDataTextFilesDirectory();
        Map<String, Long> termCounts = TestData.getTestDataTextFileTermCounts();
        File exportDirectory = new File(testOutputDirectory, "ExportTest_Export");

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "ExportTest",
                    "description", "A Nuix case created using the Nuix Java Engine API",
                    "investigator", "Test"
            );
            SimpleCase nuixCase = (SimpleCase) utilities.getCaseFactory().create(caseDirectory, caseSettings);

            log.info("Queuing data for processing...");
            Processor processor = nuixCase.createProcessor();
            EvidenceContainer evidenceContainer = processor.newEvidenceContainer("SearchTestData");
            evidenceContainer.addFile(textFilesDirectory);
            evidenceContainer.save();

            // Periodically log progress
            final long[] lastProgressTime = {0};
            int updateIntervalSeconds = 10;
            AtomicLong itemCount = new AtomicLong(0);
            processor.whenItemProcessed(info -> {
                long currentItemCount = itemCount.addAndGet(1);
                if (System.currentTimeMillis() - lastProgressTime[0] > updateIntervalSeconds * 1000) {
                    lastProgressTime[0] = System.currentTimeMillis();
                    log.info(String.format("%s items processed", currentItemCount));
                }
            });

            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

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

            // Add native product to our exporter
            exporter.addProduct("native", Map.of(
                    "naming", productNaming,
                    "path", "NATIVES",
                    "mailFormat", "eml",
                    "includeAttachments", true
            ));

            // Add native product to our exporter
            exporter.addProduct("text", Map.of(
                    "naming", productNaming,
                    "path", "TEXT"
            ));

            // Add native product to our exporter
            exporter.addProduct("pdf", Map.of(
                    "naming", productNaming,
                    "path", "PDF",
                    "regenerateStored", false
            ));

            // Add native product to our exporter
            exporter.addProduct("tiff", Map.of(
                    "naming", productNaming,
                    "path", "TIFF",
                    "regenerateStored", false,
                    "multiPageTiff", false,
                    "tiffDpi", 300,
                    "tiffFormat", "MONOCHROME_CCITT_T6_G4"
            ));

            // Add Concordance DAT load file
            exporter.addLoadFile("concordance", Map.of(
                    "metadataProfile", "Default",
                    "encoding", "UTF-8"
            ));

            // Now we are going to instruct the processor how many workers to use to load the data.  See API documentation for
            // ParallelProcessingConfigurable.setParallelProcessingSettings for list of settings and what they do.
            exporter.setParallelProcessingSettings(Map.of(
                    "workerCount", utilities.getLicence().getWorkers(),
                    "workerTemp", new File(testOutputDirectory, "WorkerTemp").getAbsolutePath()
            ));

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
                    if (currentTimeMillis - lastProgressMillis > 5 * 1000) {
                        String progressMessage = String.format(
                                "Stage: %s, Progress: %s, Errors: %s",
                                info.getStage(), info.getStageCount(), errorCount.get());
                        log.info(progressMessage);
                        lastProgressMillis = System.currentTimeMillis();
                    }

                    // If this particular item had an error we always record this
                    Exception possibleItemException = info.getFailure();
                    if (possibleItemException != null) {
                        Item item = info.getItem();
                        errorCount.incrementAndGet();
                        String errorMessage = String.format(
                                "Error while exporting item %s/%s: %s",
                                item.getGuid(), item.getLocalisedName(), possibleItemException.getMessage());
                        log.error(errorMessage);
                    }
                }
            });

            String itemsToExportQuery = "flag:audited";

            log.info(String.format("Searching: %s", itemsToExportQuery));
            List<Item> itemsToExport = nuixCase.search(itemsToExportQuery);
            log.info(String.format("Responsive Items: %s", itemsToExport.size()));

            log.info("Beginning export...");
            exporter.exportItems(itemsToExport);
            log.info("Export completed");

            log.info(String.format("Errors: %s", errorCount.get()));
            if (errorCount.get() > 0) {
                log.info("Review logs for more details regarding export errors");
            }

            log.info("Closing case");
            nuixCase.close();
        }));
    }
}
