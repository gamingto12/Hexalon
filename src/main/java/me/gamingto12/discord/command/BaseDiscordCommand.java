package me.gamingto12.discord.command;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import me.gamingto12.Hexalon;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@Getter
public abstract class BaseDiscordCommand implements DiscordCommand
{

    private final Hexalon plugin;
    private final String name;
    private final String category;
    private final List<String> aliases;
    private final boolean adminOnly;

    public BaseDiscordCommand(Hexalon plugin, String name, String category, List<String> aliases, boolean adminOnly)
    {
        this.plugin = plugin;
        this.name = name;
        this.category = category;
        this.aliases = aliases;
        this.adminOnly = adminOnly;
    }

    @Override
    public boolean canExecute(User user, Guild guild)
    {
        if (user == null || guild == null)
            return false;

        if (!adminOnly)
            return true;

        Member member = guild.getMember(user);
        if (member == null)
            return false;

        List<String> adminRoleIds = plugin.getConfig().getStringList("discord.adminRoleIds");
        Set<String> allowedRoles = new HashSet<>();
        for (String roleId : adminRoleIds)
            if (roleId != null && !roleId.isBlank())
                allowedRoles.add(roleId.trim());

        if (allowedRoles.isEmpty())
            return false;

        return member.getRoles().stream()
                .map(Role::getId)
                .filter(Objects::nonNull)
                .anyMatch(allowedRoles::contains);
    }

    protected void sendEmbed(TextChannel channel, String title, String description, int color)
    {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now());

        if (channel != null) {
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    protected abstract List<String> getAdminRoleIds();

    @Override
    public abstract void execute(User user, Guild guild, TextChannel channel, List<String> args);
}

