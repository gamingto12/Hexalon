package me.gamingto12.discord.console;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.DiscordBot;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public final class ConsoleLogger
{
    private final Hexalon plugin;
    private final DiscordBot discordBot;
    private ConsoleLogAppender appender;
    private ConsoleLogForwarder forwarder;

    public ConsoleLogger(Hexalon plugin, DiscordBot discordBot)
    {
        this.plugin = plugin;
        this.discordBot = discordBot;
    }

    public synchronized void start()
    {
        if (appender != null)
            return;

        String channelId = plugin.getConfig().getString("discord.channels.console-id", "").trim();
        TextChannel channel = discordBot.getTextChannel(channelId);
        if (channelId.isBlank() || channel == null)
            return;

        forwarder = new ConsoleLogForwarder(channel);
        appender = new ConsoleLogAppender(forwarder::offer);
        forwarder.start();
        appender.start();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        for (LoggerConfig loggerConfig : configuration.getLoggers().values())
        {
            if (!loggerConfig.isAdditive())
                loggerConfig.addAppender(appender, null, null);
        }
        configuration.getRootLogger().addAppender(appender, null, null);
        context.updateLoggers();
    }

    public synchronized void stop()
    {
        if (appender == null)
            return;

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        for (LoggerConfig loggerConfig : configuration.getLoggers().values())
            loggerConfig.removeAppender(ConsoleLogAppender.APPENDER_NAME);
        configuration.getRootLogger().removeAppender(ConsoleLogAppender.APPENDER_NAME);
        context.updateLoggers();

        appender.stop();
        forwarder.stop();
        appender = null;
        forwarder = null;
    }
}