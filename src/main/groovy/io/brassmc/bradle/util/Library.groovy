package io.brassmc.bradle.util

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

    static enum OS {
        WINDOWS('windows', 'win'),
        LINUX('linux', 'linux', 'unix'),
        OSX('osx', 'mac', 'osx'),
        UNKNOWN('unknown');

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
                    if (prop.contains(key)) {
                        return os
                    }
                }
            }
            return UNKNOWN
        }
    }
}