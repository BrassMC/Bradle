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

package io.github.brassmc.bradle.mapping.deobf;

import io.github.brassmc.bradle.Bradle;
import io.github.brassmc.bradle.mc.MinecraftExtension;
import io.github.brassmc.bradle.util.Artifact;
import io.github.brassmc.bradle.util.BaseRepo;
import io.github.brassmc.bradle.util.Utils;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class DeobfuscatingRepo extends BaseRepo {
    private final Configuration configuration;
    private ResolvedConfiguration resolvedOrigin;
    private final Deobfuscator deobfuscator;

    public DeobfuscatingRepo(Logger log, Configuration configuration, Deobfuscator deobfuscator) {
        super(Bradle.cachePath.resolve("deobf_repo").toFile(), log);
        this.configuration = configuration;
        this.deobfuscator = deobfuscator;
    }

    @Override
    protected File findFile(ArtifactIdentifier artifact) throws IOException {
        var version = artifact.getVersion();
        final var mappings = getMappings(version);

        if (mappings == null)
            return null; // We only care about the remapped files, not orig

        version = version.split("_mapped_")[0];
        final var art = Artifact.from(artifact).withGroup(artifact.getGroup().replace("_mapped_", "")).withVersion(version);
        if (artifact.getExtension().equals("jar")) {
            if (Objects.equals(artifact.getClassifier(), "sources")) {
                return null;
            }
            final var found = resolveArtifact(art);
            return found.map(it -> {
                        try {
                            return deobfuscator.binary(it, this, mappings.asMaps(), mappings.side);
                        } catch (IOException e) {
                            Utils.throwUnchecked(e);
                            return null;
                        }
                    }
            ).orElse(null);
        }
        return null;
    }

    @Nullable
    private static MappingData getMappings(String version) {
        if (!version.contains("_mapped_"))
            return null;
        final var maps = version.split("_mapped_")[1].split("_");
        return new MappingData(maps[0], maps[1], Deobfuscator.Side.by(maps.length > 2 ? maps[2] : "client"));
    }

    private Optional<File> resolveArtifact(Artifact artifact) {
        final var deps = resolveConfiguration().getFirstLevelModuleDependencies(artifact.asDependencySpec()).stream();
        return deps.flatMap(
                d -> d.getModuleArtifacts().stream()
                        .filter(artifact.asArtifactMatcher())
        ).map(ResolvedArtifact::getFile).filter(File::exists).findAny();
    }


    private ResolvedConfiguration resolveConfiguration() {
        synchronized (configuration) {
            if (resolvedOrigin == null) {
                resolvedOrigin = configuration.getResolvedConfiguration();
            }

            return resolvedOrigin;
        }
    }

    record MappingData(String channel, String version, Deobfuscator.Side side) {
        public MinecraftExtension.Mappings asMaps() {
            return new MinecraftExtension.Mappings(channel, version);
        }
    }
}
