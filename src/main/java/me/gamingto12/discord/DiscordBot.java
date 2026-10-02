package me.gamingto12.discord;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import me.gamingto12.Hexalon;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordBot
{
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    @Getter
    private JDA bot;

    public void start(Object... listeners)
    {
        if (bot != null && bot.getStatus() != JDA.Status.SHUTDOWN)
        {
            stop();
        }

        String token = getToken();
        if (token == null || token.isBlank())
        {
            throw new IllegalStateException("Discord token is missing from config.yml");
        }

        JDABuilder builder = JDABuilder.createDefault(token,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT
        );

        builder.disableCache(
                CacheFlag.VOICE_STATE,
                CacheFlag.EMOJI,
                CacheFlag.STICKER,
                CacheFlag.SOUNDBOARD_SOUNDS,
                CacheFlag.SCHEDULED_EVENTS
        );

        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.enableCache(CacheFlag.ROLE_TAGS);
        builder.setActivity(Activity.watching(Objects.requireNonNullElse(
            Hexalon.getInstance().getConfig().getString("discord.activity"), "")));

        builder.addEventListeners(listeners);

        try
        {
            bot = builder.build();
        }
        catch (Exception e)
        {
            bot = null;
            Hexalon.getInstance().getSLF4JLogger().error(
                    "Failed to start the Discord bot; please verify the token and network configuration.", e);
            throw new IllegalStateException("Unable to start the Discord bot.", e);
        }
    }

    public void stop()
    {
        JDA currentBot = bot;
        if (currentBot == null)
            return;

        try
        {
            currentBot.shutdown();
            if (!currentBot.awaitShutdown(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            {
                Hexalon.getInstance().getSLF4JLogger().warn(
                        "JDA did not shut down gracefully; forcing shutdown.");
                currentBot.shutdownNow();
                if (!currentBot.awaitShutdown(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                {
                    Hexalon.getInstance().getSLF4JLogger().error(
                            "JDA did not stop within the shutdown timeout; continuing plugin shutdown.");
                }
            }
        }
        catch (InterruptedException e)
        {
            currentBot.shutdownNow();
            Thread.currentThread().interrupt();
            Hexalon.getInstance().getSLF4JLogger().warn(
                    "Interrupted while waiting for JDA to stop; forced shutdown was requested.", e);
        }
        catch (Exception e)
        {
            Hexalon.getInstance().getSLF4JLogger().warn(
                    "Unexpected error while shutting down JDA.", e);
        }
        finally
        {
            bot = null;
        }
    }

    public Guild getMainGuild()
    {
        if (bot == null) return null;
        return bot.getGuildById(getGuildID());
    }

    public TextChannel getChatChannel()
    {
        return getTextChannel(getChatChannelID());
    }

    public TextChannel getTextChannel(String channelId)
    {
        if (bot == null || channelId == null || channelId.isBlank()) return null;
        return bot.getTextChannelById(channelId.trim());
    }

    private String getToken()
    {
        return Hexalon.getInstance().getConfig().getString("discord.token", "");
    }

    private String getGuildID()
    {
        return Hexalon.getInstance().getConfig().getString("discord.guild-id", "");
    }

    private String getChatChannelID()
    {
        Object channelId = Hexalon.getInstance().getConfig().get("discord.channels.chat-id");
        return channelId == null ? "" : String.valueOf(channelId).trim();
    }
}
