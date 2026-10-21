package me.gamingto12.discord.console;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

final class ConsoleLogAppender extends AbstractAppender
{
    static final String APPENDER_NAME = "HexalonConsoleCapture";

    private static final Pattern ANSI_FORMATTING = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern MINECRAFT_FORMATTING = Pattern.compile(
            "(?i)§(?:x(?:§[0-9a-f]){6}|[0-9a-fk-or])");
    private static final Layout<String> LAYOUT = PatternLayout.newBuilder()
            .withPattern("[%d{HH:mm:ss}] [%level] %msg")
            .build();

    private final Consumer<String> lineSink;

    ConsoleLogAppender(Consumer<String> lineSink)
    {
        super(APPENDER_NAME, null, LAYOUT, true, Property.EMPTY_ARRAY);
        this.lineSink = lineSink;
    }

    @Override
    public void append(LogEvent event)
    {
        String loggerName = event.getLoggerName();
        if (loggerName != null && (loggerName.startsWith("net.dv8tion")
                || loggerName.startsWith("me.gamingto12.discord.console")))
            return;

        String line = getLayout().toSerializable(event).toString().stripTrailing();
        line = ANSI_FORMATTING.matcher(line).replaceAll("");
        line = MINECRAFT_FORMATTING.matcher(line).replaceAll("");
        if (!line.isBlank())
            lineSink.accept(line);
    }
}