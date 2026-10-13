package me.gamingto12.discord.command;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import me.gamingto12.Hexalon;

public final class ConsoleCommandService
{
    private final Hexalon plugin;

    public ConsoleCommandService(Hexalon plugin)
    {
        this.plugin = plugin;
    }

    public Validation validate(String rawCommand)
    {
        String command = rawCommand == null ? "" : rawCommand.trim();
        if (!plugin.getConfig().getBoolean("discord.console.enabled", false))
            return new Validation(command, Rejection.DISABLED);
        if (command.isBlank())
            return new Validation(command, Rejection.MISSING);

        int rootEnd = 0;
        while (rootEnd < command.length() && !Character.isWhitespace(command.charAt(rootEnd)))
            rootEnd++;
        String root = command.substring(0, rootEnd).toLowerCase(Locale.ROOT);
        List<String> blockedCommands = plugin.getConfig().getStringList("discord.console.disallowed-commands");
        if (blockedCommands.stream().anyMatch(blocked -> blocked.equalsIgnoreCase(root)))
            return new Validation(command, Rejection.BLOCKED);

        return new Validation(command, null);
    }

    public void dispatch(String command, Consumer<DispatchResult> completion)
    {
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            DispatchResult result;
            try
            {
                result = new DispatchResult(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), null);
            }
            catch (Exception error)
            {
                result = new DispatchResult(false, error);
            }
            completion.accept(result);
        });
    }

    public enum Rejection
    {
        DISABLED,
        MISSING,
        BLOCKED
    }

    public record Validation(String command, Rejection rejection)
    {
        public boolean isAccepted()
        {
            return rejection == null;
        }
    }

    public record DispatchResult(boolean accepted, Exception error)
    {
        public boolean hasError()
        {
            return error != null;
        }
    }
}