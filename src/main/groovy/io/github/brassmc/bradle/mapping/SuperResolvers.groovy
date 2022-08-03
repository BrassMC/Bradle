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

package io.github.brassmc.bradle.mapping

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@CompileStatic
class SuperResolvers {
    private final Map<String, List<String>> directSuperNames = [:]
    void resolve(Path path) throws IOException {
        try (final is = new ZipInputStream(Files.newInputStream(path))) {
            resolve(is)
        }
    }

    void resolve(ZipInputStream zipInputStream) {
        MappingApplier.forEachZipEntry(zipInputStream, (ZipEntry entry, ZipInputStream is) -> {
            if (!entry.getName().endsWith('.class')) return
            new ClassReader(is).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if (superName != null || interfaces != null) {
                        final var list = new ArrayList<String>()
                        if (superName != null) {
                            list.add(superName)
                        }
                        if (interfaces != null) {
                            list.addAll(interfaces)
                        }
                        directSuperNames[name] = list
                    }
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
        })
    }

    void forEachSuper(String name, @ClosureParams(value = FromString, options = 'java.lang.String') Closure closure) {
        final var queue = new ArrayDeque<String>()
        final var queued = new ArrayList<String>()

        queue.addLast(name)
        while (!queue.isEmpty()) {
            final var target = queue.removeFirst()
            closure(target)

            for (superclass in directSuperNames.getOrDefault(target, [])) {
                if (superclass !in queued) {
                    queue.addLast(superclass)
                }
            }
        }
    }
}
