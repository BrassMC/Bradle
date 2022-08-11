/*
 * MIT License
 *
 * Copyright (c) 2022 BrassMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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