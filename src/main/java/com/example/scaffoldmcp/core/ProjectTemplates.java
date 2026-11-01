package com.example.scaffoldmcp.core;

/**
 * File templates for a generated Spring Modulith project.
 * Tokens look like @@NAME@@ and are replaced by ProjectGenerator.render.
 */
final class ProjectTemplates {

    private ProjectTemplates() {
    }

    static final String POM = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>

                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>4.1.2</version>
                    <relativePath/>
                </parent>

                <groupId>@@GROUP@@</groupId>
                <artifactId>@@PROJECT@@</artifactId>
                <version>0.1.0-SNAPSHOT</version>
                <name>@@PROJECT@@</name>

                <properties>
                    <java.version>@@JAVA@@</java.version>
                    <spring-modulith.version>2.1.1</spring-modulith.version>
                </properties>

                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.modulith</groupId>
                            <artifactId>spring-modulith-bom</artifactId>
                            <version>${spring-modulith.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>

                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-webmvc</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.modulith</groupId>
                        <artifactId>spring-modulith-starter-core</artifactId>
                    </dependency>

                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-test</artifactId>
                        <scope>test</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.modulith</groupId>
                        <artifactId>spring-modulith-starter-test</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

    static final String APPLICATION = """
            package @@BASE@@;

            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class @@APP@@ {

                public static void main(String[] args) {
                    SpringApplication.run(@@APP@@.class, args);
                }
            }
            """;

    static final String MODULARITY_TEST = """
            package @@BASE@@;

            import org.junit.jupiter.api.Test;
            import org.springframework.modulith.core.ApplicationModules;

            /** Fails the build if a module reaches into another module's internals or modules depend on each other in a cycle. */
            class ModularityTests {

                @Test
                void verifiesModularStructure() {
                    ApplicationModules.of(@@APP@@.class).verify();
                }
            }
            """;

    static final String MODULE_SERVICE = """
            package @@BASE@@.@@MODULE@@;

            import org.springframework.stereotype.Service;

            /**
             * Public API of the @@MODULE@@ module. Other modules may only use types in this package.
             * Anything in a sub-package (such as internal) is private to the module.
             */
            @Service
            public class @@MODULE_CLASS@@Service {

                public String moduleName() {
                    return "@@MODULE@@";
                }
            }
            """;

    static final String MODULE_SERVICE_TEST = """
            package @@BASE@@.@@MODULE@@;

            import static org.junit.jupiter.api.Assertions.assertEquals;

            import org.junit.jupiter.api.Test;

            class @@MODULE_CLASS@@ServiceTest {

                @Test
                void reportsItsModuleName() {
                    assertEquals("@@MODULE@@", new @@MODULE_CLASS@@Service().moduleName());
                }
            }
            """;

    static final String PROPERTIES = """
            spring.application.name=@@PROJECT@@
            """;

    static final String GITIGNORE = """
            target/
            *.log
            .idea/
            *.iml
            .vscode/
            .DS_Store
            .claude/settings.local.json
            """;

    static final String CI = """
            name: CI

            on:
              push:
                branches: [main]
              pull_request:

            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v4
                  - uses: actions/setup-java@v4
                    with:
                      distribution: temurin
                      java-version: "@@JAVA@@"
                      cache: maven
                  - name: Build and test
                    run: mvn -B verify
            """;

    static final String CLAUDE_MD = """
            # @@PROJECT@@

            A modular monolith built with Spring Boot and **Spring Modulith**, developed test-first (TDD).

            ## Stack
            - Java @@JAVA@@, Maven, Spring Boot 4.1.2, Spring Modulith 2.1.1
            - JUnit 5 for tests

            ## Commands
            - Build and test: `mvn -B verify`
            - Tests only: `mvn -B test`
            - Run the app: `mvn spring-boot:run`

            ## Modules
            Each top-level package under `@@BASE@@` is a module: @@MODULE_LIST@@.

            Rules:
            - A module's public API is the types in its top-level package.
            - Sub-packages such as `internal` are private. Other modules must not use them.
            - Modules talk to each other through public API types or application events, never through internals.
            - `ModularityTests` fails the build if these rules or cycle checks are broken. Never weaken it to make a build pass.

            ## Working style: TDD
            Always work in small red, green, refactor steps:
            1. **Red:** write one failing test for one behaviour. Run `mvn -B test` and confirm it fails for the right reason.
            2. **Green:** write the least code that makes it pass. Run the tests again.
            3. **Refactor:** clean up with the tests green. Run them again.

            Never write production code without a failing test first. Use the `/tdd` command to apply this to a feature.

            ## Design patterns
            Pattern examples live in `<module>/internal/<pattern>/` with their tests. Prefer the Spring way when it exists (dependency injection instead of Singleton, application events instead of hand-written observers).
            """;

    static final String TDD_COMMAND = """
            ---
            description: Implement a feature strictly test-first (red, green, refactor)
            argument-hint: <the behaviour to implement>
            ---

            Implement the following using strict TDD: $ARGUMENTS

            Follow `CLAUDE.md` and work one behaviour at a time:
            1. Write a single failing test. Run `mvn -B test` and show that it fails for the expected reason.
            2. Write the minimum production code to make it pass. Run the tests again.
            3. Refactor with the tests green. Run the tests again.
            4. Repeat for the next behaviour.

            Do not write production code before a failing test exists. Keep module boundaries intact and finish by running `mvn -B verify`.
            """;

    static final String README = """
            # @@PROJECT@@

            A modular monolith using Spring Boot and Spring Modulith, set up for test-driven development.

            ## Modules
            @@MODULE_BULLETS@@

            ## Build and test
            ```bash
            mvn -B verify
            ```

            `ModularityTests` checks that modules only use each other's public API and have no dependency cycles.

            ## Working test-first
            See `CLAUDE.md`. With Claude Code, run `/tdd <behaviour>` to implement a feature in red, green, refactor steps.

            ## Design pattern examples
            Pattern examples (with tests) live under `<module>/internal/<pattern>/`.
            """;
}
