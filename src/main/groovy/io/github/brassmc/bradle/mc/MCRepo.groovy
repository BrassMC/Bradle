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

}
