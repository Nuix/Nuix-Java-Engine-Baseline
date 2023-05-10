import com.nuix.enginebaseline.NuixEngine;
import nuix.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicTests extends CommonTestFunctionality {
    @Test
    public void GetLicense() throws Exception {
        AtomicBoolean licenseWasObtained = new AtomicBoolean(false);
        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            licenseWasObtained.set(true);
        }));
        assertTrue(licenseWasObtained.get());
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
        File dataDirectory = new File(testOutputDirectory, "LoadDataIntoSimpleCase_Natives");

        List<TermCount> termCounts = createSearchableTestData(dataDirectory, 1000);

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
            evidenceContainer.addFile(dataDirectory);
            evidenceContainer.save();
            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            log.info("Validating search counts...");
            for (TermCount termCount : termCounts) {
                long hitCount = nuixCase.count(termCount.term);
                assertEquals(termCount.count, hitCount, String.format("For term %s, expect %s but got %s",
                        termCount.term, termCount.count, hitCount));
            }

            log.info("Closing case");
            nuixCase.close();
        }));
    }

    @Test
    public void SearchAndTag() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "SearchAndTag_Case");
        File dataDirectory = new File(testOutputDirectory, "SearchAndTag_Natives");

        List<TermCount> termCounts = createSearchableTestData(dataDirectory, 5000);

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
            evidenceContainer.addFile(dataDirectory);
            evidenceContainer.save();

            // Periodically log progress
            final long[] lastProgressTime = {0};
            int updateIntervalSeconds = 10;
            AtomicLong itemCount = new AtomicLong(0);
            processor.whenItemProcessed(info -> {
                long currentItemCount = itemCount.addAndGet(1);
                if(System.currentTimeMillis() - lastProgressTime[0] > updateIntervalSeconds * 1000) {
                    lastProgressTime[0] = System.currentTimeMillis();
                    log.info(String.format("%s items processed", currentItemCount));
                }
            });

            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            log.info("Applying Tags...");
            for (TermCount termCount : termCounts) {
                String tag = "Terms|"+termCount.term;
                Set<Item> responsiveItems = nuixCase.searchUnsorted(termCount.term);
                log.info(String.format("Tagging %s items with tag '%s'",
                        responsiveItems.size(), tag));
                utilities.getBulkAnnotater().addTag(tag, responsiveItems);
            }

            log.info("Validating tag counts...");
            for (TermCount termCount : termCounts) {
                String tag = "Terms|"+termCount.term;
                String query = "tag:\""+tag+"\"";
                long hitCount = nuixCase.count(query);
                assertEquals(termCount.count, hitCount, String.format("For term %s, expect %s tagged items, but got %s",
                        termCount.term, termCount.count, hitCount));
            }

            log.info("Closing case");
            nuixCase.close();
        }));
    }
}
