package me.gamingto12.discord.listener;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.bukkit.GameRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.gamingto12.Hexalon;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatListener implements Listener
{
    private final Hexalon plugin;
    private volatile List<String> blockedTerms;

    public ChatListener()
    {
        plugin = Hexalon.getInstance();
        reloadBlockedTerms();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event)
    {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (isBlocked(message))
        {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    event.getPlayer().sendMessage(plugin.mmDeserialize("<red>Your message was blocked by the server's chat filter.")));
            return;
        }
        sendToDiscord("**" + escapeDiscord(event.getPlayer().getName()) + "** » " + escapeDiscord(message));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        String status = event.getPlayer().hasPlayedBefore() ? " joined the server" : " joined for the first time";
        sendToDiscord("**" + escapeDiscord(event.getPlayer().getName() + status) + "**");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        sendToDiscord("**" + escapeDiscord(event.getPlayer().getName() + " left the server") + "**");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        Boolean showDeathMessages = event.getEntity().getWorld().getGameRuleValue(GameRules.SHOW_DEATH_MESSAGES);
        if (!showDeathMessages || event.deathMessage() == null) return;

        String deathMessage = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());
        sendToDiscord("[Death] " + escapeDiscord(deathMessage));
    }

    public void broadcastDiscordMessage(String author, Component message)
    {
        var component = plugin.mmDeserialize(
                plugin.getConfig().getString("discord.format"),
                Placeholder.unparsed("author", author),
                Placeholder.component("message", message));
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().broadcast(component));
    }

    private void sendToDiscord(String message)
    {
        TextChannel channel = plugin.getDiscordBot().getChatChannel();
        if (channel != null) {
            channel.sendMessage(message).queue(null, error ->
                    plugin.getSLF4JLogger().warn("Failed to send an in-game message to Discord", error));
        }
    }

    private boolean isBlocked(String message)
    {
        String normalizedMessage = normalize(message);
        return blockedTerms.stream().anyMatch(normalizedMessage::contains);
    }

    public final void reloadBlockedTerms()
    {
        blockedTerms = plugin.getConfig().getStringList("chat.blocked-terms").stream()
                .filter(term -> !term.isBlank())
                .map(ChatListener::normalize)
                .toList();
    }

    private static String normalize(String text)
    {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private String escapeDiscord(String text)
    {
        return text.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("|", "\\|")
                .replace("`", "\\`");
    }
}
