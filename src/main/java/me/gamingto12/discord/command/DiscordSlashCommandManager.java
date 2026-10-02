package me.gamingto12.discord.command;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.gamingto12.Hexalon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jspecify.annotations.NonNull;

public class DiscordSlashCommandManager extends ListenerAdapter
{
    private final Hexalon plugin;
    private final Map<String, DiscordSlashCommand> commands = new HashMap<>();

    public DiscordSlashCommandManager(Hexalon plugin)
    {
        this.plugin = plugin;
    }

    public void registerCommand(DiscordSlashCommand command)
    {
        if (command == null)
        {
            plugin.getSLF4JLogger().warn("Attempted to register null slash command.");
            return;
        }

        commands.put(command.getName().trim().toLowerCase(Locale.ROOT), command);
    }

    public void registerCommands()
    {
        if (plugin.getDiscordBot() == null || plugin.getDiscordBot().getBot() == null || commands.isEmpty())
            return;

        List<CommandData> commandData = commands.values().stream()
                .map(DiscordSlashCommand::toCommandData)
                .toList();

        String guildId = plugin.getConfig().getString("discord.guild-id", "").trim();
        Guild guild = guildId.isEmpty() ? null : plugin.getDiscordBot().getBot().getGuildById(guildId);
        if (!guildId.isEmpty() && guild == null)
        {
            plugin.getSLF4JLogger().error("Could not register slash commands: configured Discord guild {} is unavailable.", guildId);
            return;
        }

        var updateAction = guild == null
            ? plugin.getDiscordBot().getBot().updateCommands()
            : guild.updateCommands();
        updateAction.addCommands(commandData).queue(
            success -> plugin.getSLF4JLogger().info("Registered {} slash command(s) {}.", commandData.size(),
                guild == null ? "globally" : "in guild " + guild.getId()),
            error -> plugin.getSLF4JLogger().error("Could not register slash commands.", error)
        );
        }

        @Override
        public void onReady(@NonNull ReadyEvent event)
        {
        registerCommands();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        DiscordSlashCommand command = commands.get(event.getName().trim().toLowerCase(Locale.ROOT));
        if (command == null)
            return;

        if (command.isAdminOnly() && !hasAdminAccess(event.getMember(), event.getGuild()))
        {
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Permission denied")
                    .setDescription("You do not have permission to use this command.")
                    .setColor(0xD64545)
                    .setTimestamp(Instant.now())
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try
        {
            command.execute(event);
        }
        catch (Exception e)
        {
            plugin.getSLF4JLogger().error("Error while executing slash command '{}'", command.getName(), e);
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Command failed")
                    .setDescription("An unexpected error occurred while running that command.")
                    .setColor(0xD64545)
                    .setTimestamp(Instant.now())
                    .build())
                    .setEphemeral(true)
                    .queue();
        }
    }

    private boolean hasAdminAccess(Member member, Guild guild)
    {
        if (member == null || guild == null)
            return false;

        List<String> adminRoleIds = plugin.getConfig().getStringList("discord.adminRoleIds");
        if (adminRoleIds == null || adminRoleIds.isEmpty())
            return false;

        return member.getRoles().stream()
                .map(Role::getId)
                .anyMatch(adminRoleIds::contains);
    }
}
