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

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import io.github.brassmc.bradle.mapping.provider.MappingProvider
import io.github.brassmc.bradle.mapping.provider.OfficialMappingsProvider
import io.github.brassmc.bradle.util.gson.PistonMeta
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.annotation.Nullable

@CompileStatic
@SuppressWarnings('unused')
abstract class MinecraftExtension {
    private final Map<String, MappingProvider> mappingProviders
    MinecraftExtension() {
        minecraftVersion.convention(PistonMeta.Store.latest())
        mappings 'official', PistonMeta.Store.latest()

        this.mappingProviders = new HashMap<>()

        registerMappingProvider('official', new OfficialMappingsProvider())
    }

    abstract Property<String> getMinecraftVersion()
    abstract Property<Mappings> getMappings()

    void minecraftVersion(String version) {
        getMinecraftVersion().set(version)
    }

    void mappings(String channel, String version) {
        mappings.set(new Mappings(channel, version))
    }

    void registerMappingProvider(String name, MappingProvider mappingProvider) {
        mappingProviders.put name, mappingProvider
    }

    @Nullable
    MappingProvider getProvider(String name) {
        return mappingProviders[name]
    }

    static MinecraftExtension get(Project project) {
        return project.extensions.getByType(MinecraftExtension)
    }

    @Canonical
    @CompileStatic
    static class Mappings {
        String channel
        String version

        Mappings(String channel, String version) {
            this.channel = channel
            this.version = version
        }
    }
}
