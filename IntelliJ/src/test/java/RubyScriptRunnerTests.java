import com.nuix.enginebaseline.NuixEngine;
import com.nuix.enginebaseline.RubyScriptRunner;
import nuix.Utilities;
import org.jruby.embed.internal.BiVariableMap;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RubyScriptRunnerTests extends CommonTestFunctionality {
    @Test
    public void TestBasicRubyScript() throws Exception {
        List<String> outputLines = new ArrayList<>();
        try (NuixEngine nuixEngine = constructNuixEngine()) {
            Utilities utilities = nuixEngine.getUtilities();
            Map<String, Object> globalVars = Map.of("$utilities", utilities);
            String script = "$utilities.getItemTypeUtility.getAllKinds.each{|kind| puts kind.getName}";
            RubyScriptRunner rubyScriptRunner = new RubyScriptRunner();
            rubyScriptRunner.setStandardOutputConsumer(outputLines::add);
            rubyScriptRunner.setErrorOutputConsumer(outputLines::add);
            rubyScriptRunner.runScriptAsync(script, nuixEngine.getNuixVersionString(), globalVars);
            rubyScriptRunner.join();
            log.info("Script Output:");
            log.info(String.join("", outputLines));
            assertTrue(outputLines.size() > 0);
        }
    }

    @Test
    public void TestEngineRunScriptAsync() throws Exception {
        List<String> outputLines = new ArrayList<>();
        try (NuixEngine nuixEngine = constructNuixEngine()) {
            String script = "$utilities.getItemTypeUtility.getAllKinds.each{|kind| puts kind.getName}";
            RubyScriptRunner rubyScriptRunner = nuixEngine.runRubyScriptAsync(
                    script, null, outputLines::add, outputLines::add, null);
            rubyScriptRunner.join();
            log.info("Script Output:");
            log.info(String.join("", outputLines));
            assertFalse(outputLines.isEmpty());
        }
    }

    @Test
    public void TestEngineRunScriptFileAsync() throws Exception {
        List<String> outputLines = new ArrayList<>();
        File rubyScriptFile = new File(testDataDirectory, "BasicRubyScript.rb").getCanonicalFile();
        try (NuixEngine nuixEngine = constructNuixEngine()) {
            Map<String, Object> additionalVariables = Map.of(
                    "x", 40,
                    "y", 2
            );
            RubyScriptRunner rubyScriptRunner = nuixEngine.runRubyScriptFileAsync(
                    rubyScriptFile, additionalVariables, outputLines::add, outputLines::add, (result, vars) -> {
                        log.info("Script Output:");
                        log.info(String.join("", outputLines));
                        assertFalse(outputLines.isEmpty());

                        log.info("Implicit Final Result: {}", result);
                        assertEquals(42L, result);

                        // Note that for some reason entrySet will not include globals that were defined during
                        // script execution, but will show globals set before script execution.  They will be
                        // accessible via get still though ¯\_(ツ)_/¯
                        for(Map.Entry<String,Object> entry : vars.entrySet()) {
                            log.info("{} => {}", entry.getKey(), entry.getValue());
                        }

                        Long zValue = (Long) vars.get("z");
                        assertEquals(42L, zValue);
                    });

            rubyScriptRunner.join();
        }
    }
}
