package me.gamingto12.discord.listener;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.entities.Role;
import org.jspecify.annotations.NonNull;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.ConsoleCommandService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MessageListener extends ListenerAdapter
{

    private static final Pattern MEDIA_URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASK_IP_PATTERN = Pattern.compile(
            "(((give *(me)?)|(wh?at'? *i?s?))( *the)?( *server)? *ip)|(ip\\?)",
            Pattern.CASE_INSENSITIVE
    );

    private final Hexalon plugin;
    private final ConsoleCommandService consoleCommands;

    public MessageListener(Hexalon plugin)
    {
        this.plugin = plugin;
        this.consoleCommands = new ConsoleCommandService(plugin);
    }

    @Override
    public void onReady(@NonNull ReadyEvent event)
    {
        plugin.getConsoleLogger().start();
        var channel = plugin.getDiscordBot().getChatChannel();
        if (channel != null)
        {
            channel.sendMessage("**Server has started**").queue(null, error ->
                    plugin.getSLF4JLogger().warn("Could not send the server-started message to Discord", error));
        }
        else
        {
            plugin.getSLF4JLogger().warn("Discord connected, but chat channel {} could not be resolved as a text channel.",
                    plugin.getConfig().getString("discord.channels.chat-id", ""));
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event)
    {
        if (!isEligibleMessage(event)) return;

        String content = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();
        String chatChannelId = plugin.getConfig().getString("discord.channels.chat-id", "").trim();
        String consoleChannelId = plugin.getConfig().getString("discord.channels.console-id", "").trim();

        if (handleConsoleMessage(event, content, channelId, consoleChannelId)) return;
        if (!channelId.equals(chatChannelId)) return;

        broadcastChatMessage(event, content);
        respondToIpRequest(event, content);
    }

    private boolean isEligibleMessage(MessageReceivedEvent event)
    {
        return !event.getAuthor().isBot()
                && event.isFromGuild()
                && event.getGuild().getId().equals(plugin.getConfig().getString("discord.guild-id", ""));
    }

    private boolean handleConsoleMessage(MessageReceivedEvent event, String content,
                                         String channelId, String consoleChannelId)
    {
        if (!channelId.equals(consoleChannelId) || content.isBlank()) return false;

        ConsoleCommandService.Validation validation = consoleCommands.validate(content);
        if (!validation.isAccepted())
        {
            sendConsoleRejection(event, validation.rejection());
            return true;
        }

        String command = validation.command();
        consoleCommands.dispatch(command, result ->
        {
            if (result.hasError())
            {
                plugin.getSLF4JLogger().error("Failed to dispatch Discord console command '{}'", command, result.error());
                event.getChannel().sendMessage("The server command failed to execute.")
                        .queue(null, error -> plugin.getSLF4JLogger().warn("Could not send console command failure to Discord", error));
            }
            else
            {
                if (!result.accepted())
                    event.getChannel().sendMessage("The server rejected that command.")
                            .queue(null, error -> plugin.getSLF4JLogger().warn("Could not send console command result to Discord", error));
            }
        });
        return true;
    }

    private void sendConsoleRejection(MessageReceivedEvent event, ConsoleCommandService.Rejection rejection)
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
        event.getChannel().asTextChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .build()).queue();
    }

    private void broadcastChatMessage(MessageReceivedEvent event, String content)
    {
        var attachments = event.getMessage().getAttachments();
        if (content.isBlank() && attachments.isEmpty()) return;

        var member = event.getMember();
        String author = member == null ? event.getAuthor().getName() : member.getEffectiveName();
        if (member != null)
        {
            var ignoredRoles = plugin.getConfig().getStringList("discord.chat-role-ignore-list").stream()
                .map(role -> role.toLowerCase(Locale.ROOT))
                .toList();
            var topRole = member.getRoles().stream()
                .filter(role -> !role.isPublicRole())
                .filter(role -> !ignoredRoles.contains(role.getName().toLowerCase(Locale.ROOT))
                    && !ignoredRoles.contains(role.getId()))
                .max(Comparator.comparingInt(Role::getPosition));
            if (topRole.isPresent())
                author = "[" + topRole.get().getName() + "] " + author;
        }

        Component message = replaceMediaUrls(content);
        boolean hasContent = !content.isBlank();
        for (var attachment : attachments)
        {
            if (hasContent) message = message.append(Component.space());
            message = message.append(Component.text("[Media]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.openUrl(attachment.getUrl())));
            hasContent = true;
        }

        plugin.getDiscordChatListener().broadcastDiscordMessage(author, message);
    }

    private Component replaceMediaUrls(String content)
    {
        Matcher matcher = MEDIA_URL_PATTERN.matcher(content);
        Component message = Component.empty();
        int copiedUntil = 0;

        while (matcher.find())
        {
            int linkEnd = matcher.end();
            while (linkEnd > matcher.start() && isTrailingUrlPunctuation(content.charAt(linkEnd - 1)))
            {
                linkEnd--;
            }
            if (linkEnd == matcher.start()) continue;

            message = message.append(Component.text(content.substring(copiedUntil, matcher.start())))
                    .append(Component.text("[Media]", NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.openUrl(content.substring(matcher.start(), linkEnd))));
            copiedUntil = linkEnd;
        }

        return message.append(Component.text(content.substring(copiedUntil)));
    }

    private boolean isTrailingUrlPunctuation(char character)
    {
        return ".,!?;:)".indexOf(character) >= 0;
    }

    private void respondToIpRequest(MessageReceivedEvent event, String content)
    {
        if (ASK_IP_PATTERN.matcher(content).find())
        {
            // send embed like python version
            EmbedBuilder em = new EmbedBuilder()
                    .setTitle("Server IP")
                    .setColor(0xA84300)
                    .setDescription(plugin.getConfig().getString("server.address"));

            event.getChannel().asTextChannel()
                    .sendMessageEmbeds(em.build())
                    .queue();
        }
    }
}

