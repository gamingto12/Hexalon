package me.gamingto12.discord.commands;

import java.time.Instant;
import java.time.Duration;

import me.gamingto12.discord.command.DiscordSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class UptimeSlashCommand implements DiscordSlashCommand
{
    private final long startTime;

    public UptimeSlashCommand(long startTime)
    {
        this.startTime = startTime;
    }

    @Override
    public String getName()
    {
        return "uptime";
    }

    @Override
    public String getDescription()
    {
        return "Show how long the server has been running.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
        Duration uptime = Duration.ofMillis(Math.max(0, System.currentTimeMillis() - startTime));
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        event.replyEmbeds(new EmbedBuilder()
                .setTitle("Server uptime")
                .setDescription(String.format("%d days, %02d hours, %02d minutes, %02d seconds", days, hours, minutes, seconds))
                .setColor(0x7C3AED)
                .setTimestamp(Instant.now())
                .build()).queue();
    }
}
