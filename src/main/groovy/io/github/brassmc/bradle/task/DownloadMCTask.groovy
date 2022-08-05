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

package io.github.brassmc.bradle.task


import io.github.brassmc.bradle.mc.MinecraftExtension
import io.github.brassmc.bradle.util.gson.MetaPackage
import io.github.brassmc.bradle.util.gson.PistonMeta
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry

@SuppressWarnings('unused')
abstract class DownloadMCTask extends DefaultTask {
    DownloadMCTask() {
        extractServer.convention(true)
    }

    @Input
    @Optional
    abstract Property<String> getMcVersion()

    @Input
    abstract Property<String> getSide()

    @OutputFile
    abstract RegularFileProperty getOutput()

    @Optional
    @OutputFile
    abstract RegularFileProperty getMappingsOutput()

    @Input
    @Optional
    abstract Property<Boolean> getExtractServer()

    @TaskAction
    void run() {
        final out = getOutput().get().asFile
        final outPath = out.toPath()
        final mapsOut = getMappingsOutput().getOrNull()?.asFile?.toPath()
                ?: outPath.getParent().resolve(RemapJarTask.withoutExtension(out) + '_mappings.txt')
        final mcVersion = getMcVersion().getOrNull() ?: MinecraftExtension.get(getProject()).minecraftVersion.get()
        final side = getSide().get().toLowerCase(Locale.ROOT)
        if (side !in ['client', 'server']) throw new IllegalArgumentException("Unknown side: $side")
        final meta = PistonMeta.Store.getVersion(mcVersion).resolvePackage()
        this."download${side.capitalize()}"(meta, outPath, mapsOut)
    }

    void downloadClient(MetaPackage meta, Path out, Path mapsOut) {
        meta.downloads.client.download(out)
        meta.downloads.client_mappings.download(mapsOut)
    }

    void downloadServer(MetaPackage meta, Path out, Path mapsOut) {
        if (extractServer.get()) {
            try (final is = URI.create(meta.downloads.server.url).toURL().openStream()
                final jarIs = new JarInputStream(is)) {
                Files.deleteIfExists(out)
                if (out.parent !== null) Files.createDirectories(out.parent)
                ZipEntry ein
                while ((ein = jarIs.nextEntry) != null) {
                    if (ein.name.startsWith('META-INF/versions') && ein.name.contains('server') && ein.name.endsWith('.jar')) {
                        final bytes = jarIs.readAllBytes()
                        Files.write(out, bytes)
                        break
                    }
                }
            }
        } else {
            meta.downloads.server.download(out)
        }
        meta.downloads.server_mappings.download(mapsOut)
    }

    void output(Object output) {
        getOutput().set(getProject().file(output))
    }

    void side(String side) {
        getSide().set side
    }
}
