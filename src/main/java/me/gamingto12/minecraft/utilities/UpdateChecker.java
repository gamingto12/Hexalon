package me.gamingto12.minecraft.utilities;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

import me.gamingto12.Hexalon;

public final class UpdateChecker
{
    private static final URI VERSION_URI = URI.create("https://gamingto12.me/hexalon");
    private static final URI DEFAULT_UPDATE_JAR_URI = URI.create("https://gamingto12.me/hexalon.jar");
    private static final URI FALLBACK_UPDATE_JAR_URI = URI.create(
            "https://github.com/gamingto12/Hexalon/releases/latest/download/Hexalon.jar");
    private static final long CHECK_INTERVAL_TICKS = 60L * 60 * 20;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Hexalon plugin;
    private final AtomicBoolean checkInProgress = new AtomicBoolean();

    public UpdateChecker(Hexalon plugin)
    {
        this.plugin = plugin;
    }

    public void checkForUpdates()
    {
        var scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskAsynchronously(plugin, this::runCheck);
        scheduler.runTaskTimerAsynchronously(plugin, this::runCheck,
                CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    private void runCheck()
    {
        if (!checkInProgress.compareAndSet(false, true))
            return;
        try
        {
            fetchAndCompare();
        }
        finally
        {
            checkInProgress.set(false);
        }
    }

    private void fetchAndCompare()
    {
        HttpRequest request = HttpRequest.newBuilder(VERSION_URI)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "text/plain")
                .GET()
                .build();

        try
        {
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200)
            {
                plugin.getSLF4JLogger().warn("Hexalon update check returned HTTP {}", response.statusCode());
                return;
            }

            String currentVersion = plugin.getBuildMeta().getVersion().trim();
            String latestVersion = response.body().trim();
            int comparison;
            try
            {
                comparison = compareVersions(latestVersion, currentVersion);
            }
            catch (IllegalArgumentException e)
            {
                plugin.getSLF4JLogger().warn("Hexalon update check returned an invalid version: {}", latestVersion);
                return;
            }

            if (comparison > 0)
            {
                plugin.getSLF4JLogger().warn("A newer Hexalon version is available: {} (installed: {}).", latestVersion, currentVersion);
                downloadAndInstallUpdate();
            }
            else
                plugin.getSLF4JLogger().info("Hexalon is up to date ({}).", currentVersion);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            plugin.getSLF4JLogger().warn("Hexalon update check was interrupted.");
        }
        catch (IOException e)
        {
            plugin.getSLF4JLogger().warn("Could not check for Hexalon updates: {}", e.getMessage());
        }
    }

    private void downloadAndInstallUpdate() throws IOException, InterruptedException
    {
        Path installedJar = plugin.getPluginFile().toPath().toAbsolutePath();
        Path pluginDirectory = installedJar.getParent();
        if (pluginDirectory == null || installedJar.getFileName() == null)
            throw new IOException("Could not determine the plugin JAR directory");

        Path updateDirectory = pluginDirectory.resolve("update");
        Files.createDirectories(updateDirectory);
        Path stagedJar = updateDirectory.resolve(installedJar.getFileName());
        Path temporaryJar = Files.createTempFile(updateDirectory, "Hexalon-update-", ".jar");
        try
        {
            List<URI> candidates = new ArrayList<>();
            String configuredUrl = plugin.getConfig().getString("updater.jar-url", DEFAULT_UPDATE_JAR_URI.toString());
            try
            {
                candidates.add(URI.create(configuredUrl));
            }
            catch (IllegalArgumentException error)
            {
                plugin.getSLF4JLogger().warn("Invalid updater.jar-url '{}'; using the release fallback.", configuredUrl);
            }
            if (!candidates.contains(FALLBACK_UPDATE_JAR_URI))
                candidates.add(FALLBACK_UPDATE_JAR_URI);

            IOException lastFailure = null;
            boolean downloaded = false;
            for (URI candidate : candidates)
            {
                try
                {
                    if (downloadCandidate(candidate, temporaryJar))
                    {
                        downloaded = true;
                        break;
                    }
                }
                catch (IOException error)
                {
                    lastFailure = error;
                    plugin.getSLF4JLogger().warn("Could not download Hexalon update from {}: {}",
                            candidate, error.getMessage());
                }
            }
            if (!downloaded)
                throw new IOException("No valid Hexalon update artifact could be downloaded", lastFailure);

            try
            {
                Files.move(temporaryJar, stagedJar, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException ignored)
            {
                Files.move(temporaryJar, stagedJar, StandardCopyOption.REPLACE_EXISTING);
            }
            plugin.getSLF4JLogger().info("Hexalon update is staged at {}; it will take effect after restart.", stagedJar);
        }
        finally
        {
            Files.deleteIfExists(temporaryJar);
        }
    }

    private boolean downloadCandidate(URI source, Path destination) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(source)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        HttpResponse<java.io.InputStream> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofInputStream());
        try (var body = response.body())
        {
            if (response.statusCode() != 200)
            {
                plugin.getSLF4JLogger().warn("Update source {} returned HTTP {}; trying the next source.",
                        source, response.statusCode());
                return false;
            }
            Files.copy(body, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        try (JarFile jar = new JarFile(destination.toFile()))
        {
            if (jar.getEntry("paper-plugin.yml") != null)
                return true;
        }
        catch (IOException ignored)
        {
        }
        plugin.getSLF4JLogger().warn("Update source {} did not return a valid plugin JAR; trying the next source.", source);
        return false;
    }

    private static int compareVersions(String first, String second)
    {
        if (!first.matches("\\d+(?:\\.\\d+)*") || !second.matches("\\d+(?:\\.\\d+)*"))
            throw new IllegalArgumentException("Versions must contain dot-separated numbers");

        String[] firstParts = first.split("\\.");
        String[] secondParts = second.split("\\.");
        int length = Math.max(firstParts.length, secondParts.length);
        for (int index = 0; index < length; index++)
        {
            BigInteger firstPart = index < firstParts.length ? new BigInteger(firstParts[index]) : BigInteger.ZERO;
            BigInteger secondPart = index < secondParts.length ? new BigInteger(secondParts[index]) : BigInteger.ZERO;
            int comparison = firstPart.compareTo(secondPart);
            if (comparison != 0)
                return comparison;
        }

        return 0;
    }
}