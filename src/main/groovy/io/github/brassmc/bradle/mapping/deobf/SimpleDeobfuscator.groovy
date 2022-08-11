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

package io.github.brassmc.bradle.mapping.deobf

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import io.github.brassmc.bradle.mapping.MappingApplier
import io.github.brassmc.bradle.mc.MinecraftExtension
import io.github.brassmc.bradle.task.RemapJarTask
import io.github.brassmc.bradle.util.Cacher
import io.github.brassmc.bradle.util.HashFunction
import io.github.brassmc.bradle.util.Utils

@Canonical
@CompileStatic
class SimpleDeobfuscator implements Deobfuscator {
    MinecraftExtension mc

    @Override
    File binary(File input, Cacher cacher, MinecraftExtension.Mappings mappings, Side side) {
        final provider = mc.getProvider(mappings.channel)
        if (!provider) throw new RuntimeException("Unknown mappings channel: $mappings.channel")
        final out = cacher.cache(HashFunction.SHA1.hash(input), RemapJarTask.withoutExtension(input) + "_mapped_${mappings.channel}_${mappings.version}_${side}.jar")
        if (out.exists()) return out
        final maps = provider.resolveMappings(cacher.cache('mappings', mappings.channel).toPath(), mappings.version, side)
        Utils.prepareForWrite(out)
        MappingApplier.apply(maps, input.toPath(), out.toPath(), true).toFile()
    }
}
