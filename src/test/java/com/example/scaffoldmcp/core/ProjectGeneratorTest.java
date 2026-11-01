package com.example.scaffoldmcp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectGeneratorTest {

    private final ProjectGenerator generator = new ProjectGenerator();

    private static Set<String> paths(List<GeneratedFile> files) {
        return files.stream().map(GeneratedFile::path).collect(Collectors.toSet());
    }

    @Test
    void scaffoldCreatesAModulithProjectWithTheRequestedModules() {
        List<GeneratedFile> files = generator.scaffold("my-shop", "com.example", null, null,
                List.of("orders", "inventory"));
        Set<String> paths = paths(files);

        assertTrue(paths.contains("pom.xml"));
        assertTrue(paths.contains("CLAUDE.md"));
        assertTrue(paths.contains(".claude/commands/tdd.md"));
        assertTrue(paths.contains(".github/workflows/ci.yml"));
        assertTrue(paths.contains("src/main/java/com/example/myshop/MyShopApplication.java"));
        assertTrue(paths.contains("src/test/java/com/example/myshop/ModularityTests.java"));
        assertTrue(paths.contains("src/main/java/com/example/myshop/orders/OrdersService.java"));
        assertTrue(paths.contains("src/test/java/com/example/myshop/inventory/InventoryServiceTest.java"));
    }

    @Test
    void noPlaceholderIsLeftInGeneratedContent() {
        List<GeneratedFile> all = new ArrayList<>(generator.scaffold("my-shop", "com.example", null, 17, null));
        for (DesignPattern pattern : DesignPattern.values()) {
            all.addAll(generator.addPattern("com.example.myshop", "orders", pattern));
        }
        all.addAll(generator.addModule("com.example.myshop", "billing"));

        List<String> offenders = new ArrayList<>();
        for (GeneratedFile file : all) {
            if (file.content().contains("@@")) {
                offenders.add(file.path());
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    void javaVersionAndModulesAppearInTheGeneratedFiles() {
        List<GeneratedFile> files = generator.scaffold("my-shop", "com.example", null, 17, List.of("orders"));
        String pom = files.stream().filter(f -> f.path().equals("pom.xml")).findFirst().orElseThrow().content();
        String claude = files.stream().filter(f -> f.path().equals("CLAUDE.md")).findFirst().orElseThrow().content();

        assertTrue(pom.contains("<java.version>17</java.version>"));
        assertTrue(pom.contains("<artifactId>my-shop</artifactId>"));
        assertTrue(claude.contains("orders"));
    }

    @Test
    void javaDefaultsTo25AndOnlyLtsVersionsAreAccepted() {
        String pom = generator.scaffold("my-shop", "com.example", null, null, null).stream()
                .filter(f -> f.path().equals("pom.xml")).findFirst().orElseThrow().content();
        assertTrue(pom.contains("<java.version>25</java.version>"));
        assertTrue(pom.contains("<version>4.1.2</version>"));
        assertTrue(pom.contains("spring-boot-starter-webmvc"));
        assertFalse(pom.contains("spring-boot-starter-web<"));
        for (int ok : new int[] {17, 21, 25}) {
            generator.scaffold("my-shop", "com.example", null, ok, null);
        }
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "com.example", null, 22, null));
    }

    @Test
    void defaultModulesAreUsedWhenNoneAreGiven() {
        Set<String> paths = paths(generator.scaffold("my-shop", "com.example", null, null, null));

        assertTrue(paths.contains("src/main/java/com/example/myshop/orders/OrdersService.java"));
        assertTrue(paths.contains("src/main/java/com/example/myshop/inventory/InventoryService.java"));
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("My Shop", "com.example", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("../evil", "com.example", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "Com.Example", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "com.class", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "com.example", null, 8, null));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "com.example", null, null, List.of("Orders")));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "com.example", null, null, List.of("class")));
        assertThrows(IllegalArgumentException.class,
                () -> generator.scaffold("my-shop", "com.example", null, null, List.of("orders", "orders")));
        assertThrows(IllegalArgumentException.class,
                () -> generator.addModule("com.example.myshop", "../etc"));
    }

    @Test
    void addModuleCreatesAServiceAndItsTest() {
        Set<String> paths = paths(generator.addModule("com.example.myshop", "billing"));

        assertEquals(Set.of(
                "src/main/java/com/example/myshop/billing/BillingService.java",
                "src/test/java/com/example/myshop/billing/BillingServiceTest.java"), paths);
    }

    @Test
    void everyPatternGeneratesCodeAndTestsInsideTheModulesInternalPackage() {
        for (DesignPattern pattern : DesignPattern.values()) {
            List<GeneratedFile> files = generator.addPattern("com.example.myshop", "orders", pattern);
            String folder = "com/example/myshop/orders/internal/" + pattern.folder() + "/";

            assertFalse(files.isEmpty());
            boolean hasMain = false;
            boolean hasTest = false;
            for (GeneratedFile file : files) {
                assertTrue(file.path().contains(folder), file.path());
                hasMain |= file.path().startsWith("src/main/java/");
                hasTest |= file.path().startsWith("src/test/java/") && file.path().endsWith("Test.java");
            }
            assertTrue(hasMain);
            assertTrue(hasTest);
        }
    }

    @Test
    void patternNamesAreParsedLeniently() {
        assertEquals(DesignPattern.STRATEGY, DesignPattern.parse("strategy"));
        assertEquals(DesignPattern.FACTORY, DesignPattern.parse("Factory Method"));
        assertEquals(DesignPattern.OBSERVER, DesignPattern.parse(" OBSERVER "));
        assertThrows(IllegalArgumentException.class, () -> DesignPattern.parse("singleton"));
        assertThrows(IllegalArgumentException.class, () -> DesignPattern.parse(null));
    }

    @Test
    void detectBasePackageFindsTheSpringBootApplication(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("src/main/java/com/example/myshop/MyShopApplication.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package com.example.myshop;\n\n@SpringBootApplication\npublic class MyShopApplication {}\n");

        assertEquals("com.example.myshop", ProjectGenerator.detectBasePackage(dir));
    }

    @Test
    void detectBasePackageFailsWhenThereIsNoSpringBootApplication(@TempDir Path dir) {
        assertThrows(IllegalArgumentException.class, () -> ProjectGenerator.detectBasePackage(dir));
    }
}
