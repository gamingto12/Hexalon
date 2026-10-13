package me.gamingto12.discord.commands;

import java.time.Instant;
import java.util.List;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.ConsoleCommandService;
import me.gamingto12.discord.command.DiscordSlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AdminConsoleSlashCommand implements DiscordSlashCommand
{
    private final Hexalon plugin;
    private final ConsoleCommandService consoleCommands;

    public AdminConsoleSlashCommand(Hexalon plugin)
    {
        this.plugin = plugin;
        this.consoleCommands = new ConsoleCommandService(plugin);
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
        return List.of(new OptionData(OptionType.STRING, "command", "The command to run on the server.", true));
    }

    @Override
    public boolean isAdminOnly()
    {
        return true;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
        String rawCommand = event.getOption("command", "", OptionMapping::getAsString);
        ConsoleCommandService.Validation validation = consoleCommands.validate(rawCommand);
        if (!validation.isAccepted())
        {
            sendRejection(event, validation.rejection());
            return;
        }

        String command = validation.command();

        plugin.getSLF4JLogger().info("Discord console slash command by {} ({}): {}",
                event.getUser().getName(), event.getUser().getId(), command);

        event.deferReply(true).queue(interaction -> consoleCommands.dispatch(command, result ->
        {
            if (result.hasError())
            {
                plugin.getSLF4JLogger().error("Failed to dispatch Discord console command '{}'", command, result.error());
                interaction.editOriginal("The server command failed to execute.")
                        .queue(null, error -> plugin.getSLF4JLogger().warn("Could not send console command failure to Discord", error));
            }
            else
            {
                interaction.editOriginalEmbeds(new EmbedBuilder()
                        .setTitle(result.accepted() ? "Command dispatched" : "Command rejected")
                        .setDescription("`" + command + "`")
                        .setColor(result.accepted() ? 0x22C55E : 0xD64545)
                        .setTimestamp(Instant.now())
                        .build())
                        .queue(null, error -> plugin.getSLF4JLogger().warn("Could not send console command result to Discord", error));
            }
        }), error -> plugin.getSLF4JLogger().warn("Could not acknowledge Discord console command", error));
    }

    private void sendRejection(SlashCommandInteractionEvent event, ConsoleCommandService.Rejection rejection)
    {
        String title = switch (rejection)
        {
            case DISABLED -> "Console bridge disabled";
            case MISSING -> "Missing command";
            case BLOCKED -> "Blocked command";
        };
        String description = switch (rejection)
        {
            case DISABLED -> "Remote console commands are disabled.";
            case MISSING -> "You must provide a command to execute.";
            case BLOCKED -> "That command is not allowed through the Discord console bridge.";
        };
        int color = rejection == ConsoleCommandService.Rejection.DISABLED ? 0xF59E0B : 0xD64545;
        event.replyEmbeds(new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now())
                .build()).setEphemeral(true).queue();
    }
}
