package me.gamingto12.discord.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.gamingto12.Hexalon;
import me.gamingto12.discord.command.DiscordCommandManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;

public class MessageListener extends ListenerAdapter
{

    private static final Pattern MEDIA_URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASK_IP_PATTERN = Pattern.compile(
            "(((give *(me)?)|(wh?at'? *i?s?))( *the)?( *server)? *ip)|(ip\\?)",
            Pattern.CASE_INSENSITIVE
    );

    private final DiscordCommandManager manager;
    private final Hexalon plugin;

    public MessageListener(DiscordCommandManager manager, Hexalon plugin)
    {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Override
    public void onReady(@NonNull ReadyEvent event)
    {
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
        String prefix = plugin.getConfig().getString("discord.prefix", "!");
        String channelId = event.getChannel().getId();
        String chatChannelId = plugin.getConfig().getString("discord.channels.chat-id", "").trim();
        String consoleChannelId = plugin.getConfig().getString("discord.channels.console-id", "").trim();

        if (handleConsoleMessage(event, content, prefix, channelId, consoleChannelId)) return;
        if (handleCommand(event, content, prefix, channelId, chatChannelId, consoleChannelId)) return;
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

    private boolean handleConsoleMessage(MessageReceivedEvent event, String content, String prefix,
                                         String channelId, String consoleChannelId)
    {
        if (!channelId.equals(consoleChannelId) || content.isBlank() || content.startsWith(prefix)) return false;

        manager.handleMessage(event.getAuthor(), event.getGuild(), event.getChannel().asTextChannel(),
                prefix + "console " + content);
        return true;
    }

    private boolean handleCommand(MessageReceivedEvent event, String content, String prefix, String channelId,
                                  String chatChannelId, String consoleChannelId)
    {
        if (!content.startsWith(prefix)) return false;
        if (!channelId.equals(chatChannelId) && !channelId.equals(consoleChannelId)) return true;

        manager.handleMessage(event.getAuthor(), event.getGuild(), event.getChannel().asTextChannel(), content);
        return true;
    }

    private void broadcastChatMessage(MessageReceivedEvent event, String content)
    {
        var attachments = event.getMessage().getAttachments();
        if (content.isBlank() && attachments.isEmpty()) return;

        var member = event.getMember();
        String author = member == null ? event.getAuthor().getName() : member.getEffectiveName();

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

