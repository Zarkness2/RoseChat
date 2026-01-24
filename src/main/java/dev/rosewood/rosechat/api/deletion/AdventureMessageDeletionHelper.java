package dev.rosewood.rosechat.api.deletion;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.listener.packet.AdventurePacketListener;
import dev.rosewood.rosechat.message.DeletableMessage;
import dev.rosewood.rosechat.message.RosePlayer;
import dev.rosewood.rosechat.message.contents.MessageContents;
import dev.rosewood.rosechat.message.tokenizer.composer.ChatComposer;
import dev.rosewood.rosechat.placeholder.DefaultPlaceholders;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;

public class AdventureMessageDeletionHelper implements MessageDeletionHelper {

    @Override
    public void deleteMessage(RoseChatAPI api, RosePlayer player, UUID uuid) {
        DeletableMessage messageToDelete = null;

        for (DeletableMessage message : player.getPlayerData().getMessageLog().getDeletableMessages())
            if (message.getUUID().equals(uuid))
                messageToDelete = message;

        if (messageToDelete == null)
            return;

        MessageContents format = api.parse(player, player, Settings.DELETED_MESSAGE_FORMAT.get(),
                DefaultPlaceholders.getFor(player, player)
                        .add("id", uuid.toString())
                        .add("type", messageToDelete.isClient() ? "client" : "server")
                        .build());

        String plainText = format.build(ChatComposer.plain());

        boolean updated = false;
        if (!plainText.isEmpty()) {
            String json;

            if (player.hasPermission("rosechat.deletemessages.see")) {
                Component component = Component.empty();
                Component messageComponent = format.build(ChatComposer.adventure().decorated());
                component = component.append(messageComponent);
                if (component.hoverEvent() == null) {
                    Component hover = ChatComposer.adventure().decorated().composeJson(messageToDelete.getOriginal());
                    component = component.hoverEvent(HoverEvent.showText(hover));
                    json = ChatComposer.json().composeAdventure().compose(component);
                } else {
                    json = ChatComposer.json().composeAdventure().compose(messageComponent);
                }
            } else {
                json = format.build(ChatComposer.json());
            }

            if (player.hasPermission("rosechat.deletemessages.client")) {
                Component withDeleteButton = AdventurePacketListener.appendClientDeleteButton(player, player.getPlayerData(), uuid, json);
                if (withDeleteButton != null) {
                    messageToDelete.setJson(ChatComposer.json().composeAdventure().compose(withDeleteButton));
                } else {
                    messageToDelete.setJson(json);
                }
            } else {
                messageToDelete.setJson(json);
            }

            updated = true;
        }

        // Remove the original message if it was not changed.
        if (!updated)
            player.getPlayerData().getMessageLog().getDeletableMessages().remove(messageToDelete);

        // Send blank lines to clear the chat.
        for (int i = 0; i < 100; i++)
            player.send("\n");

        // Resend the messages!
        for (DeletableMessage message : player.getPlayerData().getMessageLog().getDeletableMessages()) {
            Component component = ChatComposer.adventure().decorated().composeJson(message.getJson());
            if (player.isPlayer()) {
                player.asPlayer().sendMessage(component);
            } else if (player.isConsole()) {
                Bukkit.getConsoleSender().sendMessage(component);
            }
        }

        // If the message is not a client message, delete it from Discord too.
        if (!messageToDelete.isClient()) {
            if (updated)
                messageToDelete.setClient(true);

            if (!Settings.DELETE_DISCORD_MESSAGES.get())
                return;

            if (api.getDiscord() != null && messageToDelete.getDiscordId() != null)
                api.getDiscord().deleteMessage(messageToDelete.getDiscordId());
        }
    }

}
