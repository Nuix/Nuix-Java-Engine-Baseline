import com.nuix.enginebaseline.NuixLicenseResolver;
import com.nuix.enginebaseline.NuixEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

public class Tests {
    private static Logger log;

    @BeforeAll
    public static void setup() throws Exception {
        log = LogManager.getLogger("Tests");
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = bean.getInputArguments();
        System.out.println("Args");
        for (String arg : jvmArgs) {
            System.out.println(arg);
        }

    }

    @AfterAll
    public static void breakdown() {
    }

    @Test
    public void GetLicenseFromCloud() throws Exception {
        NuixLicenseResolver cloud_4_workers = NuixLicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromEnvVars()
                .withMinWorkerCount(4)
                .withRequiredFeatures("CASE_CREATION");

        NuixLicenseResolver anyDongle = NuixLicenseResolver.fromDongle()
                .withRequiredFeatures("CASE_CREATION");

        NuixEngine.usingFirstAvailableLicense(cloud_4_workers, anyDongle)
                .setEngineDistributionDirectoryFromEnvVar()
                .run((utilities -> {
                    log.info("License was obtained!");
                }));
    }
}
