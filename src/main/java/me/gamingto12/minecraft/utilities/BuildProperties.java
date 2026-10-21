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

    public BuildProperties()
    {
        super();

        try (InputStream in = Hexalon.class.getClassLoader().getResourceAsStream("build.properties"))
        {
            if (in != null)
                load(in);
        }
        catch (IOException ignored)
        {
        }

        author = getProperty("buildAuthor", "unknown");
        version = getProperty("buildVersion", "4.2.4");
        number = getProperty("buildNumber", "1");
        date = getProperty("buildDate", "unknown");
    }
}