package com.example.scaffoldmcp;

import java.util.List;

import com.example.scaffoldmcp.core.WriteReport;

/** What a tool call returns to the AI assistant. */
public record ScaffoldResult(
        boolean ok,
        String message,
        String targetDir,
        boolean dryRun,
        List<String> written,
        List<String> skipped,
        List<String> nextSteps) {

    static ScaffoldResult success(WriteReport report, List<String> nextSteps) {
        String message;
        if (report.dryRun()) {
            message = "Dry run: nothing was written. " + report.written().size() + " file(s) would be created, "
                    + report.skipped().size() + " already exist and would be skipped.";
        } else {
            message = report.written().size() + " file(s) written, "
                    + report.skipped().size() + " existing file(s) skipped.";
        }
        return new ScaffoldResult(true, message, report.targetDir(), report.dryRun(),
                report.written(), report.skipped(), nextSteps);
    }

    static ScaffoldResult failure(String message) {
        return new ScaffoldResult(false, message, null, false, List.of(), List.of(), List.of());
    }
}
