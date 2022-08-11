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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import net.minecraftforge.srgutils.IMappingFile
import net.minecraftforge.srgutils.IRenamer

import java.nio.file.Path

@Canonical
@CompileStatic
class MergedMappings implements IMappingFile {
    List<IMappingFile> files

    @Override
    Collection<? extends IPackage> getPackages() {
        return files.stream().map {it.packages}.reduce {a, b -> a + b }.orElse([])
    }

    @Override
    IPackage getPackage(String original) {
        files.stream().map {it.getPackage(original)}.find {it !== null} as IPackage
    }

    @Override
    Collection<? extends IClass> getClasses() {
        return files.stream().map {it.classes}.reduce {a, b -> a + b }.orElse([])
    }

    @Override
    IClass getClass(String original) {
        files.stream().map {it.getClass(original)}.find {it !== null} as IClass
    }

    @Override
    String remapPackage(String pkg) {
        return files.stream().map {it.remapPackage(pkg)}.filter {it != pkg}.findFirst().orElse(pkg)
    }

    @Override
    String remapClass(String desc) {
        return files.stream().map {it.remapClass(desc)}.filter {it != desc}.findFirst().orElse(desc)
    }

    @Override
    String remapDescriptor(String desc) {
        return files.stream().map {it.remapDescriptor(desc)}.filter {it != desc}.findFirst().orElse(desc)
    }

    @Override
    void write(Path path, Format format, boolean reversed) throws IOException {

    }

    @Override
    IMappingFile reverse() {
        return new MergedMappings(files.stream().map {it.reverse()}.toList())
    }

    @Override
    IMappingFile rename(IRenamer renamer) {
        return new MergedMappings(files.stream().map {it.rename(renamer)}.toList())
    }

    @Override
    IMappingFile chain(IMappingFile other) {
        return new MergedMappings(files.stream().map {it.chain(other)}.toList())
    }
}
