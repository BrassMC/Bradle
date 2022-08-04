package io.github.brassmc.bradle.task

import groovy.transform.CompileStatic
import io.github.brassmc.bradle.mapping.MappingApplier
import net.minecraftforge.srgutils.IMappingFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path

abstract class RemapJarTask extends DefaultTask {
    @InputFile
    abstract RegularFileProperty getInput()

    @Optional
    @InputFile
    abstract RegularFileProperty getMappings()

    @Optional
    @OutputFile
    abstract RegularFileProperty getOutput()

    @TaskAction
    @CompileStatic
    void run() {
        final File inFile = getInput().get().asFile
        final Path inPath = inFile.toPath()
        final Path out = getOutput().getOrNull()?.asFile?.toPath()
                ?: inPath.getParent().resolve(withoutExtension(inFile) + '_mapped.jar')
        final File mappingsIn = getMappings().getOrNull()?.asFile
                ?: inPath.getParent().resolve(withoutExtension(inFile) + '_mappings.txt').toFile()
        // We need to reverse the proguard mappings
        MappingApplier.apply(IMappingFile.load(mappingsIn).reverse(), inPath, out, true)
    }

    void from(DownloadMCTask downloadMCTask) {
        getInput().set(downloadMCTask.getOutput().get())
    }

    static String withoutExtension(File file) {
        final name = file.name
        if (name.indexOf('.') >= 0) return name.substring(0, name.indexOf('.'))
        return name
    }
}
