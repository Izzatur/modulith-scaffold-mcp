package com.example.scaffoldmcp.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Builds the list of files for a project, a module or a pattern. It never touches the disk. */
public final class ProjectGenerator {

    static final List<String> DEFAULT_MODULES = List.of("orders", "inventory");
    private static final int MAX_MODULES = 10;
    private static final Pattern PACKAGE_LINE = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;");

    /** Paths are relative to the new project's root folder. */
    public List<GeneratedFile> scaffold(String projectName, String groupId, String basePackage,
                                        Integer javaVersion, List<String> modules) {
        String project = Names.projectName(projectName);
        String group = Names.packageName(groupId);
        String base = (basePackage == null || basePackage.isBlank())
                ? Names.defaultPackage(group, project)
                : Names.packageName(basePackage);
        int java = javaVersion == null ? 25 : javaVersion;
        if (java != 17 && java != 21 && java != 25) {
            throw new IllegalArgumentException("javaVersion must be 17, 21 or 25.");
        }
        List<String> moduleNames = normalizeModules(modules);

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("PROJECT", project);
        tokens.put("GROUP", group);
        tokens.put("BASE", base);
        tokens.put("JAVA", String.valueOf(java));
        tokens.put("APP", Names.className(project) + "Application");
        tokens.put("MODULE_LIST", String.join(", ", moduleNames));
        tokens.put("MODULE_BULLETS", bullets(moduleNames));

        String basePath = base.replace('.', '/');
        List<GeneratedFile> files = new ArrayList<>();
        files.add(file("pom.xml", ProjectTemplates.POM, tokens));
        files.add(file(".gitignore", ProjectTemplates.GITIGNORE, tokens));
        files.add(file("README.md", ProjectTemplates.README, tokens));
        files.add(file("CLAUDE.md", ProjectTemplates.CLAUDE_MD, tokens));
        files.add(file(".claude/commands/tdd.md", ProjectTemplates.TDD_COMMAND, tokens));
        files.add(file(".github/workflows/ci.yml", ProjectTemplates.CI, tokens));
        files.add(file("src/main/resources/application.properties", ProjectTemplates.PROPERTIES, tokens));
        files.add(file("src/main/java/" + basePath + "/" + tokens.get("APP") + ".java",
                ProjectTemplates.APPLICATION, tokens));
        files.add(file("src/test/java/" + basePath + "/ModularityTests.java",
                ProjectTemplates.MODULARITY_TEST, tokens));
        for (String module : moduleNames) {
            files.addAll(moduleFiles(base, module));
        }
        return files;
    }

    public List<GeneratedFile> addModule(String basePackage, String module) {
        return moduleFiles(Names.packageName(basePackage), Names.moduleName(module));
    }

    public List<GeneratedFile> addPattern(String basePackage, String module, DesignPattern pattern) {
        return PatternTemplates.files(pattern, Names.packageName(basePackage), Names.moduleName(module));
    }

    /** Finds the package of the class annotated with @SpringBootApplication under src/main/java. */
    public static String detectBasePackage(Path projectRoot) throws IOException {
        Path sources = projectRoot.resolve("src/main/java");
        if (Files.isDirectory(sources)) {
            try (Stream<Path> walk = Files.walk(sources)) {
                for (Path path : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) {
                    if (Files.size(path) > 1_000_000) {
                        continue;
                    }
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    if (text.contains("@SpringBootApplication")) {
                        Matcher matcher = PACKAGE_LINE.matcher(text);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException(
                "Could not find a @SpringBootApplication class under src/main/java in " + projectRoot
                        + ". Is targetDir the project root?");
    }

    // ------------------------------------------------------------------ helpers

    private List<GeneratedFile> moduleFiles(String base, String module) {
        Map<String, String> tokens = Map.of(
                "BASE", base,
                "MODULE", module,
                "MODULE_CLASS", Names.capitalize(module));
        String path = base.replace('.', '/') + "/" + module + "/" + Names.capitalize(module);
        return List.of(
                file("src/main/java/" + path + "Service.java", ProjectTemplates.MODULE_SERVICE, tokens),
                file("src/test/java/" + path + "ServiceTest.java", ProjectTemplates.MODULE_SERVICE_TEST, tokens));
    }

    private static List<String> normalizeModules(List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return DEFAULT_MODULES;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String module : modules) {
            if (!unique.add(Names.moduleName(module))) {
                throw new IllegalArgumentException("Duplicate module name: " + module);
            }
        }
        if (unique.size() > MAX_MODULES) {
            throw new IllegalArgumentException("At most " + MAX_MODULES + " modules at a time.");
        }
        return List.copyOf(unique);
    }

    private static String bullets(List<String> modules) {
        StringBuilder out = new StringBuilder();
        for (String module : modules) {
            out.append("- `").append(module).append("`\n");
        }
        return out.toString().stripTrailing();
    }

    private static GeneratedFile file(String path, String template, Map<String, String> tokens) {
        return new GeneratedFile(path, render(template, tokens));
    }

    /** Replaces @@KEY@@ tokens. */
    static String render(String template, Map<String, String> tokens) {
        String out = template;
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            out = out.replace("@@" + token.getKey() + "@@", token.getValue());
        }
        return out;
    }
}
