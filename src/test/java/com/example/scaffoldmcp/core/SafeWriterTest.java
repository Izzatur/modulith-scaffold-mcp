package com.example.scaffoldmcp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafeWriterTest {

    private final SafeWriter writer = new SafeWriter();

    @Test
    void writesFilesAndCreatesFolders(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("project");
        WriteReport report = writer.write(target,
                List.of(new GeneratedFile("a/b/c.txt", "hello"), new GeneratedFile("d.txt", "world")),
                false, false);

        assertEquals("hello", Files.readString(target.resolve("a/b/c.txt")));
        assertEquals("world", Files.readString(target.resolve("d.txt")));
        assertEquals(List.of("a/b/c.txt", "d.txt"), report.written());
        assertEquals(List.of(), report.skipped());
    }

    @Test
    void skipsExistingFilesUnlessOverwriteIsOn(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("keep.txt"), "original");
        List<GeneratedFile> files = List.of(new GeneratedFile("keep.txt", "new"));

        WriteReport first = writer.write(dir, files, false, false);
        assertEquals("original", Files.readString(dir.resolve("keep.txt")));
        assertEquals(List.of("keep.txt"), first.skipped());
        assertEquals(List.of(), first.written());

        WriteReport second = writer.write(dir, files, true, false);
        assertEquals("new", Files.readString(dir.resolve("keep.txt")));
        assertEquals(List.of("keep.txt"), second.written());
    }

    @Test
    void dryRunWritesNothingButReportsWhatWouldHappen(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("project");
        WriteReport report = writer.write(target, List.of(new GeneratedFile("a.txt", "x")), false, true);

        assertFalse(Files.exists(target));
        assertTrue(report.dryRun());
        assertEquals(List.of("a.txt"), report.written());
    }

    @Test
    void refusesPathsThatEscapeTheTargetFolder(@TempDir Path dir) {
        Path target = dir.resolve("project");

        assertThrows(IllegalArgumentException.class,
                () -> writer.write(target, List.of(new GeneratedFile("../evil.txt", "x")), false, false));
        assertThrows(IllegalArgumentException.class,
                () -> writer.write(target, List.of(new GeneratedFile("a/../../evil.txt", "x")), false, false));
        assertFalse(Files.exists(dir.resolve("evil.txt")));
    }

    @Test
    void writesNothingWhenOneFileIsRejected(@TempDir Path dir) {
        Path target = dir.resolve("project");

        assertThrows(IllegalArgumentException.class, () -> writer.write(target,
                List.of(new GeneratedFile("ok.txt", "x"), new GeneratedFile("../evil.txt", "x")), false, false));
        assertFalse(Files.exists(target.resolve("ok.txt")));
    }

    @Test
    void refusesToReplaceAFolderWithAFile(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("thing"));

        assertThrows(IllegalArgumentException.class,
                () -> writer.write(dir, List.of(new GeneratedFile("thing", "x")), true, false));
    }
}
