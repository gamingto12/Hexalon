package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import me.gamingto12.discord.command.DiscordSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ListSlashCommand implements DiscordSlashCommand
{
    @Override
    public String getName()
    {
        return "list";
    }

    @Override
    public String getDescription()
    {
        return "Show which players are currently online.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
        Collection<? extends org.bukkit.entity.Player> players = Bukkit.getOnlinePlayers();
        int online = players.size();
        int max = Bukkit.getMaxPlayers();
        String playerList = players.stream()
            .map(org.bukkit.entity.Player::getName)
                .collect(Collectors.joining(", "));

        if (playerList.isBlank())
            playerList = "No players online.";

        event.replyEmbeds(new EmbedBuilder()
                .setTitle("Players online")
                .setDescription("**" + online + "/" + max + "**\n" + playerList)
                .setColor(0x22C55E)
                .setTimestamp(Instant.now())
                .build()).queue();
    }
}
