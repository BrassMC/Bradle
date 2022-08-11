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

import groovy.transform.CompileStatic
import io.github.brassmc.bradle.mapping.MappingApplier
import net.minecraftforge.srgutils.IMappingFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path

abstract class RemapJarTask extends DefaultTask {
    RemapJarTask() {
        stripSignatures.convention(true)
        reversedMappings.convention(true)
    }

    @InputFile
    abstract RegularFileProperty getInput()

    @Optional
    @InputFile
    abstract RegularFileProperty getMappings()

    @Optional
    @OutputFile
    abstract RegularFileProperty getOutput()

    @Input
    @Optional
    abstract Property<Boolean> getStripSignatures()

    @Input
    @Optional
    abstract Property<Boolean> getReversedMappings()

    @TaskAction
    @CompileStatic
    void run() {
        final File inFile = getInput().get().asFile
        final Path inPath = inFile.toPath()
        final Path out = getOutput().getOrNull()?.asFile?.toPath()
                ?: inPath.getParent().resolve(withoutExtension(inFile) + '_mapped.jar')
        final File mappingsIn = getMappings().getOrNull()?.asFile
                ?: inPath.getParent().resolve(withoutExtension(inFile) + '_mappings.txt').toFile()
        IMappingFile mappingFile = IMappingFile.load(mappingsIn)
        if (reversedMappings.get())
            mappingFile = mappingFile.reverse()
        MappingApplier.apply(mappingFile, inPath, out, stripSignatures.get())
    }

    void from(DownloadMCTask downloadMCTask) {
        getInput().set(downloadMCTask.getOutput().get())
    }

    static String withoutExtension(File file) {
        final name = file.name
        if (name.lastIndexOf('.') >= 0) return name.substring(0, name.lastIndexOf('.'))
        return name
    }
}
