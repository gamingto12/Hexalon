package me.gamingto12.discord.commands;

import java.time.Instant;

import org.bukkit.Bukkit;

import me.gamingto12.discord.command.DiscordSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class TPSSlashCommand implements DiscordSlashCommand
{
    @Override
    public String getName()
    {
        return "tps";
    }

    @Override
    public String getDescription()
    {
        return "Get the current server TPS averages.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
        double[] tps = Bukkit.getServer().getTPS();
        event.replyEmbeds(new EmbedBuilder()
                .setTitle("Server TPS")
                .setDescription("**1m:** " + String.format("%.2f", tps[0]) + "\n"
                        + "**5m:** " + String.format("%.2f", tps[1]) + "\n"
                        + "**15m:** " + String.format("%.2f", tps[2]))
                .setColor(0x3B82F6)
                .setTimestamp(Instant.now())
                .build()).queue();
    }
}
