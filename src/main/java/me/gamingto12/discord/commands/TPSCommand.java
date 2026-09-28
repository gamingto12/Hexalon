package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.List;

import org.bukkit.Bukkit;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.BaseDiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class TPSCommand extends BaseDiscordCommand
{
    public TPSCommand(Hexalon plugin)
    {
        super(plugin, "tps", "Server", List.of(), false);
    }

    @Override
    protected List<String> getAdminRoleIds()
    {
        return Hexalon.getInstance().getConfig().getStringList("discord.adminRoleIds");
    }

    @Override
    public void execute(User user, Guild guild, TextChannel channel, List<String> args)
    {
        double[] tps = Bukkit.getServer().getTPS();
        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Server TPS")
                        .setDescription(
                                "**1m:** " + String.format("%.2f", tps[0]) + "\n" +
                                "**5m:** " + String.format("%.2f", tps[1]) + "\n" +
                                "**15m:** " + String.format("%.2f", tps[2])
                        )
                        .setColor(0x3B82F6)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }
}

