package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.List;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.BaseDiscordCommand;
import me.gamingto12.discord.command.DiscordCommand;
import me.gamingto12.discord.command.DiscordCommandManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class HelpCommand extends BaseDiscordCommand
{
    private final DiscordCommandManager manager;

    public HelpCommand(Hexalon plugin, DiscordCommandManager manager)
    {
        super(plugin, "help", "General", List.of("?", "h"), false);
        this.manager = manager;
    }

    @Override
    protected List<String> getAdminRoleIds()
    {
        return Hexalon.getInstance().getConfig().getStringList("discord.adminRoleIds");
    }

    @Override
    public void execute(User user, Guild guild, TextChannel channel, List<String> args) {
        StringBuilder sb = new StringBuilder();
        for (DiscordCommand cmd : manager.getCommands()) {
            sb.append("`")
                    .append(cmd.getName())
                    .append("` - ")
                    .append(cmd.getCategory())
                    .append("\n");
        }

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("Available commands")
                        .setDescription(sb.toString().trim())
                        .setColor(0x3B82F6)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }
}
