package dev.egorgrigorciuc.forzammilmod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copies everything under {@code models/} in our jar into BetterModel's data folder
 * (players/ for player models, models/ for the rest) when missing or different.
 * Plain Java I/O so it can be tested without a server.
 */
public final class ModelInstaller {

    private final File jar;
    private final Path betterModelFolder;
    private final Logger log;

    public ModelInstaller(File jar, Path betterModelFolder, Logger log) {
        this.jar = jar;
        this.betterModelFolder = betterModelFolder;
        this.log = log;
    }

    /** Returns the files written (empty if everything was already up to date or on error). */
    public List<ModelInstallPlan.Copy> install() {
        List<ModelInstallPlan.Copy> copies;
        try {
            copies = ModelInstallPlan.plan(readBundled(), this::readExisting);
        } catch (IOException | UncheckedIOException e) {
            log.log(Level.WARNING, "Could not read bundled models from " + jar.getName(), e);
            return List.of();
        }
        List<ModelInstallPlan.Copy> written = new ArrayList<>();
        for (ModelInstallPlan.Copy c : copies) {
            try {
                write(resolve(c.target()), c.data());
                written.add(c);
                log.info("Installed model " + c.target() + " into BetterModel ("
                        + (c.reason() == ModelInstallPlan.Reason.MISSING ? "new" : "updated") + ")");
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not install model " + c.target(), e);
            }
        }
        if (copies.isEmpty()) log.info("BetterModel models are up to date");
        return written;
    }

    private List<ModelInstallPlan.Bundled> readBundled() throws IOException {
        List<ModelInstallPlan.Bundled> out = new ArrayList<>();
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (e.isDirectory() || ModelInstallPlan.targetPath(e.getName()).isEmpty()) continue;
                try (InputStream in = jf.getInputStream(e)) {
                    out.add(new ModelInstallPlan.Bundled(e.getName(), in.readAllBytes()));
                }
            }
        }
        return out;
    }

    private byte[] readExisting(String target) {
        Path p = resolve(target);
        if (!Files.isRegularFile(p)) return null;
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String target) {
        Path p = betterModelFolder.resolve(target).normalize();
        if (!p.startsWith(betterModelFolder.normalize())) {
            throw new IllegalArgumentException("Model path escapes BetterModel folder: " + target);
        }
        return p;
    }

    /** Writes to a temp file next to the target, then moves it in place so BetterModel never reads half a file. */
    private static void write(Path target, byte[] data) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".forzammilmod-", ".tmp");
        try {
            Files.write(tmp, data);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
