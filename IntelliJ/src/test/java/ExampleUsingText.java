import com.nuix.enginebaseline.NuixEngine;
import nuix.*;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/***
 * Demonstration of the UsingTest method, which allows for getting at very large item text that would otherwise causes
 * memory issues or issues allocating a single string which is to large (has more chars than char array max length).
 */
public class ExampleUsingText extends CommonTestFunctionality {
    @Test
    public void ExampleUsingText() throws Exception {
        File caseDirectory = new File(testOutputDirectory, "ExampleUsingText_Case");
        File dataDirectory = new File(testOutputDirectory, "ExampleUsingText_Natives");

        List<CommonTestFunctionality.TermCount> termCounts = createSearchableTestData(dataDirectory, 5000);

        NuixEngine nuixEngine = constructNuixEngine();
        nuixEngine.run((utilities -> {
            // Create a new case
            Map<String, Object> caseSettings = Map.of(
                    "compound", false,
                    "name", "ExampleUsingText",
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
                if (System.currentTimeMillis() - lastProgressTime[0] > updateIntervalSeconds * 1000) {
                    lastProgressTime[0] = System.currentTimeMillis();
                    log.info(String.format("%s items processed", currentItemCount));
                }
            });

            log.info("Processing starting...");
            processor.process();
            log.info("Processing completed");

            // Contrived example where we will iterate each line of the item's text and when a given
            // line is blank after trimming whitespace, we add 1 to our blank line count, ultimately
            // returning the number of blank lines we encountered.
            ReaderReadLogic<Integer> textOperation = new ReaderReadLogic<Integer>() {
                @Override
                public Integer withReader(Reader reader) throws IOException {
                    int blankLineCount = 0;
                    BufferedReader buffer = new BufferedReader(reader);
                    String line;
                    while((line = buffer.readLine()) != null) {
                        if(line.trim().isEmpty()) {
                            blankLineCount++;
                        }
                    }
                    return blankLineCount;
                }
            };

            String query = "flag:audited AND content:*";
            log.info(String.format("Searching for: %s", query));
            Set<Item> items = nuixCase.searchUnsorted(query);
            log.info(String.format("%s items responsive", items.size()));

            for(Item item : items) {
                Text itemTextObject = item.getTextObject();
                // Have our text operation do something with the items text.  Since this operation is handed a
                // Reader rather than attempting to construct one solitary string in memory, this operation should
                // behave better when an item has an especially large text value.
                int blankLineCount = itemTextObject.usingText(textOperation);

                // Record the number of blank lines we encountered as custom metadata
                item.getCustomMetadata().putInteger("ContentBlankLines", blankLineCount);

                log.info(String.format("%s has %s blank lines in its content text", item.getGuid(), blankLineCount));
            }

            log.info("Closing case");
            nuixCase.close();
        }));
    }
}
