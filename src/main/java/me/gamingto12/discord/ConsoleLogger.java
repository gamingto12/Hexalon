package me.gamingto12.discord;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import me.gamingto12.Hexalon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

/**
 * Captures server console/log output by attaching a Log4j2 Appender to the
 * root logger, and relays batched lines to a Discord channel.
 */
public class ConsoleLogger extends AbstractAppender
{
    private static final String APPENDER_NAME = "KoolJDA-ConsoleLogger";
    private static final int DISCORD_MESSAGE_LIMIT = 2000;
    private static final String CODE_BLOCK_START = "```\n";
    private static final String CODE_BLOCK_END = "\n```";
    private static final int MAX_LOG_CONTENT_LENGTH = DISCORD_MESSAGE_LIMIT
            - CODE_BLOCK_START.length() - CODE_BLOCK_END.length();
    private static final long MIN_DISCORD_UPDATE_INTERVAL_MILLIS = 1100;
    private static final Pattern ANSI_FORMATTING = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern MINECRAFT_FORMATTING = Pattern.compile(
            "(?i)§(?:x(?:§[0-9a-f]){6}|[0-9a-fk-or])");

    private final Hexalon plugin;
    private final DiscordBot bot;

    private final LinkedBlockingQueue<String> consoleQueue = new LinkedBlockingQueue<>();

    private Thread consoleThread;
    private volatile boolean running = false;
    private Message activeDiscordMessage;
    private String activeMessageBody = "";
    private String activeChannelId;
    private long lastDiscordUpdateMillis;

    public ConsoleLogger(Hexalon plugin, DiscordBot discordBot)
    {
        super(APPENDER_NAME, null, buildLayout(), true, Property.EMPTY_ARRAY);
        this.plugin = plugin;
        this.bot = discordBot;
    }

    private static Layout<String> buildLayout()
    {
        // Plain pattern - no Minecraft/ANSI color codes, those don't render in Discord.
        return PatternLayout.newBuilder()
                .withPattern("[%d{HH:mm:ss}] [%level] %msg")
                .build();
    }

    /**
     * Starts capturing console output.
     */
    public synchronized void start()
    {
        if (running)
            return;

        running = true;
        super.start(); // Appender lifecycle - must be started before it will accept events

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        // Attach ourselves to every logger config so we see everything the
        // console sees (this mirrors what TerminalConsoleAppender receives).
        for (LoggerConfig loggerConfig : config.getLoggers().values())
        {
            loggerConfig.addAppender(this, null, null);
        }
        config.getRootLogger().addAppender(this, null, null);
        context.updateLoggers();

        consoleThread = new Thread(this::processConsoleQueue, "KoolJDA-Console-Logger");
        consoleThread.setDaemon(true);
        consoleThread.start();

        plugin.getSLF4JLogger().info("Console logger started - output will be sent to Discord");
    }

    /**
     * Stops capturing console output.
     */
    public synchronized void stop()
    {
        if (!running)
            return;

        running = false;

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        for (LoggerConfig loggerConfig : config.getLoggers().values())
        {
            loggerConfig.removeAppender(APPENDER_NAME);
        }
        config.getRootLogger().removeAppender(APPENDER_NAME);
        context.updateLoggers();

        super.stop();

        if (consoleThread != null)
        {
            consoleThread.interrupt();
            try
            {
                consoleThread.join(5000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        plugin.getSLF4JLogger().info("Console logger stopped");
    }

    /**
     * Called by Log4j2 for every log event, on the logging thread. Keep this fast -
     * just format and queue, never block or touch Discord here directly.
     */
    @Override
    public void append(LogEvent event)
    {
        if (!running)
            return;

        // Avoid feedback loops: don't re-queue our own plugin's log lines
        // or JDA's internal chatter about failed sends.
        String loggerName = event.getLoggerName();
        if (loggerName != null && loggerName.startsWith("net.dv8tion"))
            return;

        String formatted = getLayout().toSerializable(event).toString().stripTrailing();
        formatted = ANSI_FORMATTING.matcher(formatted).replaceAll("");
        formatted = MINECRAFT_FORMATTING.matcher(formatted).replaceAll("");
        if (!formatted.isEmpty())
            consoleQueue.offer(formatted);
    }

    /**
     * Processes the console queue and sends batches to Discord.
     */
    private void processConsoleQueue()
    {
        StringBuilder batch = new StringBuilder();
        int lineCount = 0;

        while (running || !consoleQueue.isEmpty())
        {
            try
            {
                String line = consoleQueue.poll(500, TimeUnit.MILLISECONDS);

                if (line != null)
                {
                    if (!batch.isEmpty())
                        batch.append("\n");

                    batch.append(line);
                    lineCount++;

                    int maxMessageLength = 1990;
                    int maxBatchSize = 10;
                    if (batch.length() >= maxMessageLength || lineCount >= maxBatchSize)
                    {
                        flushBatch(batch.toString());
                        batch = new StringBuilder();
                        lineCount = 0;
                    }
                }
                else if (!batch.isEmpty())
                {
                    flushBatch(batch.toString());
                    batch = new StringBuilder();
                    lineCount = 0;
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                if (!running)
                    break;
            }
            catch (Exception e)
            {
                plugin.getSLF4JLogger().error("Error processing console queue", e);
            }
        }

        if (!batch.isEmpty())
        {
            flushBatch(batch.toString());
        }
    }

    /**
     * Sends a batch of console messages to Discord.
     */
    private void flushBatch(String messages)
    {
        try
        {
            String consoleChannelId = plugin.getConfig().getString("discord.channels.console-id", "");
            if (consoleChannelId.isBlank())
                return; // channel not configured

            TextChannel channel = bot.getTextChannel(consoleChannelId);
            if (channel == null)
                return;

            if (!consoleChannelId.equals(activeChannelId))
            {
                activeDiscordMessage = null;
                activeMessageBody = "";
                activeChannelId = consoleChannelId;
            }

            String[] lines = messages.stripTrailing().split("\n", -1);
            for (String line : lines)
            {
                if (line.length() <= MAX_LOG_CONTENT_LENGTH)
                {
                    appendLine(channel, line);
                    continue;
                }

                int offset = 0;
                boolean continuation = false;
                while (offset < line.length())
                {
                    String marker = continuation ? "... " : "";
                    int chunkLength = Math.min(MAX_LOG_CONTENT_LENGTH - marker.length(), line.length() - offset);
                    appendLine(channel, marker + line.substring(offset, offset + chunkLength));
                    offset += chunkLength;
                    continuation = true;
                }
            }

            sendActiveMessage(channel);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            activeDiscordMessage = null;
            activeMessageBody = "";
        }
        catch (Exception e)
        {
            activeDiscordMessage = null;
            activeMessageBody = "";
            // Log via SLF4J at debug/warn - NOT via System.err/println, to avoid
            // Paper's "please use your logger" nag and to avoid any risk of
            // feedback loops now that we're hooked into Log4j directly.
            plugin.getSLF4JLogger().warn("Error sending console batch to Discord: {}", e.getMessage());
        }
    }

    private void appendLine(TextChannel channel, String line) throws InterruptedException
    {
        int separatorLength = activeMessageBody.isEmpty() ? 0 : 1;
        if (activeMessageBody.length() + separatorLength + line.length() > MAX_LOG_CONTENT_LENGTH)
        {
            sendActiveMessage(channel);
            activeDiscordMessage = null;
            activeMessageBody = "";
        }

        activeMessageBody = activeMessageBody.isEmpty()
                ? line
                : activeMessageBody + "\n" + line;
    }

    private void sendActiveMessage(TextChannel channel) throws InterruptedException
    {
        if (activeMessageBody.isEmpty())
            return;

        long waitMillis = MIN_DISCORD_UPDATE_INTERVAL_MILLIS
                - (System.currentTimeMillis() - lastDiscordUpdateMillis);
        if (waitMillis > 0)
            Thread.sleep(waitMillis);

        lastDiscordUpdateMillis = System.currentTimeMillis();
        String formattedMessage = CODE_BLOCK_START + activeMessageBody + CODE_BLOCK_END;

        if (activeDiscordMessage == null)
            activeDiscordMessage = channel.sendMessage(formattedMessage).complete();
        else
            activeDiscordMessage = activeDiscordMessage.editMessage(formattedMessage).complete();
    }
}