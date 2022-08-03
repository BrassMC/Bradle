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

import io.github.brassmc.bradle.dep.Dependencies
import io.github.brassmc.bradle.mc.MinecraftExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Path

class Bradle implements Plugin<Project> {
    private static final var MC_NAME = 'minecraft'
    private static final var MC_RUNTIME = 'minecraftRuntime'
    public static final var MOJANG_MAVEN_URL = 'https://libraries.minecraft.net'
    public static boolean isOffline = false
    public static Path cachePath

    @Override
    void apply(Project project) {
        project.getPlugins().apply('java')

        isOffline = project.gradle.startParameter.offline
        cachePath = project.gradle.gradleUserHomeDir.toPath().resolve('caches/bradle')
        final var mc = project.getExtensions().create(MC_NAME, MinecraftExtension)
        final var mcConf = project.configurations.create(MC_RUNTIME)

        Dependencies.configure project, mcConf, mc
    }
}
