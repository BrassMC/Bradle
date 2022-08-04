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

abstract class DownloadMCTask extends DefaultTask {
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
        final jar = side == 'client' ? meta.downloads.client : meta.downloads.server
        final maps = side == 'client' ? meta.downloads.client_mappings : meta.downloads.server_mappings

        jar.download(outPath)
        maps.download(mapsOut)
    }

    void output(Object output) {
        getOutput().set(getProject().file(output))
    }

    void side(String side) {
        getSide().set side
    }
}
