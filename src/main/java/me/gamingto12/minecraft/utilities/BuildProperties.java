package me.gamingto12.minecraft.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

        try (InputStream in = plugin.getResource("build.properties"))
        {
            if (in != null)
                load(in);
            else
                plugin.getSLF4JLogger().warn("Could not find build.properties; using plugin metadata defaults.");
        }
        catch (IOException ignored)
        {
        }

        author = getProperty("buildAuthor", "unknown");
        version = getProperty("buildVersion", plugin.getPluginMeta().getVersion());
        number = getProperty("buildNumber", "1");
        date = getProperty("buildDate", "unknown");
    }
}