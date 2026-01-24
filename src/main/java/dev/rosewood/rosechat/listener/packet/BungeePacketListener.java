package dev.rosewood.rosechat.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import dev.rosewood.rosechat.RoseChat;
import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.message.DeletableMessage;
import dev.rosewood.rosechat.message.RosePlayer;
import dev.rosewood.rosechat.message.contents.MessageContents;
import dev.rosewood.rosechat.message.tokenizer.composer.ChatComposer;
import dev.rosewood.rosechat.placeholder.DefaultPlaceholders;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import org.bukkit.Bukkit;

public class BungeePacketListener extends PacketListener<BaseComponent[]> {

    public BungeePacketListener(RoseChat roseChat) {
        super(roseChat);
    }

    @Override
    public String getDeletableMessageJson(RosePlayer player, UUID messageId, String messageJson) {
        BaseComponent[] withClientButton = BungeePacketListener.appendClientDeleteButton(player, player.getPlayerData(), messageId, messageJson);
        if (withClientButton == null)
            return null;

        return ChatComposer.json().composeBungee(withClientButton);
    }

    @Override
    protected String getMessageReflectively(PacketContainer packet) {
        if (packet.getType() == PacketType.Play.Server.CHAT) {
            try {
                if (componentsArrayField == null) {
                    try {
                        componentsArrayField = packet.getHandle().getClass().getDeclaredField("components");
                        componentsArrayField.setAccessible(true);
                    } catch (ReflectiveOperationException ignored) {
                        return null;
                    }
                }

                return ChatComposer.json().composeBungee((BaseComponent[]) componentsArrayField.get(packet.getHandle()));
            } catch (Exception ignored) {}

            return null;
        }

        if (packet.getType() == PacketType.Play.Server.SYSTEM_CHAT) {
            try {
                if (contentField == null) {
                    try {
                        contentField = packet.getHandle().getClass().getDeclaredField("content");
                        contentField.setAccessible(true);
                    } catch (ReflectiveOperationException ignored) {}
                }

                String value = (String) contentField.get(packet.getHandle());
                if (value != null)
                    return value;
            } catch (Exception ignored) {}

            return null;
        }

        return null;
    }

    @Override
    protected String getServerMessageJson(DeletableMessage message, RosePlayer viewer) {
        BaseComponent[] components = ChatComposer.decorated().composeJson(message.getJson());
        if (components == null || viewer.isConsole())
            return null;

        boolean isSamePlayer = message.getSender() != null && message.getSender().equals(viewer.getUUID());
        String permission = isSamePlayer ? "rosechat.deletemessages.self" : "rosechat.deletemessages.others";
        String format = isSamePlayer ? Settings.DELETE_OWN_MESSAGE_FORMAT.get() :  Settings.DELETE_OTHER_MESSAGE_FORMAT.get();

        if (!viewer.hasPermission(permission))
            return null;

        BaseComponent[] appended = this.appendDeleteButton(components, message, viewer, format);
        return ChatComposer.json().composeBungee(appended);
    }

    @Override
    protected BaseComponent[] appendDeleteButton(BaseComponent[] component, DeletableMessage message, RosePlayer viewer, String placeholder) {
        BaseComponent[] button = this.getDeleteButton(message, viewer, placeholder);

        ComponentBuilder builder = new ComponentBuilder();
        if (Settings.DELETE_MESSAGE_SUFFIX.get()) {
            builder.append(component, FormatRetention.NONE);

            if (button != null)
                builder.append(button, FormatRetention.NONE);
        } else {
            if (button != null)
                builder.append(button, FormatRetention.NONE);

            builder.append(component, FormatRetention.NONE);
        }

        return builder.create();
    }

    @Override
    protected BaseComponent[] getDeleteButton(DeletableMessage message, RosePlayer viewer, String placeholder) {
        RosePlayer sender = new RosePlayer(message.getSender() == null ? Bukkit.getConsoleSender() : Bukkit.getPlayer(message.getSender()));

        return RoseChatAPI.getInstance().parse(new RosePlayer(Bukkit.getConsoleSender()), viewer, placeholder,
                DefaultPlaceholders.getFor(sender, viewer)
                        .add("id", message.getUUID().toString())
                        .add("type", "server")
                        .add("channel", message.getChannel())
                        .build()).build(ChatComposer.decorated());
    }

    public static BaseComponent[] appendClientDeleteButton(RosePlayer sender, PlayerData playerData, UUID messageId, String messageJson) {
        ComponentBuilder builder = new ComponentBuilder();
        String placeholder = Settings.DELETE_CLIENT_MESSAGE_FORMAT.get();

        MessageContents results = RoseChatAPI.getInstance().parse(new RosePlayer(Bukkit.getConsoleSender()), sender, placeholder,
                DefaultPlaceholders.getFor(sender, sender)
                        .add("id", messageId.toString())
                        .add("type", "client").build());

        BaseComponent[] deleteClientButton = results.build(ChatComposer.decorated());
        if (deleteClientButton == null) {
            playerData.getMessageLog().addDeletableMessage(new DeletableMessage(UUID.randomUUID(), messageJson, true));
            return null;
        }

        if (Settings.DELETE_MESSAGE_SUFFIX.get()) {
            builder.append(ChatComposer.decorated().composeJson(messageJson), ComponentBuilder.FormatRetention.NONE)
                    .append(deleteClientButton, ComponentBuilder.FormatRetention.NONE);
        } else {
            builder.append(deleteClientButton, ComponentBuilder.FormatRetention.NONE)
                    .append(ChatComposer.decorated().composeJson(messageJson), ComponentBuilder.FormatRetention.NONE);
        }

        return builder.create();
    }

}
