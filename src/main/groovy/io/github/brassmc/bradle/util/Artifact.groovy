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

import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.specs.Spec

import javax.annotation.Nullable
import java.util.function.Predicate

public class Artifact implements ArtifactIdentifier, Serializable {

    // group:name:version[:classifier][@extension]
    private final String group;
    private final String name;
    private final String version;
    @Nullable
    private final String classifier;
    @Nullable
    private final String ext;

    // Cached after building the first time we're asked
    // Transient field so these aren't serialized
    @Nullable
    private transient String path;
    @Nullable
    private transient String file;
    @Nullable
    private transient String fullDescriptor;
    @Nullable
    private transient Boolean isSnapshot;

    static Artifact from(String descriptor) {
        String group, name, version;
        String ext = null, classifier = null;

        String[] pts = descriptor.split(':')
        group = pts[0];
        name = pts[1];

        int last = pts.length - 1;
        int idx = pts[last].indexOf('@');
        if (idx != -1) { // we have an extension
            ext = pts[last].substring(idx + 1);
            pts[last] = pts[last].substring(0, idx);
        }

        version = pts[2];

        if (pts.length > 3) // We have a classifier
            classifier = pts[3];

        return new Artifact(group, name, version, classifier, ext);
    }

    public static Artifact from(ArtifactIdentifier identifier) {
        return new Artifact(identifier.getGroup(), identifier.getName(), identifier.getVersion(), identifier.getClassifier(), identifier.getExtension());
    }

    public static Artifact from(String group, String name, String version, @Nullable String classifier, @Nullable String ext) {
        return new Artifact(group, name, version, classifier, ext);
    }

    Artifact(String group, String name, String version, @Nullable String classifier, @Nullable String ext) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.ext = ext != null ? ext : "jar";
    }

    public String getLocalPath() {
        return getPath().replace('/', File.separatorChar);
    }

    public String getDescriptor() {
        if (fullDescriptor == null) {
            StringBuilder buf = new StringBuilder();
            buf.append(this.group).append(':').append(this.name).append(':').append(this.version);
            if (this.classifier != null) {
                buf.append(':').append(this.classifier);
            }
            if (ext != null && !"jar".equals(this.ext)) {
                buf.append('@').append(this.ext);
            }
            this.fullDescriptor = buf.toString();
        }
        return fullDescriptor;
    }

    public String getPath() {
        if (path == null) {
            this.path = String.join("/", this.group.replace('.', '/'), this.name, this.version, getFilename());
        }
        return path;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    @Nullable
    public String getClassifier() {
        return classifier;
    }

    @Override
    @Nullable
    public String getExtension() {
        return ext;
    }

    public String getFilename() {
        if (file == null) {
            String file;
            file = this.name + '-' + this.version;
            if (this.classifier != null) file += '-' + this.classifier;
            file += '.' + this.ext;
            this.file = file;
        }
        return file;
    }

    public boolean isSnapshot() {
        if (isSnapshot == null) {
            this.isSnapshot = this.version.toLowerCase(Locale.ROOT).endsWith("-snapshot");
        }
        return isSnapshot;
    }

    Artifact withVersion(String version) {
        return Artifact.from(group, name, version, classifier, ext);
    }
    Artifact withGroup(String group) {
        return Artifact.from(group, name, version, classifier, ext);
    }

    @Override
    public String toString() {
        return getDescriptor();
    }

    @Override
    public int hashCode() {
        return getDescriptor().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Artifact &&
                this.getDescriptor().equals(((Artifact) o).getDescriptor());
    }

    public Spec<Dependency> asDependencySpec() {
        return (dep) -> group.equals(dep.getGroup()) && name.equals(dep.getName()) && version.equals(dep.getVersion());
    }

    public Predicate<ResolvedArtifact> asArtifactMatcher() {
        return (art) -> {
            String theirClassifier;
            if (art.getClassifier() == null) {
                theirClassifier = "";
            } else {
                theirClassifier = art.getClassifier();
            }

            String theirExt;
            if (art.getExtension().isEmpty()) {
                theirExt = "jar";
            } else {
                theirExt = art.getExtension();
            }

            return (classifier == null || classifier.equals(theirClassifier)) && (ext == null || ext.equals(theirExt));
        };
    }
}
