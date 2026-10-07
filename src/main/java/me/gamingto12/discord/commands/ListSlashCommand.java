package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.gamingto12.discord.command.DiscordSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ListSlashCommand implements DiscordSlashCommand
{
    public ListSlashCommand() {}

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
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String playerList = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
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
