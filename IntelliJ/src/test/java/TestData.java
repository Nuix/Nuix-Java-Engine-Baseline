import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/***
 * A class which tests can query for test data.  This class will create the test data if needed and provided
 * path to existing test data if it has already been created.
 */
public class TestData {
    private static final Logger log = LoggerFactory.getLogger(TestData.class);
    private static File testDataDirectory = new File(System.getenv("TEST_DATA_DIRECTORY"));

    public static void init() {
        // Initialize here, creates more predictable overall output since we ensure that all
        // types are created and created in the same order.
        System.out.println("Ensuring test data has been generated...");
        getTestDataTextFilesDirectory();
        getTestDataJsonFilesDirectory();
    }

    private static File getTestDataCreateAsNeeded(String subDirectoryName, Consumer<File> creationLogic) {
        File subDirectory = new File(testDataDirectory, subDirectoryName);
        if (!subDirectory.exists()) {
            subDirectory.mkdirs();
            log.info("Creating data for: "+subDirectory.getAbsolutePath());
            creationLogic.accept(subDirectory);
        } else {
            log.info("Using existing data from: "+subDirectory.getAbsolutePath());
        }
        return subDirectory;
    }

    public static File getTestDataOtherFilesDirectory() {
        return new File(testDataDirectory, "OTHER");
    }

    public static File getTestDataTextFilesDirectory() {
        return getTestDataCreateAsNeeded("TEXT_FILES", (dir) -> {
            try {
                FakeDataGenerator.generateRandomTextFiles("TEXT", dir, 5000);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Map<String, Long> getTestDataTextFileTermCounts() throws IOException {
        File termCountsFile = new File(getTestDataTextFilesDirectory(), "@termcounts.json");
        Type listType = new TypeToken<Map<String, Long>>() {}.getType();
        return new GsonBuilder().create().fromJson(FileUtils.readFileToString(termCountsFile, StandardCharsets.UTF_8), listType);
    }

    public static File getTestDataJsonFilesDirectory() {
        return getTestDataCreateAsNeeded("JSON_FILES", (dir) -> {
            try {
                FakeDataGenerator.createJsonFiles(dir, 100);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
