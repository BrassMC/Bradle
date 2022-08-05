package io.github.brassmc.bradle.util

import com.google.gson.Gson
import groovy.transform.CompileStatic
import io.github.brassmc.bradle.Bradle

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

@CompileStatic
class Utils {
    public static final Gson GSON = new Gson()

    static Path updateOrDownload(Path path, String url) throws IOException {
        if (Bradle.isOffline) {
            if (Files.notExists(path))
                throw new RuntimeException("File not cached at $path! Please disable offline mode")
            return path
        }
        try (final is = URI.create(url).toURL().openStream()) {
            final bytes = is.readAllBytes()
            if (Files.notExists(path) || !Arrays.equals(bytes, Files.readAllBytes(path))) {
                if (path.parent !== null) Files.createDirectories(path.parent)
                Files.write(path, bytes)
            }
        }
        return path
    }

    static <T> T loadJson(Path path, Class<T> clazz) throws IOException {
        try (final var reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, clazz)
        }
    }

    static Runnable catchingExceptions(Runnable inRunnable, Consumer<Exception> onException) {
        return () -> {
            try {
                inRunnable.run()
            } catch (Exception exception) {
                onException.accept(exception)
            }
        }
    }
}
