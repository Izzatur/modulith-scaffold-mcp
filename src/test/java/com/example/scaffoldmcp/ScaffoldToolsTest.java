package com.example.scaffoldmcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.example.scaffoldmcp.core.TargetResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScaffoldToolsTest {

    private static ScaffoldTools toolsWithWorkingFolder(Path dir) {
        return new ScaffoldTools(new TargetResolver(dir, dir.resolve("home"), null));
    }

    @Test
    void scaffoldWritesTheProjectIntoTheChosenFolder(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);
        Path chosen = dir.resolve("work");

        ScaffoldResult result = tools.scaffoldProject("my-shop", "com.example", chosen.toString(),
                List.of("orders"), null, null, null);

        assertTrue(result.ok(), result.message());
        assertTrue(Files.exists(chosen.resolve("my-shop/pom.xml")));
        assertTrue(Files.exists(chosen.resolve("my-shop/src/main/java/com/example/myshop/orders/OrdersService.java")));
    }

    @Test
    void currentMeansTheFolderTheServerWasStartedIn(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);

        ScaffoldResult result = tools.scaffoldProject("my-shop", "com.example", "current",
                List.of("orders"), null, null, null);

        assertTrue(result.ok(), result.message());
        assertTrue(Files.exists(dir.resolve("my-shop/pom.xml")));
    }

    @Test
    void dryRunWritesNothing(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);

        ScaffoldResult result = tools.scaffoldProject("my-shop", "com.example", "current",
                null, null, true, null);

        assertTrue(result.ok());
        assertTrue(result.dryRun());
        assertFalse(result.written().isEmpty());
        assertFalse(Files.exists(dir.resolve("my-shop")));
    }

    @Test
    void aMissingFolderChoiceIsRefusedWithAHelpfulMessage(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);

        ScaffoldResult result = tools.scaffoldProject("my-shop", "com.example", null,
                null, null, null, null);

        assertFalse(result.ok());
        assertTrue(result.message().contains("targetDir is required"));
    }

    @Test
    void addModuleAndAddPatternWorkOnAScaffoldedProject(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);
        tools.scaffoldProject("my-shop", "com.example", "current", List.of("orders"), null, null, null);
        Path project = dir.resolve("my-shop");

        ScaffoldResult module = tools.addModule("billing", project.toString(), null, null);
        ScaffoldResult pattern = tools.addPattern("strategy", "orders", project.toString(), null, null);

        assertTrue(module.ok(), module.message());
        assertTrue(pattern.ok(), pattern.message());
        assertTrue(Files.exists(project.resolve("src/main/java/com/example/myshop/billing/BillingService.java")));
        assertTrue(Files.exists(project.resolve(
                "src/test/java/com/example/myshop/orders/internal/strategy/DiscountServiceTest.java")));
    }

    @Test
    void runningTwiceSkipsExistingFiles(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);
        tools.scaffoldProject("my-shop", "com.example", "current", List.of("orders"), null, null, null);

        ScaffoldResult second = tools.scaffoldProject("my-shop", "com.example", "current",
                List.of("orders"), null, null, null);

        assertTrue(second.ok());
        assertEquals(List.of(), second.written());
        assertFalse(second.skipped().isEmpty());
    }

    @Test
    void problemsComeBackAsFailuresNotExceptions(@TempDir Path dir) {
        ScaffoldTools tools = toolsWithWorkingFolder(dir);

        assertFalse(tools.scaffoldProject("Bad Name", "com.example", "current", null, null, null, null).ok());
        assertFalse(tools.addPattern("singleton", "orders", "current", null, null).ok());
        assertFalse(tools.addModule("billing", "current", null, null).ok()); // no project in the folder
    }

    @Test
    void listPatternsDescribesEveryPattern(@TempDir Path dir) {
        List<ScaffoldTools.PatternInfo> patterns = toolsWithWorkingFolder(dir).listPatterns();

        assertEquals(4, patterns.size());
        assertEquals("strategy", patterns.get(0).name());
    }
}
