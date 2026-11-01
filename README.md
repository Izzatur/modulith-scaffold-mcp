# modulith-scaffold-mcp

An MCP server that lets an AI assistant (Claude Code, Claude Desktop, etc.) scaffold a **modular Spring Boot project** with **Spring Modulith**, **test-first (TDD)** setup, and optional **design-pattern examples**.

You ask in plain English. The assistant calls the tools. Files appear in the folder you choose.

## Tools

| Tool | What it does |
|---|---|
| `listPatterns` | Lists the available design patterns (Strategy, Factory, Observer, Decorator) with when-to-use notes |
| `scaffoldProject` | Creates a new Spring Boot + Modulith project with modules, a module-boundary test, CI, `CLAUDE.md` and a `/tdd` command |
| `addModule` | Adds a module (service + test) to an existing generated project |
| `addPattern` | Adds a pattern example (code + tests) inside a module's `internal` package |

### Choosing the folder
Every writing tool requires `targetDir`:
- `current` – the folder the server was started in
- or an absolute path, e.g. `C:\Users\you\Work` or `/home/you/work`

If you don't say, the assistant is told to ask you.

### Safety
- Refuses filesystem roots and your home folder itself
- Optional: set env `SCAFFOLD_ALLOWED_ROOT` to restrict all writes to one folder tree
- No path traversal, no writing through symlinks
- Existing files are skipped unless `overwrite` is true
- `dryRun` shows what would be written without touching disk
- Project, module and package names are validated (no Java keywords)

## Build and run

Requires Java 25 and Maven.

```bash
mvn -B verify
java -jar target/modulith-scaffold-mcp.jar
```

## Use with Claude Code

```bash
claude mcp add modulith-scaffold -- java -jar /absolute/path/to/target/modulith-scaffold-mcp.jar
```

Or keep the included `.mcp.json` (relative jar path, run Claude Code from this folder). Check with `/mcp`.

Then try:

> Scaffold a Spring Modulith project called `my-shop` with modules `orders` and `inventory` in `C:\Users\you\Work`, then add a strategy pattern to `orders`.

## What a generated project contains
- Spring Boot 4.1 + Spring Modulith 2.1, Maven, JUnit, Java 25 by default (17 and 21 also supported)
- One package per module, each with a service and a test
- `ModularityTests` that verifies module boundaries
- Pattern examples under `<module>/internal/<pattern>/` with their own tests
- `CLAUDE.md` with TDD and module rules, `/tdd` slash command, GitHub Actions CI

## Notes
- Versions: Spring Boot 4.1.2 and Spring AI 2.0.2 for the server; generated projects use Spring Boot 4.1.2 and Spring Modulith 2.1.1. Boot 4 needs Spring AI 2.x, so bump those together. Versions live in `pom.xml` and `ProjectTemplates.java`.
- Design-pattern code is original, Spring-style examples inspired by the catalogue at refactoring.guru.

## License
MIT (add a LICENSE file before publishing)
