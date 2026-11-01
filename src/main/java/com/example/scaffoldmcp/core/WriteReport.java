package com.example.scaffoldmcp.core;

import java.util.List;

/** What a write did (or, in dry-run mode, would do). Paths are relative to targetDir. */
public record WriteReport(String targetDir, boolean dryRun, List<String> written, List<String> skipped) {
}
