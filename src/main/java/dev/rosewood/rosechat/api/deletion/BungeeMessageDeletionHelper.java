package dev.rosewood.rosechat.api.deletion;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.listener.packet.BungeePacketListener;
import dev.rosewood.rosechat.message.DeletableMessage;
import dev.rosewood.rosechat.message.RosePlayer;
import dev.rosewood.rosechat.message.contents.MessageContents;
import dev.rosewood.rosechat.message.tokenizer.composer.ChatComposer;
import dev.rosewood.rosechat.placeholder.DefaultPlaceholders;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

public class BungeeMessageDeletionHelper implements MessageDeletionHelper {


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
                ComponentBuilder builder = new ComponentBuilder();
                BaseComponent[] messageComponents = format.build(ChatComposer.decorated());
                builder.append(messageComponents);
                if (builder.getCurrentComponent().getHoverEvent() == null) {
                    builder.getCurrentComponent().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComposer.decorated().composeJson(messageToDelete.getOriginal())));
                    json = ChatComposer.json().composeBungee(new BaseComponent[]{builder.build()});
                } else {
                    json = ChatComposer.json().composeBungee(messageComponents);
                }
            } else {
                json = format.build(ChatComposer.json());
            }

            if (player.hasPermission("rosechat.deletemessages.client")) {
                BaseComponent[] withDeleteButton = BungeePacketListener.appendClientDeleteButton(player, player.getPlayerData(), uuid, json);

                if (withDeleteButton != null) {
                    messageToDelete.setJson(ChatComposer.json().composeBungee(withDeleteButton));
                } else {
                    // If the delete button doesn't exist, just use the 'Deleted Message' message.
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
            player.send(ChatComposer.decorated().composeJson(message.getJson()));
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
