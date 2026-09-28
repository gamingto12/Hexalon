package me.gamingto12.discord.command;

import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

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

    default void execute(User user, Guild guild, TextChannel channel, List<String> args,
                         boolean fromConsoleChannel)
    {
        execute(user, guild, channel, args);
    }

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
