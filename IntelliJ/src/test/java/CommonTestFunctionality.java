import com.nuix.enginebaseline.NuixEngine;
import com.nuix.enginebaseline.NuixLicenseResolver;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
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

    // When true, the testOutputDirectory used during tests will be deleted
    // upon test completion.  Set this to false if you wish to manually review the output
    // of tests afterwards.
    protected static boolean deleteTestOutputOnCompletion = true;

    @BeforeAll
    public static void setup() throws Exception {
        TestData.init();
        log = LogManager.getLogger("Tests");
        testOutputDirectory = new File(System.getenv("TEST_OUTPUT_DIRECTORY"));
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
        return constructNuixEngine((String[])null);
    }

    /***
     * Tests will generally call this method to construct the instance of NuixEngine they will use to perform
     * their given test.  This allows you to customize it to your environment without having to alter all the tests.
     * @return A NuixEngine instance ready to use
     */
    public NuixEngine constructNuixEngine(String... additionalRequiredFeatures) {
        List<String> features = List.of("CASE_CREATION");
        if(additionalRequiredFeatures != null && additionalRequiredFeatures.length > 0) {
            features.addAll(List.of(additionalRequiredFeatures));
        }

        NuixLicenseResolver caseCreationCloud = NuixLicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromEnvVars()
                .withMinWorkerCount(4)
                .withRequiredFeatures(features);

        NuixLicenseResolver caseCreationDongle = NuixLicenseResolver.fromDongle()
                .withRequiredFeatures("CASE_CREATION");

        return NuixEngine.usingFirstAvailableLicense(caseCreationCloud, caseCreationDongle)
                .setEngineDistributionDirectoryFromEnvVar()
                .setLogDirectory(new File(testOutputDirectory, "Logs_"+System.currentTimeMillis()));
    }
}
