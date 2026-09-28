package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.BaseDiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class ListCommand extends BaseDiscordCommand
{

    public ListCommand(Hexalon plugin) {
        super(plugin, "list", "Utility", Collections.emptyList(), false);
    }

    @Override
    public void execute(User user, Guild guild, TextChannel channel, List<String> args)
    {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        String playerList = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        if (playerList.isEmpty()) {
            playerList = "No players online.";
        }

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Players online")
                        .setDescription("**" + online + "/" + max + "**\n" + playerList)
                        .setColor(0x22C55E)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }

    @Override
    protected List<String> getAdminRoleIds() {
        return Collections.emptyList();
    }
}
