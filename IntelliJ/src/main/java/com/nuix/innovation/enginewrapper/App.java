package com.nuix.innovation.enginewrapper;

import nuix.Utilities;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private static boolean logEnvDetails = true;

    public static void main(String[] args) {
        logEnvDetails();

        try (NuixEngine nuixEngine = constructNuixEngine()) {
            Utilities utilities = nuixEngine.getUtilities();

            // This is where the magic happens.  In this scope, Nuix should be licensed.
            // To run a test in the IDE, I recommend invoking from AppTest in the project tests.

            log.info("Nuix Engine Version: {}", nuixEngine.getNuixVersionString());

        } catch (Exception exc) {
            log.error("Uncaught exception, exiting", exc);
        }
    }

    private static void logEnvDetails() {
        if (logEnvDetails) {
            System.out.println("JVM Arguments:");
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            List<String> jvmArgs = bean.getInputArguments();
            for (String arg : jvmArgs) {
                System.out.println(arg);
            }

            System.out.println("Environment Variables:");
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                System.out.println(String.format("%s => %s", entry.getKey(), entry.getValue()));
            }
        }
    }

    /***
     * Tests will generally call this method to construct the instance of NuixEngine they will use to perform
     * their given test.  This allows you to customize it to your environment without having to alter all the tests.
     * @return A NuixEngine instance ready to use
     */
    public static NuixEngine constructNuixEngine(String... additionalRequiredFeatures) throws IOException {
        List<String> features = List.of("CASE_CREATION");
        if (additionalRequiredFeatures != null && additionalRequiredFeatures.length > 0) {
            features.addAll(List.of(additionalRequiredFeatures));
        }

        NuixLicenseResolver cloud = NuixLicenseResolver.fromCloud()
                .withLicenseCredentialsResolvedFromEnvVars()
                .withMinWorkerCount(4)
                .withRequiredFeatures(features);

        NuixLicenseResolver dongle = NuixLicenseResolver.fromDongle()
                .withRequiredFeatures(features);

        NuixLicenseResolver nms = NuixLicenseResolver.fromServer("<NMS HOST OR IP>")
                .withMinWorkerCount(4)
                .withRequiredFeatures(features)
                .withRequiredFeatures(features);

        return NuixEngine.usingFirstAvailableLicense(cloud, nms, dongle)
                .setEngineDistributionDirectoryFromEnvVar()
                .setLogDirectory(new File("C:/NuixEngineLogs/", DateTime.now().toString("YYYY-MM-dd_HH-mm-ss")).getCanonicalFile());
    }
}
