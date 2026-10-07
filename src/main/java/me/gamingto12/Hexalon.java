package me.gamingto12;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.gamingto12.discord.ConsoleLogger;
import me.gamingto12.discord.DiscordBot;
import me.gamingto12.discord.command.DiscordSlashCommandManager;
import me.gamingto12.discord.commands.AdminConsoleSlashCommand;
import me.gamingto12.discord.commands.ListSlashCommand;
import me.gamingto12.discord.commands.TPSSlashCommand;
import me.gamingto12.discord.commands.UptimeSlashCommand;
import me.gamingto12.discord.listener.ChatListener;
import me.gamingto12.discord.listener.MessageListener;
import me.gamingto12.minecraft.command.HexalonCMD;
import me.gamingto12.minecraft.utilities.BuildProperties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

@Getter
public class Hexalon extends JavaPlugin
{
    @Getter
    private static Hexalon instance;
    private BuildProperties buildMeta;

    private DiscordSlashCommandManager slashCommandManager;
    private ConsoleLogger consoleLogger;
    private DiscordBot discordBot;
    public ChatListener discordChatListener;
    public MessageListener messageListener;
    private final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public Component mmDeserialize(String message, TagResolver... placeholders)
    {
        return MINI_MESSAGE.deserialize(message, placeholders);
    }

    @Override
    public void onLoad()
    {
        instance = this;
        buildMeta = new BuildProperties();
    }

    @Override
    public void onEnable()
    {
        try
        {
            getConfig().options().copyDefaults(true);
            saveConfig();

            initializeCoreServices();
            registerBukkitCommands();
            loadListeners();
            getServer().getPluginManager().registerEvents(discordChatListener, this);
            startDiscordBridgeIfConfigured();
            new Metrics(this, 34451);
            getSLF4JLogger().info("Enabled Metrics");
            getSLF4JLogger().info("Hexalon enabled successfully.");
        }
        catch (Exception e)
        {
            getSLF4JLogger().error("Failed to enable Hexalon", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable()
    {
        getSLF4JLogger().info("Disabling Hexalon");

        if (consoleLogger != null)
            consoleLogger.stop();

        if (discordBot != null && discordBot.getBot() != null)
        {
            sendDiscordStatusMessage();
            discordBot.stop();
        }
    }

    private void initializeCoreServices()
    {
        this.discordBot = new DiscordBot();
        this.consoleLogger = new ConsoleLogger(this, discordBot);
        this.consoleLogger.start();

        this.slashCommandManager = new DiscordSlashCommandManager(this);
        registerDiscordSlashCommands();
    }

    private void registerBukkitCommands()
    {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("hexalon", "Hexalon plugin commands", new HexalonCMD()));
    }

    private void startDiscordBridgeIfConfigured()
    {
        String token = getConfig().getString("discord.token");
        if (token == null || token.isBlank())
        {
            getSLF4JLogger().warn("Discord token is not configured; bridge startup skipped.");
            return;
        }

        try
        {
            discordBot.start(messageListener, slashCommandManager);
        }
        catch (Exception e)
        {
            getSLF4JLogger().error("Could not start the Discord bridge", e);
        }
    }

    private void sendDiscordStatusMessage()
    {
        if (discordBot == null || discordBot.getBot() == null)
            return;

        var channel = discordBot.getChatChannel();
        if (channel == null)
            return;

        try
        {
            channel.sendMessage("**Server has stopped**").submit().get(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            getSLF4JLogger().warn("Interrupted while sending a Discord status message", e);
        }
        catch (ExecutionException | TimeoutException e)
        {
            getSLF4JLogger().warn("Could not send a Discord status message", e);
        }
    }

    public void loadListeners()
    {
        discordChatListener = new ChatListener();
        messageListener = new MessageListener(this);
    }

    public void registerDiscordSlashCommands()
    {
        if (slashCommandManager == null)
            return;

        slashCommandManager.registerCommand(new AdminConsoleSlashCommand(this));
        slashCommandManager.registerCommand(new ListSlashCommand());
        slashCommandManager.registerCommand(new TPSSlashCommand());
        slashCommandManager.registerCommand(new UptimeSlashCommand(System.currentTimeMillis()));
    }
}
