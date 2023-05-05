import com.nuix.enginebaseline.NuixEngine;
import com.nuix.enginebaseline.NuixLicenseResolver;
import net.datafaker.Faker;
import nuix.Case;
import nuix.EvidenceContainer;
import nuix.Processor;
import nuix.SimpleCase;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests {
    // Used in some tests, this is a simple class to hold
    // a term and an associated count.
    class TermCount {
        public String term;
        public long count;

        public TermCount(String term, long count) {
            this.term = term;
            this.count = count;
        }
    }

    private static Logger log;

    // Populated with timestamped, project relative, directory for tests to
    // write data to, like created cases.
    private static File testOutputDirectory;

    // Populated with a project relative path to test data directory.  Intended to
    // place inputs for tests.
    private static File testDataDirectory;

    // A seeded Random instance so that we get repeatable random results in a few places
    // where randomness is used, for example while generating test source data.
    private static Random rand = new Random(1234567890);

    // Instance of Faker library to assist with fake data generation for tests.  We provide it
    // seeded Random instance so that when repeatedly running the same test or tests, the results will hopefully
    // be consistent between runs.
    private static Faker faker = new Faker(rand);

    // When true, the testOutputDirectory used during tests will be deleted
    // upon test completion.  Set this to false if you wish to manually review the output
    // of tests afterwards.
    private static boolean deleteTestOutputOnCompletion = true;

    @BeforeAll
    public static void setup() throws Exception {
        log = LogManager.getLogger("Tests");

        testOutputDirectory = new File(System.getenv("TEST_OUTPUT_DIRECTORY"));
        testDataDirectory = new File(System.getenv("TEST_DATA_DIRECTORY"));
        log.info("TEST_OUTPUT_DIRECTORY: " + testOutputDirectory.getAbsolutePath());
        log.info("TEST_DATA_DIRECTORY: " + testDataDirectory.getAbsolutePath());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(deleteTestOutputOnCompletion) {
                try {
                    FileUtils.deleteDirectory(testOutputDirectory);
                } catch (IOException exc) {
                    log.error("Error while deleting output directory: " + testOutputDirectory.getAbsolutePath(), exc);
                }
            }
        }));

        log.info("JVM Args:");
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = bean.getInputArguments();
        for (String arg : jvmArgs) {
            log.info(arg);
        }
    }

    @AfterAll
    public static void breakdown() {
        NuixEngine.closeGlobalContainer();
    }

    /***
     * Tests will generally call this method to construct the instance of NuixEngine they will use to perform
     * their given test.  This allows you to customize it to your environment without having to alter all the tests.
     * @return A NuixEngine instance ready to use
     */
    public NuixEngine constructNuixEngine() {
        NuixLicenseResolver cloud_4_workers = NuixLicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromEnvVars()
                .withMinWorkerCount(4)
                .withRequiredFeatures("CASE_CREATION");

        NuixLicenseResolver anyDongle = NuixLicenseResolver.fromDongle()
                .withRequiredFeatures("CASE_CREATION");

        return NuixEngine.usingFirstAvailableLicense(cloud_4_workers, anyDongle)
                .setEngineDistributionDirectoryFromEnvVar()
                .setLogDirectory(new File(testOutputDirectory, "Logs"));
    }

    public List<TermCount> createSearchableTestData(File outputDirectory, int itemsToGenerate) {
        Map<String, TermCount> overallTermCounts = new HashMap<>();

        // Will hold terms to be written to each generated text file
        Set<String> textFileTerms = new HashSet<>();

        // Iteratively generate test text files
        for (int i = 0; i < itemsToGenerate; i++) {
            textFileTerms.clear();

            // Randomly determine how many terms to write
            int targetTermCount = faker.random().nextInt(5, 10);

            // Determine output file name
            File textFile = new File(outputDirectory, String.format("%08d.txt", i));

            for (int t = 0; t < targetTermCount; t++) {
                // Generate term
                String term = faker.text().text(4, 8);
                // We will only write each term once for simplicity
                if (textFileTerms.contains(term)) {
                    continue;
                }
                // Add terms to list of terms to be written to file
                textFileTerms.add(term);
                // Track this term in overall counts for later verification
                if (!overallTermCounts.containsKey(term)) {
                    overallTermCounts.put(term, new TermCount(term, 1));
                } else {
                    overallTermCounts.get(term).count++;
                }
            }

            try {
                FileUtils.writeStringToFile(textFile, String.join(" ", textFileTerms), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new ArrayList<>(overallTermCounts.values());
    }

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
}
