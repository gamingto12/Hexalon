package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.DiscordSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AdminConsoleSlashCommand implements DiscordSlashCommand
{
    private final Hexalon plugin;

    public AdminConsoleSlashCommand(Hexalon plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public String getName()
    {
        return "console";
    }

    @Override
    public String getDescription()
    {
        return "Run a server command from Discord.";
    }

    @Override
    public List<OptionData> getOptions()
    {
        return List.of(
                new OptionData(OptionType.STRING, "command", "The command to run on the server.", true)
        );
    }

    @Override
    public boolean isAdminOnly()
    {
        return true;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
        if (!plugin.getConfig().getBoolean("discord.console.enabled", false))
        {
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Console bridge disabled")
                    .setDescription("Remote console commands are disabled.")
                    .setColor(0xF59E0B)
                    .setTimestamp(Instant.now())
                    .build()).setEphemeral(true).queue();
            return;
        }

        String command = event.getOption("command", null, OptionMapping::getAsString).trim();
        if (command.isBlank())
        {
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Missing command")
                    .setDescription("You must provide a command to execute.")
                    .setColor(0xF59E0B)
                    .setTimestamp(Instant.now())
                    .build()).setEphemeral(true).queue();
            return;
        }

        String root = command.split("\\s+")[0].toLowerCase(Locale.ROOT);
        List<String> blocked = plugin.getConfig().getStringList("discord.console.disallowed-commands");
        if (blocked.stream().anyMatch(value -> value.equalsIgnoreCase(root)))
        {
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Blocked command")
                    .setDescription("That command is not allowed through the Discord console bridge.")
                    .setColor(0xD64545)
                    .setTimestamp(Instant.now())
                    .build()).setEphemeral(true).queue();
            return;
        }

        plugin.getSLF4JLogger().info("Discord console slash command by {} ({}): {}",
                event.getUser().getName(), event.getUser().getId(), command);

        event.deferReply(true).queue(interaction -> Bukkit.getScheduler().runTask(plugin, () ->
        {
            try
            {
                boolean accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                interaction.editOriginalEmbeds(new EmbedBuilder()
                        .setTitle(accepted ? "Command dispatched" : "Command rejected")
                        .setDescription("`" + command + "`")
                        .setColor(accepted ? 0x22C55E : 0xD64545)
                        .setTimestamp(Instant.now())
                        .build())
                        .queue(null, error -> plugin.getSLF4JLogger().warn("Could not send console command result to Discord", error));
            }
            catch (Exception e)
            {
                plugin.getSLF4JLogger().error("Failed to dispatch Discord console command '{}'", command, e);
                interaction.editOriginal("The server command failed to execute.")
                        .queue(null, error -> plugin.getSLF4JLogger().warn("Could not send console command failure to Discord", error));
            }
        }), error -> plugin.getSLF4JLogger().warn("Could not acknowledge Discord console command", error));
    }
}
