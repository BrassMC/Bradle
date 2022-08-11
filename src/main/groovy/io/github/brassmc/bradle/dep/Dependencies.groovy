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

package io.github.brassmc.bradle.dep

import groovy.transform.CompileStatic
import io.github.brassmc.bradle.Bradle
import io.github.brassmc.bradle.mc.MinecraftExtension
import io.github.brassmc.bradle.util.gson.PistonMeta
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPlugin

@CompileStatic
class Dependencies {

    static void configureDeps(Project project, Configuration clientRuntime, Configuration serverRuntime, MinecraftExtension mc) {
        project.configurations
                .findByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                .extendsFrom(clientRuntime)

        project.afterEvaluate {
            final var version = PistonMeta.Store.getVersion(mc.getMinecraftVersion().get())
            version.resolvePackage().libraries.each {
                if (!it.canContinue()) return
                clientRuntime.dependencies.add project.dependencies.create(it.name)
            }

            version.resolveServerLibraries().each {
                serverRuntime.dependencies.add project.dependencies.create(it)
            }
        }
    }

    static void configureMavens(Project project) {
        project.repositories.maven { MavenArtifactRepository repo ->
            repo.url = Bradle.MOJANG_MAVEN_URL
            repo.name = 'Mojang Maven'
        }
        project.repositories.mavenCentral()
    }

    static List<String> resolveServerLibraries(String serverLibraries) {
        serverLibraries.split('\n').toList().stream()
                .map {it.split('\t') }
                .filter { it.length == 3}
                .map { it[1] }
                .toList()
    }
}
