package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.Locale;

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
            .setDescription(String.format(Locale.ROOT,
                "**1m:** %.2f\n**5m:** %.2f\n**15m:** %.2f", tps[0], tps[1], tps[2]))
                .setColor(0x3B82F6)
                .setTimestamp(Instant.now())
                .build()).queue();
    }
}
