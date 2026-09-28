package me.gamingto12.discord.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;

import me.gamingto12.Hexalon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class DiscordCommandManager
{

    private final Hexalon plugin;
    private final List<DiscordCommand> commands = new ArrayList<>();

    public DiscordCommandManager(Hexalon plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(DiscordCommand command) {
        commands.add(command);
    }

    public List<DiscordCommand> getCommands()
    {
        return new ArrayList<>(commands); // Return a copy to avoid external modification
    }

    public void handleMessage(User user, Guild guild, TextChannel channel, String content)
    {
        handleMessage(user, guild, channel, content, false);
    }

    public void handleMessage(User user, Guild guild, TextChannel channel, String content,
                              boolean fromConsoleChannel)
    {
        if (user == null || guild == null || channel == null || content == null || content.isBlank())
            return;

        Bukkit.getScheduler().runTask(plugin,
                () -> handleMessageOnServerThread(user, guild, channel, content, fromConsoleChannel));
    }

    private void handleMessageOnServerThread(User user, Guild guild, TextChannel channel, String content,
                                             boolean fromConsoleChannel)
    {
        String prefix = Objects.requireNonNullElse(plugin.getConfig().getString("discord.prefix"), "!");

        if (!content.startsWith(prefix))
            return;

        String trimmed = content.substring(prefix.length()).trim();
        if (trimmed.isEmpty())
            return;

        List<String> args = new ArrayList<>(Arrays.asList(trimmed.split("\\s+")));
        String cmd = args.removeFirst().toLowerCase();

        for (DiscordCommand command : commands)
        {
            if (command.getName().equalsIgnoreCase(cmd) || command.getAliases().contains(cmd))
            {
                if (!command.canExecute(user, guild))
                {
                    new EmbedBuilder()
                            .setTitle("Permission denied")
                            .setDescription("You do not have permission to use this command.")
                            .setColor(0xD64545)
                            .setTimestamp(Instant.now())
                            .build();
                    channel.sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setTitle("Permission denied")
                                    .setDescription("You do not have permission to use this command.")
                                    .setColor(0xD64545)
                                    .setTimestamp(Instant.now())
                                    .build()
                    ).queue();
                    return;
                }
                command.execute(user, guild, channel, args, fromConsoleChannel);
                return;
            }
        }
    }
}
