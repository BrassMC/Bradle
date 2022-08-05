package io.github.brassmc.bradle.task

import groovy.transform.CompileStatic
import io.github.brassmc.bradle.Bradle
import io.github.brassmc.bradle.mc.MinecraftExtension
import io.github.brassmc.bradle.util.HashFunction
import io.github.brassmc.bradle.util.Utils
import io.github.brassmc.bradle.util.gson.PistonMeta
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@CompileStatic
abstract class DownloadAssetsTask extends DefaultTask {
    DownloadAssetsTask() {
        getAssetRepository().convention("https://resources.download.minecraft.net/")
        getConcurrentDownloads().convention(8)
    }

    @TaskAction
    void run() throws IOException, InterruptedException {
        final index = Utils.loadJson(getIndex(), AssetIndex.class)
        final keys = new ArrayList<>(index.objects.keySet())
        Collections.sort(keys)
        removeDuplicateRemotePaths(keys, index)

        final out = getOutput().toPath().resolve('objects')
        final assetsPath = Bradle.getMCDir().toPath().resolve('assets/objects')
        ExecutorService executorService = Executors.newFixedThreadPool(getConcurrentDownloads().get())
        CopyOnWriteArrayList<String> failedDownloads = new CopyOnWriteArrayList<>()
        final assetRepo = getAssetRepository().get()
        for (String key : keys) {
            final asset = index.objects.get(key)
            final target = out.resolve(asset.path)
            if (Files.notExists(target) || HashFunction.SHA1.hash(target) != asset.hash) {
                if (Files.notExists(target.parent)) Files.createDirectories(target.parent)
                final url = new URL(assetRepo + asset.getPath())
                Runnable copyURLtoFile = () -> {
                    final localFile = assetsPath.resolve(asset.getPath())
                    if (Files.exists(localFile)) {
                        Files.copy(localFile, target)
                    } else {
                        try (final is = url.openStream()) {
                            Files.write(target, is.readAllBytes())
                        }
                    }
                    if (HashFunction.SHA1.hash(target) != asset.hash) {
                        failedDownloads.add(key)
                        Files.deleteIfExists target
                        getProject().getLogger().error("Hash validation failed for {}", key)
                    }
                }
                executorService.execute(Utils.catchingExceptions(copyURLtoFile, (Exception e) -> {
                    failedDownloads.add key
                    getProject().getLogger().error('Failed to download {}: ', key, e)
                }))
            }
        }
        executorService.shutdown()
        executorService.awaitTermination(8, TimeUnit.HOURS)
        if (failedDownloads) {
            String errorMessage = ''
            for (String key : failedDownloads) {
                errorMessage += "Failed to get asset: $key\n"
            }
            errorMessage += 'Some assets failed to download or validate, try running the task again.'
            throw new RuntimeException(errorMessage)
        }
    }

    // Some keys may reference the same remote file. Remove these duplicates to prevent two threads
    // writing to the same file on disk.
    private static void removeDuplicateRemotePaths(List<String> keys, AssetIndex index) {
        Set<String> seen = new HashSet<>(keys.size())
        keys.removeIf(key -> !seen.add(index.objects.get(key).getPath()))
    }

    private Path getIndex() throws IOException {
        final data = PistonMeta.Store.getVersion(getMcVersion().getOrElse(
                MinecraftExtension.get(getProject()).getMinecraftVersion().get()
        )).resolvePackage()
        final target = Bradle.cachePath.resolve("assets/indexes/${data.assetIndex.id}.json")
        return Utils.updateOrDownload(target, data.assetIndex.url)
    }

    @Input
    @Optional
    abstract Property<String> getMcVersion()

    /**
     * The Base URL that will be used to download Minecraft assets.
     * A trailing slash is required.
     */
    @Input
    @Optional
    abstract Property<String> getAssetRepository()

    /**
     * Defines how many threads will be used to download assets concurrently.
     */
    @Input
    @Optional
    abstract Property<Integer> getConcurrentDownloads()

    @OutputDirectory
    @SuppressWarnings('GrMethodMayBeStatic')
    File getOutput() {
        Bradle.cachePath.resolve('assets').toFile()
    }

    @CompileStatic
    private static class AssetIndex {
        Map<String, Asset> objects
    }

    @CompileStatic
    private static class Asset {
        String hash

        String getPath() {
            return hash.substring(0, 2) + '/' + hash
        }
    }
}