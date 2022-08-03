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

package io.github.brassmc.bradle.util.gson

import groovy.transform.CompileStatic

import javax.annotation.Nullable

@CompileStatic
@SuppressWarnings('unused')
class Library {
    String name
    Downloads downloads
    @Nullable
    Rule[] rules

    boolean canContinue() {
        if (!rules) return true
        for (rule in rules) {
            if (!rule) {
                return false
            }
        }
        return true
    }

    @CompileStatic
    static class Downloads {
        Artifact artifact
    }

    @CompileStatic
    static class Artifact {
        String path
        String sha1
        int size
        String url
    }

    @CompileStatic
    static class Rule {
        String action
        OsCondition os

        boolean asBoolean() {
            return (os == null || os.platformMatches()) && action == 'allow'
        }
    }

    @CompileStatic
    static class OsCondition {
        @Nullable
        public String name
        @Nullable
        public String version
        @Nullable
        public String arch

        boolean nameMatches() {
            return name == null || OS.getCurrent().getName() == name
        }

        boolean versionMatches() {
            return version == null || System.getProperty('os.version') =~ /${version}/
        }

        boolean archMatches() {
            return arch == null || System.getProperty('os.arch') =~ /${arch}/
        }

        boolean platformMatches() {
            return nameMatches() && versionMatches() && archMatches()
        }
    }

    enum OS {
        WINDOWS("windows", "win"),
        LINUX("linux", "linux", "unix"),
        OSX("osx", "mac"),
        UNKNOWN("unknown");

        private final String name
        private final String[] keys

        OS(String name, String... keys) {
            this.name = name
            this.keys = keys
        }

        String getName() {
            return this.name
        }

        static OS getCurrent() {
            String prop = System.getProperty("os.name").toLowerCase(Locale.ENGLISH)
            for (os in values()) {
                for (key in os.keys) {
                    if (key in prop) {
                        return os
                    }
                }
            }
            return UNKNOWN
        }
    }
}
