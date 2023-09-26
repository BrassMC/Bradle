package io.brassmc.bradle.minecraft

import org.gradle.api.provider.Property

import javax.annotation.Nullable

abstract class MinecraftExtension {
    private final Map<String, MappingProvider> mappingProviders;

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

    void registerMappingsProvider(String name, MappingProvider mappingProvider) {
        mappingProviders.put(name, mappingProvider)
    }

    @Nullable
    MappingProvider getProvider(String name) {
        return mappingProviders[name]
    }

    static class Mappings {
        String channel
        String version

        Mappings(String channel, String version) {
            this.channel = channel
            this.version = version
        }
    }
}
