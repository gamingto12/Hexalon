package me.gamingto12.discord.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;

public interface DiscordCommand
{

    /**
     * Checks if the user can execute this command
     */
    boolean canExecute(User user, Guild guild);

    /**
     * Executes the command
     */
    void execute(User user, Guild guild, TextChannel channel, List<String> args);

    /**
     * The main command name
     */
    String getName();

    /**
     * Aliases
     */
    List<String> getAliases();

    /**
     * Command category
     */
    String getCategory();
}
