package me.gamingto12.minecraft.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import lombok.Getter;
import me.gamingto12.Hexalon;

@Getter
public class BuildProperties extends Properties
{
    private final String author;
    private final String version;
    private final String number;
    private final String date;

    public BuildProperties(Hexalon plugin)
    {
        super();

        try (JarFile pluginJar = new JarFile(plugin.getPluginFile()))
        {
            JarEntry entry = pluginJar.getJarEntry("build.properties");
            if (entry == null)
                plugin.getSLF4JLogger().warn("Could not find build.properties in {}; using plugin metadata defaults.",
                        plugin.getPluginFile());
            else
            {
                try (InputStream in = pluginJar.getInputStream(entry))
                {
                    load(in);
                }
            }
        }
        catch (IOException error)
        {
            plugin.getSLF4JLogger().warn("Could not read build.properties from {}; using plugin metadata defaults: {}",
                    plugin.getPluginFile(), error.getMessage());
        }

        author = getProperty("buildAuthor", "unknown");
        version = getProperty("buildVersion", plugin.getPluginMeta().getVersion());
        number = getProperty("buildNumber", "1");
        date = getProperty("buildDate", "unknown");
    }
}