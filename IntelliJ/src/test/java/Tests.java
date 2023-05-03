import com.nuix.enginebaseline.LicenseResolver;
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
        LicenseResolver cloud_4_workers = LicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromENV()
                .withMinWorkerCount(4)
                .withRequiredFeatures("CASE_CREATION");

        LicenseResolver anyDongle = LicenseResolver.fromDongle()
                .withRequiredFeatures("CASE_CREATION");

        NuixEngine.usingFirstAvailableLicense(cloud_4_workers, anyDongle)
                .setEngineDistributionDirectoryFromENV()
                .run((utilities -> {
                    log.info("License was obtained!");
                }));
    }
}
