package me.gamingto12.minecraft.command;

import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gamingto12.Hexalon;
import me.gamingto12.minecraft.utilities.BuildProperties;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jspecify.annotations.NonNull;

public class HexalonCMD implements BasicCommand
{
    private final Hexalon plugin = Hexalon.getInstance();

    @Override
    public void execute(CommandSourceStack source, String[] args)
    {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            BuildProperties build = Hexalon.getInstance().getBuildMeta();
            sender.sendMessage(plugin.mmDeserialize("<gradient:#38d6c8:#8eb8ff><bold>Hexalon</bold></gradient> <dark_gray>•</dark_gray> <gray>Discord bridge"));
            sender.sendMessage(plugin.mmDeserialize("<gray>Version <white>" + build.getVersion() + "." + build.getNumber()));
            sender.sendMessage(plugin.mmDeserialize("<gray>Built <white>" + build.getDate() + " <dark_gray>by</dark_gray> <white>" + build.getAuthor()));
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT))
        {
            case "help" -> sendHelp(sender);
            case "invite" -> sender.sendMessage(plugin.mmDeserialize(
                    "<gray>Discord invite <dark_gray>» <white><invite></white>",
                    Placeholder.unparsed(
                            "invite", Hexalon.getInstance().getConfig().getString("discord.invite", "Not configured"))));
            case "reload" -> reload(sender);
            case "broadcast", "bcast", "bc" -> broadcast(sender, args);
            default -> sender.sendMessage(plugin.mmDeserialize("<red>Unknown subcommand. Use <white>/kooljda help</white>."));
        }
    }

    @Override
    public @NonNull List<String> suggest(@NonNull CommandSourceStack source, String[] args)
    {

        if (args.length != 1) return List.of();
        List<String> options = source.getSender().hasPermission("hexalon.admin")
                ? List.of("help", "invite", "reload", "broadcast")
                : List.of("help", "invite");
        String input = args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(input)).toList();
    }

    private void sendHelp(CommandSender sender)
    {
        sender.sendMessage(plugin.mmDeserialize("<gradient:#38d6c8:#8eb8ff><bold>Hexalon commands</bold></gradient>"));
        sender.sendMessage(plugin.mmDeserialize("<gray>/hexalon <dark_gray>»</dark_gray> <white>Show plugin information"));
        sender.sendMessage(plugin.mmDeserialize("<gray>/hexalon invite <dark_gray>»</dark_gray> <white>Show the Discord invite"));
        if (sender.hasPermission("hexalon.admin"))
        {
            sender.sendMessage(plugin.mmDeserialize("<gray>/hexalon reload <dark_gray>»</dark_gray> <white>Reload configuration"));
            sender.sendMessage(plugin.mmDeserialize("<gray>/hexalon broadcast <message> <dark_gray>»</dark_gray> <white>Send a message to Discord"));
        }
    }

    private void reload(CommandSender sender)
    {
        if (!sender.hasPermission("hexalon.admin"))
        {
            sender.sendMessage(plugin.mmDeserialize("<red>You do not have permission to execute this command."));
            return;
        }
        Hexalon.getInstance().reloadConfig();
        Hexalon.getInstance().getDiscordChatListener().reloadBlockedTerms();
        sender.sendMessage(plugin.mmDeserialize("<green>Configuration reloaded. <gray>Restart to apply Discord connection changes."));
    }

    private void broadcast(CommandSender sender, String[] args)
    {
        if (!sender.hasPermission("hexalon.admin")) {

            sender.sendMessage(plugin.mmDeserialize("<red>You do not have permission to execute this command."));
            return;
        }
        if (args.length < 2)
        {
            sender.sendMessage(plugin.mmDeserialize("<red>Usage: <white>/hexalon broadcast <message>"));
            return;
        }
        TextChannel channel = Hexalon.getInstance().getDiscordBot().getChatChannel();
        if (channel == null)
        {
            sender.sendMessage(plugin.mmDeserialize("<red>Discord chat channel is unavailable."));
            return;
        }
        channel.sendMessage(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))).queue();
        sender.sendMessage(plugin.mmDeserialize("<green>Message sent to Discord."));
    }
}