# modulith-scaffold-mcp

MCP server (stdio, Spring AI) that scaffolds modular Spring Boot projects using Spring Modulith, test-first, with optional design-pattern examples.

## Layout
- `ScaffoldTools` – the MCP tools (`listPatterns`, `scaffoldProject`, `addModule`, `addPattern`). Thin: validate, generate, write, report.
- `core/` – plain Java, no Spring. Generation (`ProjectGenerator`, `ProjectTemplates`, `PatternTemplates`), safety (`TargetResolver`, `SafeWriter`, `Names`).

## Rules
- TDD: write the failing test first, then the code.
- Keep `core/` free of Spring so it stays easy to unit test.
- Never write outside the folder the user chose. All file output goes through `SafeWriter`; all folder choices go through `TargetResolver`.
- Tools return `ScaffoldResult.failure(...)` for user errors; they do not throw.
- stdio transport: never print to stdout. Logging goes to a file (see `application.properties`).
- Generated templates use `@@TOKEN@@` placeholders; a test asserts none are left over.

## Commands
- `mvn -B verify` – build and test
- `java -jar target/modulith-scaffold-mcp.jar` – run the server
