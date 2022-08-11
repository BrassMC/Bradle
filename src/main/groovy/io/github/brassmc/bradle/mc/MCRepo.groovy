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

package io.github.brassmc.bradle.mc

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.github.brassmc.bradle.Bradle
import io.github.brassmc.bradle.mapping.MappingApplier
import io.github.brassmc.bradle.util.BaseRepo
import io.github.brassmc.bradle.util.Utils
import io.github.brassmc.bradle.util.gson.MetaPackage
import io.github.brassmc.bradle.util.gson.PistonMeta
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier
import org.slf4j.Logger

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@CompileStatic
class MCRepo extends BaseRepo {
    private static final String GROUP = 'net.minecraft'

    MCRepo(Logger log) {
        super(Bradle.cachePath.resolve("mc_repo").toFile(), log)
    }

    @Override
    protected File findFile(ArtifactIdentifier artifact) throws IOException {
        String side = artifact.getName()

        if (artifact.getGroup() != GROUP || side !in ['client', 'server'])
            return null

        if ('jar' == artifact.getExtension()) {
            if (artifact.classifier == 'extra') {
                return resolveExtra(artifact.version, side)
            } else if (artifact.classifier === null) {
                return resolveNormal(artifact.version, side)
            } else if (artifact.classifier == 'joined') {
                return resolveJoined(artifact.version, side)
            }
        }

        return null
    }

    @CompileDynamic
    private File resolveExtra(String version, String side) throws IOException {
        final meta = PistonMeta.Store.getVersion(version).resolvePackage()
        final download = meta.downloads."${side}" as MetaPackage.Download
        final path = cache('extra', "${side}-${version}.jar")
        final oldHash = cache('extra', "${side}-${version}.sha1")
        if (path.exists() && oldHash.exists() && oldHash.getText() == download.sha1) return path
        try (final jarIs = new ZipInputStream(download.open())
            final out = new ZipOutputStream(Utils.prepareAndOpenFOS(path))) {
            final entries = Utils.collectEntries(jarIs,
                    (ZipEntry entry) -> !entry.name.endsWith('.class') && !entry.name.endsWith('.SF'))
            entries.each {
                final entry = MappingApplier.makeNewEntry(it.entry)
                out.putNextEntry(entry)
                out.write(it.data)
                out.closeEntry()
            }
        }
        Utils.prepareForWrite(oldHash)
        oldHash.write(download.sha1)
        return path
    }

    @CompileDynamic
    private File resolveNormal(String version, String side) throws IOException {
        final meta = PistonMeta.Store.getVersion(version).resolvePackage()
        final download = meta.downloads."${side}" as MetaPackage.Download
        final path = cache(side, "${version}.jar")
        final oldHash = cache(side, "${version}.sha1")
        if (path.exists() && oldHash.exists() && oldHash.getText() == download.sha1) return path
        try (final jarIs = new ZipInputStream(download.open())
            final out = new ZipOutputStream(Utils.prepareAndOpenFOS(path))) {
            final entries = Utils.collectEntries(jarIs,
                    (ZipEntry entry) -> entry.name.endsWith('.class') || entry.name.startsWith('META-INF/'))
            entries.each {
                final entry = MappingApplier.makeNewEntry(it.entry)
                out.putNextEntry(entry)
                out.write(it.data)
                out.closeEntry()
            }
        }
        Utils.prepareForWrite(oldHash)
        oldHash.write(download.sha1)
        return path
    }

    @CompileDynamic
    private File resolveJoined(String version, String side) throws IOException {
        final meta = PistonMeta.Store.getVersion(version).resolvePackage()
        final download = meta.downloads."${side}" as MetaPackage.Download
        final path = cache('joined', "${side}-${version}.jar")
        final oldHash = cache('joined', "${side}-${version}.sha1")
        if (path.exists() && oldHash.exists() && oldHash.getText() == download.sha1) return path
        try (final inStream = download.open()
            final outStream = Utils.prepareAndOpenFOS(path)) {
            inStream.transferTo(outStream)
        }
        Utils.prepareForWrite(oldHash)
        oldHash.write(download.sha1)
        return path
    }

}
