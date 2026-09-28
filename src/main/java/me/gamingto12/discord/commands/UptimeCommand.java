package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.List;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.BaseDiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class UptimeCommand extends BaseDiscordCommand
{
    private final long startTime;

    public UptimeCommand(Hexalon plugin, long startTime)
    {
        super(plugin, "uptime", "Server", List.of(), false);
        this.startTime = startTime;
    }

    @Override
    protected List<String> getAdminRoleIds()
    {
        return Hexalon.getInstance().getConfig().getStringList("discord.adminRoleIds");
    }

    @Override
    public void execute(User user, Guild guild, TextChannel channel, List<String> args)
    {
        long uptimeMillis = System.currentTimeMillis() - startTime;
        long seconds = uptimeMillis / 1000 % 60;
        long minutes = uptimeMillis / (1000 * 60) % 60;
        long hours = uptimeMillis / (1000 * 60 * 60) % 24;
        long days = uptimeMillis / (1000 * 60 * 60 * 24);

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Server uptime")
                        .setDescription(String.format("%d days, %02d hours, %02d minutes, %02d seconds", days, hours, minutes, seconds))
                        .setColor(0x7C3AED)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }
}