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

package io.github.brassmc.bradle.util.gson

import com.google.gson.Gson
import groovy.transform.CompileStatic
import io.github.brassmc.bradle.Bradle

import javax.annotation.Nullable
import java.nio.file.Files

@CompileStatic
class PistonMeta {

    Latest latest
    List<Version> versions

    @CompileStatic
    static class Latest {
        String release
        String snapshot
    }

    @CompileStatic
    static class Version {
        String id
        String type
        String url
        String sha1

        MetaPackage resolvePackage() {
            final var cachedPath = Bradle.cachePath.resolve("mojangdata/packages/$id-${sha1}.json")
            if (Bradle.isOffline) {
                if (!Files.exists(cachedPath)) throw new RuntimeException("No piston meta package is cached at $cachedPath! Please disable offline mode")
            } else {
                try (final var is = URI.create(url).toURL().openStream()) {
                    if (!Files.exists(cachedPath) || !Arrays.equals(is.readAllBytes(), Files.newInputStream(cachedPath).readAllBytes())) {
                        Files.deleteIfExists(cachedPath)
                        Files.createDirectories(cachedPath.getParent())
                        Files.write(cachedPath, is.readAllBytes())
                    }
                }
            }
            try (final var is = Files.newBufferedReader(cachedPath)) {
                return new Gson().fromJson(is, MetaPackage)
            }
        }
    }

    @CompileStatic
    static class Store {
        private static final URL META_URL = URI.create('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json').toURL()
        static final PistonMeta DATA = {
            final var cachedPath = Bradle.cachePath.resolve('mojangdata/piston-meta.json')
            if (Bradle.isOffline) {
                if (!Files.exists(cachedPath)) throw new RuntimeException("No piston meta is cached at $cachedPath! Please disable offline mode")
            } else {
                try (final var is = META_URL.openStream()) {
                    if (!Files.exists(cachedPath) || !Arrays.equals(is.readAllBytes(), Files.newInputStream(cachedPath).readAllBytes())) {
                        Files.deleteIfExists(cachedPath)
                        Files.createDirectories(cachedPath.getParent())
                        Files.write(cachedPath, is.readAllBytes())
                    }
                }
            }
            try (final var is = Files.newBufferedReader(cachedPath)) {
                return new Gson().fromJson(is, PistonMeta)
            }
        }.call()

        @Nullable
        static Version getVersion(String id) {
            DATA.versions.stream().filter { it.id == id }
                    .findFirst().orElse(null)
        }

        static String latest() {
            DATA.latest.release
        }
    }
}
