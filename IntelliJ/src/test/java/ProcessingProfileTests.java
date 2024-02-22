import com.nuix.innovation.enginewrapper.NuixEngine;
import nuix.EvidenceContainer;
import nuix.Processor;
import nuix.SimpleCase;
import nuix.profile.ProcessingProfile;
import nuix.profile.ProcessingProfileStore;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProcessingProfileTests extends CommonTestFunctionality {
    @Test
    public void testProcessingProfileMimeTypeAdherence_CSV() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "ProcessingProfileMimeTypeAdherenceCSV_Case");
        File csvFilesDirectory = TestData.getTestDataCsvFilesDirectory();

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "testProcessingProfileMimeTypeAdherence"
            );
            SimpleCase nuixCase = (SimpleCase) utilities.getCaseFactory().create(caseDirectory, caseSettings);


//            log.info("Queuing data for processing...");
            Processor processor = nuixCase.createProcessor();
            EvidenceContainer evidenceContainer = processor.newEvidenceContainer("CSVTestData");
            evidenceContainer.addFile(csvFilesDirectory);
            evidenceContainer.save();

            processor.setMimeTypeProcessingSettings("text/csv", Map.of(
                    "enabled", true,
                    "processEmbedded", true,
                    "processText", true,
                    "processNamedEntities", true,
                    "processImages", true,
                    "storeBinary", true
            ));

            System.out.println("Processing settings before loading profile:");
            System.out.println(processor.getMimeTypeProcessingSettings("text/csv"));
            processor.setProcessingProfile("Default");

            System.out.println("Processing settings after loading profile:");
            System.out.println(processor.getMimeTypeProcessingSettings("text/csv"));

            ProcessingProfileStore store = utilities.getProcessingProfileStore();
            ProcessingProfile profile = store.getProfile("Default");

            System.out.println("Processing settings of profile:");
            System.out.println(profile.getMimeTypeProcessingSettings("text/csv"));


            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            long csvDescendantCount = nuixCase.count("path-mime-type:text/csv");
            assertEquals(0, csvDescendantCount, String.format("Expected no CSV descendants but got %s", csvDescendantCount));

            log.info("Closing case");
            nuixCase.close();
        }));
    }

    @Test
    public void testProcessingProfileMimeTypeAdherence_WinLogs() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "ProcessingProfileMimeTypeAdherenceWinLogs_Case");
        File winLogsDirectory = new File(testDataDirectory, "WIN_LOGS");

        if (!winLogsDirectory.exists()) {
            System.out.println("WIN_LOGS dir does not exist, skipping test using it: " + winLogsDirectory);
            return;
        }

        List<String> mimeTypes = List.of(
                "application/vnd.ms-windows-event-logx",
                "application/vnd.ms-windows-event-logx-chunk",
                "application/vnd.ms-windows-event-logx-record"
        );

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "testProcessingProfileMimeTypeAdherence"
            );
            SimpleCase nuixCase = (SimpleCase) utilities.getCaseFactory().create(caseDirectory, caseSettings);


            log.info("Queuing data for processing...");
            Processor processor = nuixCase.createProcessor();
            EvidenceContainer evidenceContainer = processor.newEvidenceContainer("WinLogs");
            evidenceContainer.addFile(winLogsDirectory);
            evidenceContainer.save();

            System.out.println("Processing settings before loading profile:");
            mimeTypes.forEach(m -> System.out.println(processor.getMimeTypeProcessingSettings(m)));

            System.out.println("Processing settings after loading profile:");
            processor.setProcessingProfile("Default");
            mimeTypes.forEach(m -> System.out.println(processor.getMimeTypeProcessingSettings(m)));

            System.out.println("Processing settings of profile:");
            ProcessingProfileStore store = utilities.getProcessingProfileStore();
            ProcessingProfile profile = store.getProfile("Default");
            mimeTypes.forEach(m -> System.out.println(profile.getMimeTypeProcessingSettings(m)));

            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            mimeTypes.forEach(m -> {
                try {
                    long csvDescendantCount = nuixCase.count("path-mime-type:" + m);
                    assertEquals(0, csvDescendantCount, String.format("Expected no descendants of %s but got %s",
                            m, csvDescendantCount));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });


            log.info("Closing case");
            nuixCase.close();
        }));
    }
}
