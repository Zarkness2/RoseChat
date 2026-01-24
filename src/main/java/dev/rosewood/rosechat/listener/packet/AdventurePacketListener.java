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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;

public class AdventurePacketListener extends PacketListener<Component> {

    public AdventurePacketListener(RoseChat roseChat) {
        super(roseChat);
    }

    @Override
    public String getDeletableMessageJson(RosePlayer player, UUID messageId, String messageJson) {
        Component deleteClientButton = AdventurePacketListener.appendClientDeleteButton(player, player.getPlayerData(), messageId, messageJson);
        if (deleteClientButton == null)
            return null;

        return ChatComposer.json().composeAdventure().compose(deleteClientButton);
    }

    @Override
    protected String getMessageReflectively(PacketContainer packet) {
        if (packet.getType() == PacketType.Play.Server.CHAT) {
            try {
                if (adventureMessageField == null) {
                    try {
                        adventureMessageField = packet.getHandle().getClass().getDeclaredField("adventure$message");
                        adventureMessageField.setAccessible(true);
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                }

                GsonComponentSerializer serializer = GsonComponentSerializer.builder().build();
                return serializer.serialize((Component) adventureMessageField.get(packet.getHandle()));
            } catch (Exception ignored) {}

            return null;
        }

        if (packet.getType() == PacketType.Play.Server.SYSTEM_CHAT) {
            try {
                if (adventureContentField == null) {
                    try {
                        adventureContentField = packet.getHandle().getClass().getDeclaredField("adventure$content");
                        adventureContentField.setAccessible(true);
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                }

                GsonComponentSerializer serializer = GsonComponentSerializer.builder().build();
                return serializer.serialize((Component) adventureContentField.get(packet.getHandle()));
            } catch (Exception ignored) {}

            return null;
        }

        return null;
    }

    @Override
    protected String getServerMessageJson(DeletableMessage message, RosePlayer viewer) {
        Component component = ChatComposer.adventure().decorated().composeJson(message.getJson());
        if (component == null || viewer.isConsole())
            return null;

        boolean isSamePlayer = message.getSender() != null && message.getSender().equals(viewer.getUUID());
        String permission = isSamePlayer ? "rosechat.deletemessages.self" : "rosechat.deletemessages.others";
        String format = isSamePlayer ? Settings.DELETE_OWN_MESSAGE_FORMAT.get() :  Settings.DELETE_OTHER_MESSAGE_FORMAT.get();

        if (!viewer.hasPermission(permission))
            return null;

        Component appended = this.appendDeleteButton(component, message, viewer, format);
        return ChatComposer.json().composeAdventure().compose(appended);
    }

    @Override
    protected Component appendDeleteButton(Component component, DeletableMessage message, RosePlayer viewer, String placeholder) {
        Component button = this.getDeleteButton(message, viewer, placeholder);

        Component output = Component.empty();
        if (Settings.DELETE_MESSAGE_SUFFIX.get()) {
            output = output.append(component);

            if (button != null)
                output = output.append(button);
        } else {
            if (button != null)
                output = output.append(button);

            output = output.append(component);
        }

        return output;
    }

    @Override
    protected Component getDeleteButton(DeletableMessage message, RosePlayer viewer, String placeholder) {
        RosePlayer sender = new RosePlayer(message.getSender() == null ? Bukkit.getConsoleSender() : Bukkit.getPlayer(message.getSender()));

        return RoseChatAPI.getInstance().parse(new RosePlayer(Bukkit.getConsoleSender()), viewer, placeholder,
                DefaultPlaceholders.getFor(sender, viewer)
                        .add("id", message.getUUID().toString())
                        .add("type", "server")
                        .add("channel", message.getChannel())
                        .build()).build(ChatComposer.adventure().decorated());
    }

    public static Component appendClientDeleteButton(RosePlayer sender, PlayerData playerData, UUID messageId, String messageJson) {
        Component component = Component.empty();
        String placeholder = Settings.DELETE_CLIENT_MESSAGE_FORMAT.get();

        MessageContents results = RoseChatAPI.getInstance().parse(new RosePlayer(Bukkit.getConsoleSender()), sender, placeholder,
                DefaultPlaceholders.getFor(sender, sender)
                        .add("id", messageId.toString())
                        .add("type", "client").build());

        Component deleteClientButton = results.build(ChatComposer.adventure().decorated());
        if (deleteClientButton == null) {
            playerData.getMessageLog().addDeletableMessage(new DeletableMessage(UUID.randomUUID(), messageJson, true));
            return null;
        }

        if (Settings.DELETE_MESSAGE_SUFFIX.get()) {
            component = component.append(ChatComposer.adventure().decorated().composeJson(messageJson))
                    .append(deleteClientButton);
        } else {
            component = component.append(deleteClientButton)
                            .append(ChatComposer.adventure().decorated().composeJson(messageJson));
        }

        return component;
    }

}
