package io.github.brassmc.bradle.util

import groovy.transform.CompileStatic
import io.github.brassmc.bradle.Bradle
import io.github.brassmc.bradle.util.Artifact as BradleArtifact
import net.minecraftforge.artifactural.api.artifact.Artifact
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier
import net.minecraftforge.artifactural.api.artifact.ArtifactType
import net.minecraftforge.artifactural.api.repository.ArtifactProvider
import net.minecraftforge.artifactural.base.artifact.StreamableArtifact
import net.minecraftforge.artifactural.base.repository.ArtifactProviderBuilder
import net.minecraftforge.artifactural.base.repository.SimpleRepository
import net.minecraftforge.artifactural.gradle.GradleRepositoryAdapter
import org.gradle.api.Project
import org.slf4j.Logger

import javax.annotation.Nullable

@CompileStatic
abstract class BaseRepo implements ArtifactProvider<ArtifactIdentifier> {

    private final File cache
    protected final Logger log
    protected final String REPO_NAME = getClass().getSimpleName()

    protected BaseRepo(File cache, Logger log) {
        this.cache = cache
        this.log = log
    }

    protected File getCacheRoot() {
        return this.cache
    }

    protected File cache(String... path) {
        return new File(getCacheRoot(), String.join(File.separator, path))
    }

    protected static String clean(ArtifactIdentifier art) {
        return art.getGroup() + ":" + art.getName() + ":" + art.getVersion() + ":" + art.getClassifier() + "@" + art.getExtension()
    }

    protected void debug(String message) {
        this.log.debug(message)
    }

    @Override
    final Artifact getArtifact(ArtifactIdentifier artifact) {
        try {
            debug(REPO_NAME + " Request: " + clean(artifact))
            String[] pts = artifact.getExtension().split("\\.")

            String desc = (artifact.getGroup() + ":" + artifact.getName() + ":" + artifact.getVersion() + ":" + artifact.getClassifier() + "@" + pts[0]).intern()
            File ret
            synchronized (desc) {
                if (pts.length == 1)
                    ret = findFile(artifact)
                else // Call without the .md5/.sha extension.
                    ret = findFile(BradleArtifact.from(artifact.getGroup(), artifact.getName(), artifact.getVersion(), artifact.getClassifier(), pts[0]))
            }

            if (ret != null) {
                ArtifactType type = ArtifactType.OTHER
                if (artifact.getClassifier() != null && artifact.getClassifier().endsWith("sources"))
                    type = ArtifactType.SOURCE
                else if ("jar" == artifact.getExtension())
                    type = ArtifactType.BINARY

                if (pts.length == 1)
                    return StreamableArtifact.ofFile(artifact, type, ret)
                else if (pts.length == 2) {
                    File hash = new File(ret.getAbsolutePath() + "." + pts[1])
                    if (hash.exists())
                        return StreamableArtifact.ofFile(artifact, type, hash)
                }
            }
            return Artifact.none()
        } catch (Throwable e) {
            log.info("Error getting artifact: " + clean(artifact) + " from  " + REPO_NAME, e)
            return Artifact.none()
        }
    }

    @Nullable
    protected abstract File findFile(ArtifactIdentifier artifact) throws IOException

    @CompileStatic
    static class Builder {
        private List<ArtifactProvider<ArtifactIdentifier>> repos = new ArrayList<>()

        Builder add(@Nullable ArtifactProvider<ArtifactIdentifier> repo) {
            if (repo)
                repos.add(repo)
            return this
        }

        void attach(Project project) {
            int random = new Random().nextInt()
            File cache = Bradle.cachePath.resolve('repocache').toFile()
            // Java 8's compiler doesn't allow the lambda to be a method reference, but Java 16 allows it
            // noinspection Convert2MethodRef
            GradleRepositoryAdapter.add(project.getRepositories(), "MC_" + random, cache,
                    SimpleRepository.of(ArtifactProviderBuilder.begin(ArtifactIdentifier.class).provide(
                            (ArtifactIdentifier artifact) -> repos.stream()
                                    .map((ArtifactProvider<ArtifactIdentifier> repo) -> repo.getArtifact(artifact))
                                    .filter((Artifact s) -> s.isPresent())
                                    .findFirst()
                                    .orElse(Artifact.none())
                    ))
            )
        }
    }
}
