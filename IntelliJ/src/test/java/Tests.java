import com.nuix.enginebaseline.NuixLicenseResolver;
import com.nuix.enginebaseline.NuixEngine;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests {
    private static Logger log;

    // Populated with timestamped, project relative, directory for tests to
    // write data to, like created cases.
    private static File testOutputDirectory;

    // Populated with a project relative path to test data directory.  Intended to
    // place inputs for tests.
    private static File testDataDirectory;

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

        log.info("JVM Args:");
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = bean.getInputArguments();
        for (String arg : jvmArgs) {
            log.info(arg);
        }
    }

    @AfterAll
    public static void breakdown() {
        if (deleteTestOutputOnCompletion) {
            try {
                log.info("Deleting test output directory: " + testOutputDirectory.getAbsolutePath());
                FileUtils.deleteDirectory(testOutputDirectory);
            } catch (IOException exc) {
                log.error("Error while deleting output direcotyr: " + testOutputDirectory.getAbsolutePath(), exc);
            }
        }
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
                .setEngineDistributionDirectoryFromEnvVar();
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
}
