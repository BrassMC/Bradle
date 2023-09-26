package io.brassmc.bradle

import io.brassmc.bradle.util.Library
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.nio.file.Path

class Bradle implements Plugin<Project> {
    private static final var MC_NAME = 'minecraft'
    private static final var MC_CLIENT_RUNTIME = 'minecraftClientRuntime'
    private static final var MC_SERVER_RUNTIME = 'minecraftServerRuntime'
    public static final var MOJANG_MAVEN_URL = 'https://libraries.minecraft.net'

    public static boolean isOffline = false
    public static Path cachePath

    @Override
    void apply(Project project) {
        // apply java plugin?

        isOffline = project.gradle.startParameter.offline
        cachePath = project.gradle.gradleUserHomeDir.toPath().resolve("caches/bradle")

        final var mc = project.extensions.create(MC_NAME, MinecraftExtension)
        final var clientRuntimeConf = project.configurations.create(MC_CLIENT_RUNTIME)
        final var serverRuntimeConf = project.configurations.create(MC_SERVER_RUNTIME)

        // configure deps?

        project.tasks.create('downloadAssets', DownloadAssetsTask) {
            it.setGroup(MC_NAME)
        }

        final obfConfiguration = project.configurations.create('bradleObfuscated')
        final bradleExt = project.extensions.create(BradleExtension.NAME, BradleExtension, project, obfConfiguration)
        project.afterEvaluate {
            bradleExt.mappingsConfiguredListeners.each {
                it.accept(mc.getMappings().get())
            }

            new BaseRepo.Builder()
                .add(new MCRepo(project.logger))
                .add(new DeobfuscatingRepo(project.logger, obfConfiguration, new SimpleObfuscator(mc)))
                .attach(project)

            // configure mavens?
        }
    }

    static File getMCDir() {
        switch (Library.OS.current) {
            case Library.OS.OSX:
                return new File(System.getProperty("user.home") + "/Library/Application Support/minecraft")
            case Library.OS.WINDOWS:
                return new File(System.getenv("APPDATA") + "\\.minecraft")
            case Library.OS.LINUX:
            default:
                return new File(System.getProperty("user.home") + "/.minecraft")
        }
    }
}
