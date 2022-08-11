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

package io.github.brassmc.bradle

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import io.github.brassmc.bradle.mc.MinecraftExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.FileCollectionDependency

import java.util.function.Consumer

@Canonical
@CompileStatic
abstract class BradleExtension {
    public static final String NAME = 'bradle'

    Project project
    Configuration obfConfiguration

    Dependency deobf(Object dependency, String side = 'client') {
        return deobf(dependency, null, side)
    }

    Dependency deobf(Object dependency, Closure<?> configure, String side = 'client') {
        Dependency baseDependency = project.dependencies.create(dependency, configure)
        obfConfiguration.getDependencies().add(baseDependency)
        return remap(baseDependency, configure, side)
    }

    @PackageScope
    final List<Consumer<MinecraftExtension.Mappings>> mappingsConfiguredListeners = []
    private Dependency remap(Dependency dependency, Closure configure, String side) {
        if (dependency instanceof ExternalModuleDependency) {
            return remapExternalModule(dependency, configure, side)
        }

        if (dependency instanceof FileCollectionDependency) {
            project.getLogger().warn("files(...) dependencies are not deobfuscated. Use a flatDir repository instead: https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:flat_dir_resolver")
        }

        project.getLogger().warn("Cannot deobfuscate dependency of type {}, using obfuscated version!", dependency.getClass().getSimpleName())
        return dependency
    }

    // TODO we need something less hacky here
    private ExternalModuleDependency remapExternalModule(ExternalModuleDependency dependency, Closure configure, String side) {
        final newDep = project.getDependencies().create('_mapped_' + dependency.getGroup() + ':' + dependency.getName() + ':' + dependency.getVersion(), configure) as ExternalModuleDependency
        mappingsConfiguredListeners.add(new Consumer<MinecraftExtension.Mappings>() {
            @Override
            void accept(MinecraftExtension.Mappings m) {
                newDep.version(v -> v.strictly(newDep.getVersion() + "_mapped_${m.channel}_${m.version}_${side}"))
            }
        })
        return newDep
    }
}
