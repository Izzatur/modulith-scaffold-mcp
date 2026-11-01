package com.example.scaffoldmcp.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes generated files under one target folder and nothing else.
 * Existing files are skipped unless overwrite is true. Dry-run touches nothing.
 */
public final class SafeWriter {

    public WriteReport write(Path targetDir, List<GeneratedFile> files, boolean overwrite, boolean dryRun)
            throws IOException {
        Path root = targetDir.toAbsolutePath().normalize();

        // 1. Validate every path before writing anything.
        List<Path> destinations = new ArrayList<>();
        for (GeneratedFile file : files) {
            Path relative = Path.of(file.path());
            if (relative.isAbsolute()) {
                throw new IllegalArgumentException("Generated path must be relative: " + file.path());
            }
            Path destination = root.resolve(relative).normalize();
            if (!destination.startsWith(root)) {
                throw new IllegalArgumentException(
                        "Refusing to write outside the target folder: " + file.path());
            }
            if (Files.isDirectory(destination)) {
                throw new IllegalArgumentException("A folder already exists where a file is needed: " + file.path());
            }
            if (Files.isSymbolicLink(destination)) {
                throw new IllegalArgumentException("Refusing to write through a symbolic link: " + file.path());
            }
            destinations.add(destination);
        }

        if (!dryRun) {
            Files.createDirectories(root);
        }
        Path realRoot = Files.exists(root) ? root.toRealPath() : null;

        // 2. Write (or plan to write).
        List<String> written = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            Path destination = destinations.get(i);
            String shown = root.relativize(destination).toString().replace('\\', '/');

            if (realRoot != null) {
                Path ancestor = destination.getParent();
                while (ancestor != null && !Files.exists(ancestor)) {
                    ancestor = ancestor.getParent();
                }
                if (ancestor == null || !ancestor.toRealPath().startsWith(realRoot)) {
                    throw new IllegalArgumentException("Refusing to write outside the target folder: " + shown);
                }
            }

            if (Files.exists(destination) && !overwrite) {
                skipped.add(shown);
                continue;
            }
            if (!dryRun) {
                Files.createDirectories(destination.getParent());
                Files.writeString(destination, files.get(i).content(), StandardCharsets.UTF_8);
            }
            written.add(shown);
        }
        return new WriteReport(root.toString(), dryRun, List.copyOf(written), List.copyOf(skipped));
    }
}
