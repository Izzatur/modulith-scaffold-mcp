package com.example.scaffoldmcp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TargetResolverTest {

    @Test
    void currentMeansTheWorkingFolder(@TempDir Path dir) {
        TargetResolver resolver = new TargetResolver(dir, dir.resolve("home"), null);

        assertEquals(dir.toAbsolutePath().normalize(), resolver.resolve("current"));
        assertEquals(dir.toAbsolutePath().normalize(), resolver.resolve(" Current "));
    }

    @Test
    void absolutePathsAreAccepted(@TempDir Path dir) {
        TargetResolver resolver = new TargetResolver(dir, dir.resolve("home"), null);
        Path wanted = dir.resolve("work/projects");

        assertEquals(wanted.toAbsolutePath().normalize(), resolver.resolve(wanted.toString()));
    }

    @Test
    void blankAndRelativePathsAreRejected(@TempDir Path dir) {
        TargetResolver resolver = new TargetResolver(dir, dir.resolve("home"), null);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("  "));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("projects/foo"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("../foo"));
    }

    @Test
    void rootAndHomeFolderAreRefused(@TempDir Path dir) {
        Path home = dir.resolve("home");
        TargetResolver resolver = new TargetResolver(dir, home, null);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(dir.getRoot().toString()));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(home.toString()));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("~"));
        assertEquals(home.resolve("code").toAbsolutePath().normalize(), resolver.resolve("~/code"));
    }

    @Test
    void allowedRootIsEnforcedEvenWithDotDotSegments(@TempDir Path dir) {
        Path allowed = dir.resolve("allowed");
        TargetResolver resolver = new TargetResolver(allowed, dir.resolve("home"), allowed);

        assertEquals(allowed.resolve("a").toAbsolutePath().normalize(), resolver.resolve(allowed.resolve("a").toString()));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(dir.resolve("other").toString()));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(allowed.toString() + "/../other"));
    }
}
