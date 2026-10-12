package me.gamingto12.minecraft.utilities;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import me.gamingto12.Hexalon;

public final class UpdateChecker
{
    private static final URI VERSION_URI = URI.create("https://gamingto12.me/hexalon");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Hexalon plugin;

    public UpdateChecker(Hexalon plugin)
    {
        this.plugin = plugin;
    }

    public void checkForUpdates()
    {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::fetchAndCompare);
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
                plugin.getSLF4JLogger().warn("A newer Hexalon version is available: {} (installed: {}).", latestVersion, currentVersion);
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