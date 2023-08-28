import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.datafaker.Faker;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/***
 * A class for generating random data.
 */
public class FakeDataGenerator {
    private static final Logger log = LoggerFactory.getLogger(FakeDataGenerator.class);
    public static final int seed = 1234567890;
    public static final Faker faker = new Faker(new Random(seed));
    public static final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private static ListOrderedSet<String> termPool = null;
    private static List<Supplier<String>> nameSuppliers = List.of(
            faker.science()::bosons,
            faker.science()::leptons,
            faker.science()::element,
            faker.science()::quark,
            faker.science()::tool,
            faker.computer()::type,
            faker.house()::room,
            faker.tea()::type
    );
    private static List<Supplier<Object>> objectSuppliers = List.of(
            faker.random()::nextBoolean,
            faker.random()::nextInt,
            faker.random()::nextLong,
            faker.random()::nextDouble,
            faker.random()::nextFloat,
            () -> faker.random().hex(faker.random().nextInt(8, 32)),
            () -> faker.text().text(4, 10, true),
            faker.lorem()::sentence,
            faker.chuckNorris()::fact,
            faker.cryptoCoin()::coin,
            faker.internet()::domainName,
            faker.internet()::emailAddress,
            faker.internet()::macAddress
    );

    private static int nextId = 0;

    private static List<KeyValue<String, Object>> propertyProviders = new ArrayList<>();

    static {
        Set<String> alreadyUsedNames = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String generatedName = null;
            do {
                generatedName = StringUtils.capitalize(rand(nameSuppliers).get());
            } while (alreadyUsedNames.contains(generatedName));
            alreadyUsedNames.add(generatedName);

            String finalGeneratedName = generatedName;
            propertyProviders.add(new KeyValue<String, Object>() {
                String name = finalGeneratedName;
                Supplier<Object> valueSupplier = rand(objectSuppliers);

                @Override
                public String getKey() {
                    return name;
                }

                @Override
                public Object getValue() {
                    return valueSupplier.get();
                }
            });

        }

        termPool = new ListOrderedSet<>();
        for (int i = 0; i < 5000; i++) {
            termPool.add(faker.text().text(6, 10));
        }
    }

    public static String getRandomPoolTerm() {
        return termPool.get(faker.random().nextInt(0, termPool.size() - 1));
    }

    public static Set<String> saveRandomTextFile(File textFile) throws IOException {
        Set<String> distinctTerms = new HashSet<>();
        int termCount = faker.random().nextInt(10, 20);
        for (int i = 0; i < termCount; i++) {
            String term = getRandomPoolTerm();
            distinctTerms.add(term);
        }
        FileUtils.writeStringToFile(textFile, String.join(" ", distinctTerms), StandardCharsets.UTF_8);
        return distinctTerms;
    }

    public static Map<String, Long> generateRandomTextFiles(String filenamePrefix, File directory, int count) throws IOException {
        Map<String, Long> termFileCounts = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String fileName = String.format("%s_%08d.txt", filenamePrefix, i);
            Set<String> distinctFileTerms = saveRandomTextFile(new File(directory, fileName));
            for (String distinctFileTerm : distinctFileTerms) {
                if (!termFileCounts.containsKey(distinctFileTerm)) {
                    termFileCounts.put(distinctFileTerm, 0L);
                }
                termFileCounts.put(distinctFileTerm, termFileCounts.get(distinctFileTerm) + 1);
            }
        }

        // Save term counts for later
        FileUtils.writeStringToFile(
                new File(directory, "@termcounts.json"),
                gson.toJson(termFileCounts),
                StandardCharsets.UTF_8
        );

        return termFileCounts;
    }

    public static <V> V rand(List<V> list) {
        return list.get(faker.random().nextInt(list.size()));
    }

    public static void createJsonFiles(File directory, int count) {
        for (int i = 0; i < count; i++) {
            saveJsonItemHierarchy(directory);
        }
    }

    protected static void saveJsonItemHierarchy(File directory) {
        List<Map<String, Object>> itemMaps = createItemHierarchy();
        for (int i = 0; i < itemMaps.size(); i++) {
            try {
                Map<String, Object> itemMap = itemMaps.get(i);
                String fileName = itemMap.get("ID") + ".json";
                File outputFile = new File(directory, fileName);
                FileUtils.writeStringToFile(
                        outputFile, gson.toJson(itemMap), StandardCharsets.UTF_8
                );
            } catch (IOException e) {
                System.out.println("Error generating JSON item: " + e.getMessage());
            }
        }
    }

    protected static List<Map<String, Object>> createItemHierarchy() {
        Queue<Map<String, Object>> itemStack = new ArrayDeque<>();
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> childIds = new ArrayList<>();
        itemStack.add(createItemDataMap(null, 0));
        Map<String, Object> current = null;
        for (int i = 0; i < faker.random().nextInt(1, 100); i++) {
            if (itemStack.size() < 1) {
                break;
            }
            current = itemStack.poll();
            childIds.clear();
            result.add(current);
            for (int c = 0; c < faker.random().nextInt(0, 8); c++) {
                Map<String, Object> child = createItemDataMap(
                        (String) current.get("ID"), ((Integer) current.get("Depth")) + 1);
                itemStack.add(child);
                childIds.add((String) child.get("ID"));
            }
            current.put("ChildIDs", childIds);
        }

        return result;
    }

    protected static Map<String, Object> createItemDataMap(String parentID, int depth) {
        Map<String, Object> result = new HashMap<>();
        if (parentID != null && !parentID.isEmpty()) {
            result.put("ID", String.format("%08d", nextId++));
        } else {
            result.put("ID", String.format("@%08d", nextId++));
        }
        result.put("ParentID", parentID);
        result.put("Name", faker.lorem().sentence(5));
        result.put("Text", String.join("\n", faker.lorem().paragraphs(faker.random().nextInt(2, 10))));
        result.put("Depth", depth);
        result.put("Properties", createItemPropertiesMap());
        return result;
    }

    protected static Map<String, Object> createItemPropertiesMap() {
        Map<String, Object> result = new HashMap<>();
        int propertyCount = faker.random().nextInt(5, 20);
        for (int i = 0; i < propertyCount; i++) {
            KeyValue<String, Object> propertyProvider = rand(propertyProviders);
            result.put(propertyProvider.getKey(), propertyProvider.getValue());
        }
        return result;
    }
}
