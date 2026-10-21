package me.gamingto12.discord.console;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

final class ConsoleLogForwarder
{
    private static final Logger LOGGER = LogManager.getLogger(ConsoleLogForwarder.class);
    private static final int QUEUE_CAPACITY = 2048;
    private static final int MAX_DISCORD_CONTENT_LENGTH = 1900;
    private static final int MAX_BATCH_LINES = 12;
    private static final long POLL_INTERVAL_MILLIS = 300;

    private final TextChannel channel;
    private final ArrayBlockingQueue<String> lines = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong droppedLines = new AtomicLong();
    private volatile boolean running;
    private Thread worker;

    ConsoleLogForwarder(TextChannel channel)
    {
        this.channel = channel;
    }

    void start()
    {
        running = true;
        worker = new Thread(this::run, "Hexalon-Discord-Console");
        worker.setDaemon(true);
        worker.start();
    }

    void offer(String line)
    {
        if (!running || !lines.offer(line))
            droppedLines.incrementAndGet();
    }

    void stop()
    {
        running = false;
        if (worker == null)
            return;

        worker.interrupt();
        try
        {
            worker.join(3000);
        }
        catch (InterruptedException error)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void run()
    {
        StringBuilder batch = new StringBuilder();
        int batchLines = 0;

        while (running || !lines.isEmpty() || droppedLines.get() > 0)
        {
            try
            {
                String line = lines.poll(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
                long dropped = droppedLines.getAndSet(0);
                if (dropped > 0)
                    batchLines += appendLine(batch, "[Hexalon] Dropped " + dropped
                            + " console line(s) because the forwarding queue was full");

                if (line != null)
                    batchLines += appendLine(batch, line);

                if (batchLines >= MAX_BATCH_LINES || (line == null && !batch.isEmpty()))
                {
                    sendBatch(batch);
                    batchLines = 0;
                }
            }
            catch (InterruptedException ignored)
            {
                if (running)
                    continue;
            }
            catch (RuntimeException error)
            {
                LOGGER.warn("Could not forward a console log batch to Discord", error);
                batch.setLength(0);
                batchLines = 0;
            }
        }

        if (!batch.isEmpty())
            sendBatch(batch);
    }

    private int appendLine(StringBuilder batch, String line)
    {
        line = line.replace("```", "'''");
        int addedLines = 0;
        int offset = 0;
        do
        {
            int length = Math.min(MAX_DISCORD_CONTENT_LENGTH, line.length() - offset);
            String chunk = line.substring(offset, offset + length);
            int separator = batch.isEmpty() ? 0 : 1;
            if (batch.length() + separator + chunk.length() > MAX_DISCORD_CONTENT_LENGTH)
                sendBatch(batch);
            if (!batch.isEmpty())
                batch.append('\n');
            batch.append(chunk);
            offset += length;
            addedLines++;
        }
        while (offset < line.length());
        return addedLines;
    }

    private void sendBatch(StringBuilder batch)
    {
        if (batch.isEmpty())
            return;

        String content = "```\n" + batch + "\n```";
        batch.setLength(0);
        channel.sendMessage(content).queue(null,
                error -> LOGGER.warn("Discord rejected a console log message", error));
    }
}