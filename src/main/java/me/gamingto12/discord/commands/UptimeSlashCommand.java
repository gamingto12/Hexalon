package me.gamingto12.discord.commands;

import java.time.Instant;

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
        long uptimeMillis = System.currentTimeMillis() - startTime;
        long seconds = uptimeMillis / 1000 % 60;
        long minutes = uptimeMillis / (1000 * 60) % 60;
        long hours = uptimeMillis / (1000 * 60 * 60) % 24;
        long days = uptimeMillis / (1000 * 60 * 60 * 24);

        event.replyEmbeds(new EmbedBuilder()
                .setTitle("Server uptime")
                .setDescription(String.format("%d days, %02d hours, %02d minutes, %02d seconds", days, hours, minutes, seconds))
                .setColor(0x7C3AED)
                .setTimestamp(Instant.now())
                .build()).queue();
    }
}
