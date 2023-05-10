import com.esotericsoftware.minlog.Log;
import com.nuix.enginebaseline.NuixEngine;
import com.nuix.enginebaseline.NuixLicenseResolver;
import net.datafaker.Faker;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommonTestFunctionality {
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

    protected static Logger log;

    // Populated with timestamped, project relative, directory for tests to
    // write data to, like created cases.
    protected static File testOutputDirectory;

    // Populated with a project relative path to test data directory.  Intended to
    // place inputs for tests.
    protected static File testDataDirectory;

    // A seeded Random instance so that we get repeatable random results in a few places
    // where randomness is used, for example while generating test source data.
    protected static Random rand = new Random(1234567890);

    // Instance of Faker library to assist with fake data generation for tests.  We provide it
    // seeded Random instance so that when repeatedly running the same test or tests, the results will hopefully
    // be consistent between runs.
    protected static Faker faker = new Faker(rand);

    // When true, the testOutputDirectory used during tests will be deleted
    // upon test completion.  Set this to false if you wish to manually review the output
    // of tests afterwards.
    protected static boolean deleteTestOutputOnCompletion = true;


    @BeforeAll
    public static void setup() throws Exception {
        log = LogManager.getLogger("Tests");

        testOutputDirectory = new File(System.getenv("TEST_OUTPUT_DIRECTORY"));
        testDataDirectory = new File(System.getenv("TEST_DATA_DIRECTORY"));
        log.info("TEST_OUTPUT_DIRECTORY: " + testOutputDirectory.getAbsolutePath());
        log.info("TEST_DATA_DIRECTORY: " + testDataDirectory.getAbsolutePath());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (deleteTestOutputOnCompletion) {
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
        NuixLicenseResolver caseCreationCloud = NuixLicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromEnvVars()
                .withMinWorkerCount(4)
                .withRequiredFeatures("CASE_CREATION");

        NuixLicenseResolver caseCreationDongle = NuixLicenseResolver.fromDongle()
                .withRequiredFeatures("CASE_CREATION");

        return NuixEngine.usingFirstAvailableLicense(caseCreationCloud, caseCreationDongle)
                .setEngineDistributionDirectoryFromEnvVar()
                .setLogDirectory(new File(testOutputDirectory, "Logs"));
    }

    /***
     * Creates a series of random text files at a specified location, returning details about the expected terms
     * and their counts so a test may later ingest and verify the counts.
     * @param outputDirectory Where the text files should be written to
     * @param itemsToGenerate The number of text files desired
     * @return A List of {@link TermCount} objects
     */
    public List<TermCount> createSearchableTestData(File outputDirectory, int itemsToGenerate) {
        log.info(String.format("Generating %s random text files, for use as test data, to directory %s",
                itemsToGenerate, outputDirectory));
        Map<String, TermCount> overallTermCounts = new HashMap<>();

        List<String> termPool = new ArrayList<>();

        // Pre generate pool of 2000 terms
        for (int i = 0; i < 2000; i++) {
            String term = faker.text().text(4, 8);
            termPool.add(term);
        }

        // Will hold terms to be written to each generated text file
        Set<String> textFileTerms = new HashSet<>();

        // Iteratively generate test text files
        for (int i = 0; i < itemsToGenerate; i++) {
            if (i + 1 % 1000 == 0) {
                Log.info(String.format("Generated %s fake text files so far...", i + 1));
            }
            textFileTerms.clear();

            // Randomly determine how many terms to write
            int targetTermCount = faker.random().nextInt(5, 10);

            // Determine output file name
            File textFile = new File(outputDirectory, String.format("%08d.txt", i));

            for (int t = 0; t < targetTermCount; t++) {
                // Grab term from pool
                String term = termPool.get(faker.random().nextInt(0, termPool.size() - 1));

                // We will only write each term once per file for simplicity
                if (textFileTerms.contains(term)) {
                    continue;
                } else {
                    textFileTerms.add(term);
                }

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
}
