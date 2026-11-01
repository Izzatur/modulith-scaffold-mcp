package com.example.scaffoldmcp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.example.scaffoldmcp.core.DesignPattern;
import com.example.scaffoldmcp.core.GeneratedFile;
import com.example.scaffoldmcp.core.Names;
import com.example.scaffoldmcp.core.ProjectGenerator;
import com.example.scaffoldmcp.core.SafeWriter;
import com.example.scaffoldmcp.core.TargetResolver;
import com.example.scaffoldmcp.core.WriteReport;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tools. This class is a thin layer: it resolves the folder the user chose,
 * asks ProjectGenerator for files and hands them to SafeWriter.
 */
@Component
public class ScaffoldTools {

    private static final String TARGET_HELP =
            "Where to work. Either the word 'current' (the folder this server was started in) "
                    + "or an absolute path. Required: if the user has not said which, ASK THEM first.";

    private final TargetResolver resolver;
    private final ProjectGenerator generator = new ProjectGenerator();
    private final SafeWriter writer = new SafeWriter();

    public ScaffoldTools() {
        this(TargetResolver.fromEnvironment());
    }

    ScaffoldTools(TargetResolver resolver) {
        this.resolver = resolver;
    }

    public record PatternInfo(String name, String summary, String whenToUse, String springNote) {
    }

    @Tool(description = "List the design patterns that addPattern can generate, with when to use each "
            + "and how Spring changes the picture.")
    public List<PatternInfo> listPatterns() {
        List<PatternInfo> out = new ArrayList<>();
        for (DesignPattern pattern : DesignPattern.values()) {
            out.add(new PatternInfo(pattern.folder(), pattern.summary(), pattern.whenToUse(), pattern.springNote()));
        }
        return out;
    }

    @Tool(description = "Create a new modular Spring Boot project (Spring Modulith, Maven, JUnit 5) set up for "
            + "test-driven development. The project is created in a subfolder named after projectName, inside targetDir. "
            + "Existing files are never overwritten unless overwrite is true. "
            + "Tip: call with dryRun=true first and show the user the file list.")
    public ScaffoldResult scaffoldProject(
            @ToolParam(description = "Project name: lowercase letters, digits and hyphens, e.g. my-shop") String projectName,
            @ToolParam(description = "Maven groupId, e.g. com.example") String groupId,
            @ToolParam(description = "Parent folder for the new project. " + TARGET_HELP) String targetDir,
            @ToolParam(required = false, description = "Module names, lowercase letters and digits, e.g. [orders, inventory]. "
                    + "Default: orders and inventory.") List<String> modules,
            @ToolParam(required = false, description = "Java version: 17, 21 or 25. Default 25.") Integer javaVersion,
            @ToolParam(required = false, description = "Preview only: list the files without writing. Default false.") Boolean dryRun,
            @ToolParam(required = false, description = "Overwrite files that already exist. Default false.") Boolean overwrite) {
        try {
            Path parent = resolver.resolve(targetDir);
            List<GeneratedFile> files = generator.scaffold(projectName, groupId, null, javaVersion, modules);
            Path projectDir = parent.resolve(Names.projectName(projectName));
            WriteReport report = writer.write(projectDir, files, flag(overwrite), flag(dryRun));
            return ScaffoldResult.success(report, List.of(
                    "cd " + projectDir,
                    "mvn -B verify",
                    "git init"));
        } catch (IllegalArgumentException | IOException e) {
            return ScaffoldResult.failure(e.getMessage());
        }
    }

    @Tool(description = "Add a new module (a public service class plus its test) to an existing project "
            + "created with scaffoldProject or any Spring Modulith project.")
    public ScaffoldResult addModule(
            @ToolParam(description = "Module name: lowercase letters and digits, e.g. billing") String moduleName,
            @ToolParam(description = "The project root folder (where pom.xml is). " + TARGET_HELP) String targetDir,
            @ToolParam(required = false, description = "Preview only. Default false.") Boolean dryRun,
            @ToolParam(required = false, description = "Overwrite existing files. Default false.") Boolean overwrite) {
        try {
            Path projectDir = resolver.resolve(targetDir);
            String basePackage = ProjectGenerator.detectBasePackage(projectDir);
            List<GeneratedFile> files = generator.addModule(basePackage, moduleName);
            WriteReport report = writer.write(projectDir, files, flag(overwrite), flag(dryRun));
            return ScaffoldResult.success(report, List.of("mvn -B verify"));
        } catch (IllegalArgumentException | IOException e) {
            return ScaffoldResult.failure(e.getMessage());
        }
    }

    @Tool(description = "Add a design pattern example (code plus tests) inside a module's internal package. "
            + "Call listPatterns to see the options.")
    public ScaffoldResult addPattern(
            @ToolParam(description = "Pattern name: strategy, factory, observer or decorator") String pattern,
            @ToolParam(description = "The module to add it to, e.g. orders") String moduleName,
            @ToolParam(description = "The project root folder (where pom.xml is). " + TARGET_HELP) String targetDir,
            @ToolParam(required = false, description = "Preview only. Default false.") Boolean dryRun,
            @ToolParam(required = false, description = "Overwrite existing files. Default false.") Boolean overwrite) {
        try {
            DesignPattern designPattern = DesignPattern.parse(pattern);
            Path projectDir = resolver.resolve(targetDir);
            String basePackage = ProjectGenerator.detectBasePackage(projectDir);
            List<GeneratedFile> files = generator.addPattern(basePackage, moduleName, designPattern);
            WriteReport report = writer.write(projectDir, files, flag(overwrite), flag(dryRun));
            return ScaffoldResult.success(report, List.of("mvn -B verify"));
        } catch (IllegalArgumentException | IOException e) {
            return ScaffoldResult.failure(e.getMessage());
        }
    }

    private static boolean flag(Boolean value) {
        return value != null && value;
    }
}
