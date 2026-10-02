package me.gamingto12.discord.command;

import java.util.List;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public interface DiscordSlashCommand
{
    String getName();

    String getDescription();

    default List<OptionData> getOptions() {
        return List.of();
    }

    default boolean isAdminOnly() {
        return false;
    }

    void execute(SlashCommandInteractionEvent event);

    default CommandData toCommandData() {
        return Commands.slash(getName(), getDescription()).addOptions(getOptions());
    }
}
