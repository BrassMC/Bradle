package io.github.brassmc.bradle.util

import javax.annotation.Nullable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

enum HashFunction {
    MD5("md5", 32),
    SHA1("SHA-1", 40),
    SHA256("SHA-256", 64),
    SHA512("SHA-512", 128)

    private final String algo
    private final String pad

    HashFunction(String algo, int length) {
        this.algo = algo
        this.pad = String.format(Locale.ROOT, "%0" + length + "d", 0)
    }

    String getExtension() {
        return this.name().toLowerCase(Locale.ROOT)
    }

    MessageDigest get() {
        try {
            return MessageDigest.getInstance(algo)
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e) // Never happens
        }
    }

    String hash(File file) throws IOException {
        return hash(file.toPath())
    }

    String hash(Path file) throws IOException {
        return hash(Files.readAllBytes(file))
    }

    String hash(Iterable<File> files) throws IOException {
        MessageDigest hash = get()

        for (File file : files) {
            if (!file.exists())
                continue
            hash.update(Files.readAllBytes(file.toPath()))
        }
        return pad(new BigInteger(1, hash.digest()).toString(16))
    }

    String hash(@Nullable String data) {
        return hash(data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8))
    }

    String hash(InputStream stream) throws IOException {
        return hash(stream.readAllBytes())
    }

    String hash(byte[] data) {
        return pad(new BigInteger(1, get().digest(data)).toString(16))
    }

    String pad(String hash) {
        return (pad + hash).substring(hash.length())
    }
}