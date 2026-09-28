package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.BaseDiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class AdminConsoleCommand extends BaseDiscordCommand
{

    private final Hexalon plugin;

    public AdminConsoleCommand(Hexalon plugin)
    {
        super(Hexalon.getInstance(), "adminconsole", "Admin", List.of("c", "ac", "console"), true);
        this.plugin = plugin;
    }

    @Override
    protected List<String> getAdminRoleIds() {
        return plugin.getConfig().getStringList("discord.adminRoleIds");
    }

    @Override
    public void execute(User user, Guild guild, TextChannel channel, List<String> args)
    {
        execute(user, guild, channel, args, false);
    }

    @Override
    public void execute(User user, Guild guild, TextChannel channel, List<String> args,
                        boolean fromConsoleChannel)
    {
        if (!plugin.getConfig().getBoolean("discord.console.enabled", false))
        {
            channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("Console bridge disabled")
                            .setDescription("Remote console commands are disabled.")
                            .setColor(0xF59E0B)
                            .setTimestamp(Instant.now())
                            .build()
            ).queue();
            return;
        }
        if (args.isEmpty())
        {
            channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("Missing command")
                            .setDescription("You must provide a command to execute.")
                            .setColor(0xF59E0B)
                            .setTimestamp(Instant.now())
                            .build()
            ).queue();
            return;
        }
        String command = String.join(" ", args);
        String root = args.getFirst().toLowerCase(Locale.ROOT);
        List<String> blacklistedCommands = plugin.getConfig().getStringList("discord.console.disallowed-commands");
        if (blacklistedCommands.stream().anyMatch(value -> value.equalsIgnoreCase(root)))
        {
            channel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("Blocked command")
                            .setDescription("That command is not allowed through the Discord console bridge.")
                            .setColor(0xD64545)
                            .setTimestamp(Instant.now())
                            .build()
            ).queue();
            return;
        }

        plugin.getSLF4JLogger().info("Discord console command by {} ({}): {}", user.getName(), user.getId(), command);
        boolean accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (fromConsoleChannel)
            return;

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle(accepted ? "Command dispatched" : "Command rejected")
                        .setDescription("`" + command + "`")
                        .setColor(accepted ? 0x22C55E : 0xD64545)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }
}
