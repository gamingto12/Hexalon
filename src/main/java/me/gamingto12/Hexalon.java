package me.gamingto12;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.gamingto12.discord.ConsoleLogger;
import me.gamingto12.discord.DiscordBot;
import me.gamingto12.discord.command.DiscordCommandManager;
import me.gamingto12.discord.commands.AdminConsoleCommand;
import me.gamingto12.discord.commands.HelpCommand;
import me.gamingto12.discord.commands.ListCommand;
import me.gamingto12.discord.commands.TPSCommand;
import me.gamingto12.discord.commands.UptimeCommand;
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

    private DiscordCommandManager manager;
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
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.discordBot = new DiscordBot();
        this.consoleLogger = new ConsoleLogger(this, discordBot);
        this.consoleLogger.start();
        getSLF4JLogger().info("Enabled Hexalon");

        // Initialize command manager first
        this.manager = new DiscordCommandManager(this);
        registerDiscordCommands();

        registerMinecraftCommands();

        // Then load listeners (so manager is not null)
        loadListeners();

        // Register for Bukkit events
        getServer().getPluginManager().registerEvents(discordChatListener, this);

        String token = getConfig().getString("discord.token");
        if (token == null || token.isBlank())
        {
            getSLF4JLogger().warn("Discord token is not configured; bridge startup skipped.");
        }
        else
        {
            try
            {
                discordBot.start(messageListener);
            }
            catch (Exception e)
            {
                getSLF4JLogger().error("Could not start the Discord bridge", e);
            }
        }
    }



    @Override
    public void onDisable()
    {
        getSLF4JLogger().info("Disabled Hexalon");

        if (consoleLogger != null) {
            consoleLogger.stop();
        }

        if (discordBot != null && discordBot.getBot() != null) {
            var channel = discordBot.getChatChannel();
            if (channel != null) {
                try {
                    channel.sendMessage("**Server has stopped**").submit().get(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    getSLF4JLogger().warn("Interrupted while sending the server-stopped message to Discord", e);
                } catch (ExecutionException | TimeoutException e) {
                    getSLF4JLogger().warn("Could not send the server-stopped message to Discord", e);
                }
            }
            discordBot.stop();
        }
    }

    public void loadListeners()
    {
        discordChatListener = new ChatListener();
        messageListener = new MessageListener(manager, this);
    }

    public void registerDiscordCommands()
    {
        manager.registerCommand(new AdminConsoleCommand(this));
        manager.registerCommand(new TPSCommand(this));
        manager.registerCommand(new HelpCommand(this, manager));
        manager.registerCommand(new UptimeCommand(this, System.currentTimeMillis()));
        manager.registerCommand(new ListCommand(this));
    }

    public void registerMinecraftCommands()
    {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register("hexalon", "Hexalon plugin commands", new HexalonCMD()));
    }
}
