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
import net.minecraftforge.srgutils.IMappingFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.function.Predicate
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@CompileStatic
class MappingApplier {
    static void apply(IMappingFile mappings, Path file, boolean stripSignature = true) {
        final resolver = new SuperResolvers()
        resolver.resolve(file)
        apply(mappings, file, resolver, stripSignature)
    }

    static void apply(IMappingFile mappings, Path file, SuperResolvers resolver, boolean stripSignature = true) {
        final remapper = new Remapper() {
            @Override
            String map(String internalName) {
                mappings.remapClass(internalName)
            }

            @Override
            String mapPackageName(String name) {
                mappings.remapPackage(name)
            }

            @Override
            String mapMethodName(String owner, String name, String descriptor) {
                String result = mappings.getClass(owner)?.getMethod(name, descriptor)?.getMapped()
                if (result) return result
                resolver.forEachSuper(owner) {
                    if (result) return
                    result = mappings.getClass(it)?.getMethod(name, descriptor)?.getMapped()
                }
                return result ?: name
            }

            @Override
            String mapFieldName(String owner, String name, String descriptor) {
                String result = mappings.getClass(owner)?.getField(name)?.getMapped()
                if (result) return result
                resolver.forEachSuper(owner) {
                    if (result) return
                    result = mappings.getClass(it)?.getField(name)?.getMapped()
                }
                return result ?: name
            }
        }

        List<ZipEntryProcessor> processors = new ArrayList<>()
        processors.add(new ZipEntryProcessor((ZipEntry ein) -> ein.getName().endsWith(".class"), (ZipEntry ein, ZipInputStream zin, ZipOutputStream zout) ->
                processClass(ein, zin, zout, remapper)))

        if (stripSignature) {
            processors.add(new ZipEntryProcessor((ZipEntry ein) -> ein.getName().endsWith("META-INF/MANIFEST.MF"), MappingApplier::processManifest))
            processors.add(new ZipEntryProcessor(MappingApplier::holdsSignatures, ZipWritingConsumer.NOOP))
        }

        ZipWritingConsumer defaultProcessor = (ZipEntry ein, ZipInputStream zin, ZipOutputStream zout) -> {
            zout.putNextEntry(makeNewEntry(ein))
            copy(zin, zout)
        }


        final outPath = Path.of(file.toAbsolutePath().toString().replace('.jar', '') + '_mapped.jar')
        try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(outPath))
             ZipInputStream zin = new ZipInputStream(Files.newInputStream(file))) {
            process(processors, defaultProcessor, zin, zout)
        }
    }

    private static void process(List<ZipEntryProcessor> processors, ZipWritingConsumer defaultProcessor, ZipInputStream zin, ZipOutputStream zout) throws IOException {
        forEachZipEntry(zin, (ein, zin$) -> {
            for (processor in processors) {
                if (processor.validate(ein)) {
                    processor.getProcessor().process(ein, zin$, zout)
                    return
                }
            }

            defaultProcessor.process(ein, zin, zout)
        })
    }

    static void forEachZipEntry(ZipInputStream zin, ZipConsumer entryConsumer) throws IOException {
        String prevName = null
        ZipEntry ein
        while ((ein = zin.getNextEntry()) != null) {
            try {
                entryConsumer.processEntry(ein, zin)
            } catch (ZipException e) {
                throw new RuntimeException("Unable to process entry '" + ein.getName() + "' due to an error when processing previous entry '" + prevName + "'", e)
            }
            prevName = ein.getName()
        }
    }

    private static void processClass(final ZipEntry ein, final ZipInputStream zin, final ZipOutputStream zout, final Remapper remapper) throws IOException {
        byte[] data = toByteArray(zin)

        ClassReader reader = new ClassReader(data)
        ClassWriter writer = new ClassWriter(0)
        reader.accept(new ClassRemapper(writer, remapper), 0)
        data = writer.toByteArray()

        final var inName = reader.className
        zout.putNextEntry(makeNewEntry(ein, (remapper.mapType(inName) ?: inName) + '.class'))
        zout.write(data)
    }

    static byte[] toByteArray(InputStream input) throws IOException {
        final output = new ByteArrayOutputStream()
        copy(input, output)
        return output.toByteArray()
    }

    static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buf = new byte[0x100]
        // noinspection GroovyUnusedAssignment
        int cnt = 0
        while ((cnt = input.read(buf, 0, buf.length)) != -1) {
            output.write(buf, 0, cnt)
        }
    }

    private static void processManifest(final ZipEntry ein, final ZipInputStream zin, final ZipOutputStream zout) throws IOException {
        Manifest min = new Manifest(zin)
        Manifest mout = new Manifest()
        mout.getMainAttributes().putAll(min.getMainAttributes())
        min.getEntries().forEach((name, ain) -> {
            final var aout = new Attributes()
            ain.forEach((k, v) -> {
                if (!"SHA-256-Digest".equalsIgnoreCase(k.toString())) {
                    aout.put(k, v)
                }
            })
            if (!aout.values().isEmpty()) {
                mout.getEntries().put(name, aout)
            }
        })

        zout.putNextEntry(makeNewEntry(ein))
        mout.write(zout)
    }

    private static boolean holdsSignatures(final ZipEntry ein) {
        return ein.getName().startsWith("META-INF/") && (ein.getName().endsWith(".SF") || ein.getName().endsWith(".RSA"))
    }

    private static ZipEntry makeNewEntry(ZipEntry oldEntry, String newName = null) {
        ZipEntry newEntry = new ZipEntry(newName ?: oldEntry.getName())

        if (oldEntry.getLastModifiedTime() != null) {
            newEntry.setLastModifiedTime(oldEntry.getLastModifiedTime())
        } else {
            newEntry.setLastModifiedTime(FileTime.fromMillis(0x386D4380))
        }

        if (oldEntry.getCreationTime() != null) newEntry.setCreationTime(oldEntry.getCreationTime())
        if (oldEntry.getLastAccessTime() != null) newEntry.setLastAccessTime(oldEntry.getLastAccessTime())
        if (oldEntry.getComment() != null) newEntry.setComment(oldEntry.getComment())

        return newEntry
    }

    @CompileStatic
    private static class ZipEntryProcessor {
        private final Predicate<ZipEntry> validator
        private final ZipWritingConsumer consumer

        ZipEntryProcessor(Predicate<ZipEntry> validator, ZipWritingConsumer consumer) {
            this.validator = validator
            this.consumer = consumer
        }

        boolean validate(ZipEntry ein) {
            return this.validator.test(ein)
        }

        ZipWritingConsumer getProcessor() {
            return this.consumer
        }
    }

    @CompileStatic
    @FunctionalInterface
    private static interface ZipWritingConsumer {
        ZipWritingConsumer NOOP = (ZipEntry ein, ZipInputStream zin, ZipOutputStream zout) -> { }

        void process(ZipEntry ein, ZipInputStream zin, ZipOutputStream zout) throws IOException
    }

    @CompileStatic
    @FunctionalInterface
    private static interface ZipConsumer {
        void processEntry(ZipEntry entry, ZipInputStream stream) throws IOException
    }
}
