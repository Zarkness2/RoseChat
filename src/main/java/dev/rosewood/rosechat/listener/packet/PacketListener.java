package dev.rosewood.rosechat.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.rosewood.rosechat.RoseChat;
import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.message.DeletableMessage;
import dev.rosewood.rosechat.message.RosePlayer;
import dev.rosewood.rosechat.message.tokenizer.composer.ChatComposer;
import dev.rosewood.rosegarden.utils.NMSUtil;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.md_5.bungee.api.chat.BaseComponent;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;

public abstract class PacketListener<T> {

    protected static final String EMPTY_JSON = "{\"text\":\"\"}";
    protected static Field componentsArrayField;
    protected static Field adventureMessageField;
    protected static Field contentField;
    protected static Field adventureContentField;

    protected final PacketType[] legacyTypes;
    protected final PacketType[] types;
    protected final Cache<UUID, Boolean> permissionsCache;
    protected final Cache<UUID, String> groupCache;
    protected final RoseChat plugin;
    protected ListenerPriority priority;

    protected PacketListener(RoseChat plugin) {
        this.plugin = plugin;

        this.permissionsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS).build();
        this.groupCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS).build();
        this.legacyTypes = new PacketType[]{ PacketType.Play.Server.CHAT };
        this.types = new PacketType[]{ PacketType.Play.Server.CHAT, PacketType.Play.Server.SYSTEM_CHAT };
    }

    public void removeListeners() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this.plugin);
    }

    public void addListener() {
        this.updateListenerPriority();

        PacketType[] packetTypes = NMSUtil.getVersionNumber() >= 19 ? this.types : this.legacyTypes;
        PacketAdapter packetAdapter = new PacketAdapter(this.plugin, this.priority, packetTypes) {

            @Override
            public void onPacketSending(PacketEvent event) {
                RosePlayer player = new RosePlayer(event.getPlayer());
                PlayerData data = player.getPlayerData();
                if (data == null)
                    return;

                PacketContainer packet = event.getPacket();
                if (event.getPacketType() == PacketType.Play.Server.CHAT) {
                    WrappedChatComponent wrappedChatComponent = packet.getChatComponents().readSafely(0);
                    String messageJson = wrappedChatComponent == null ?
                            PacketListener.this.getMessageReflectively(packet) :
                            wrappedChatComponent.getJson();

                    String updatedMessageJson = PacketListener.this.createDeletableMessage(messageJson, player, event);
                    if (updatedMessageJson == null)
                        return;

                    // Overwrite the packet since packet fields are final in 1.19
                    PacketContainer newPacket = new PacketContainer(PacketType.Play.Server.CHAT);
                    newPacket.getChatComponents().write(0, WrappedChatComponent.fromJson(updatedMessageJson));
                    event.setPacket(newPacket);
                    return;
                }

                if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT) {
                    // Ignore actionbar messages
                    if (packet.getBooleans().size() == 0 || packet.getBooleans().readSafely(0))
                        return;

                    String messageJson;

                    // Use a chat component on 1.20.4+
                    if (NMSUtil.getVersionNumber() > 20 ||
                            (NMSUtil.getVersionNumber() == 20 && NMSUtil.getMinorVersionNumber() >= 4)) {
                        WrappedChatComponent wrappedChatComponent = packet.getChatComponents().readSafely(0);
                        messageJson = wrappedChatComponent == null ?
                                PacketListener.this.getMessageReflectively(packet) :
                                wrappedChatComponent.getJson();
                    } else {
                        String jsonString = packet.getStrings().readSafely(0);
                        messageJson = jsonString == null ?
                                PacketListener.this.getMessageReflectively(packet) :
                                jsonString;
                    }

                    String updatedMessageJson = PacketListener.this.createDeletableMessage(messageJson, player, event);
                    if (updatedMessageJson == null)
                        return;

                    event.setPacket(PacketListener.this.createSystemPacket(updatedMessageJson));
                }
            }
        };

        ProtocolLibrary.getProtocolManager().addPacketListener(packetAdapter);
    }

    private String createDeletableMessage(String messageJson, RosePlayer player, PacketEvent event) {
        if (messageJson == null || messageJson.equalsIgnoreCase("\"\"") || messageJson.equalsIgnoreCase(EMPTY_JSON))
            return null;

        boolean updated = PacketListener.this.updateLoggedMessage(player, player.getPlayerData(), messageJson, event);
        if (updated)
            return null;

        UUID messageId = UUID.randomUUID();
        if (PacketListener.this.updateAndCheckPermissions(player, messageId, messageJson))
            return null;

        // Grab the message json with the delete button applied.
        String updatedMessageJson = PacketListener.this.getDeletableMessageJson(player, messageId, messageJson);
        if (updatedMessageJson == null)
            return null;

        // Set the original message json to the message without the button applied.
        DeletableMessage deletableMessage = new DeletableMessage(messageId, messageJson, true);
        deletableMessage.setOriginal(messageJson);

        // Set the actual message json to the message with the button applied.
        deletableMessage.setJson(updatedMessageJson);
        player.getPlayerData().getMessageLog().addDeletableMessage(deletableMessage);

        return updatedMessageJson;
    }

    protected void updateListenerPriority() {
        try {
            this.priority = ListenerPriority.valueOf(Settings.PACKET_EVENT_PRIORITY.get());
        } catch (IllegalArgumentException ignored) {
            this.priority = ListenerPriority.NORMAL;
        }
    }

    protected abstract String getMessageReflectively(PacketContainer packet);

    protected abstract String getServerMessageJson(DeletableMessage message, RosePlayer viewer);

    protected abstract T appendDeleteButton(T component, DeletableMessage message, RosePlayer viewer, String placeholder);

    protected abstract T getDeleteButton(DeletableMessage message, RosePlayer viewer, String placeholder);

    public abstract String getDeletableMessageJson(RosePlayer player, UUID messageId, String messageJson);

    protected PacketContainer createSystemPacket(String json) {
        // Overwrite the packet since packet fields are final in 1.19
        PacketContainer packet = new PacketContainer(NMSUtil.getVersionNumber() >= 19 ?
                PacketType.Play.Server.SYSTEM_CHAT :
                PacketType.Play.Server.CHAT);

        if (NMSUtil.getVersionNumber() > 20 ||
                (NMSUtil.getVersionNumber() == 20 && NMSUtil.getMinorVersionNumber() >= 4)) {
            packet.getChatComponents().write(0, WrappedChatComponent.fromJson(json));
        } else if (NMSUtil.getVersionNumber() == 19) {
            packet.getStrings().write(0, json);
        } else {
            packet.getChatComponents().write(0, WrappedChatComponent.fromJson(json));
        }

        return packet;
    }

    protected boolean updateAndCheckPermissions(RosePlayer player, UUID messageId, String messageJson) {
        if (!this.permissionsCache.asMap().containsKey(player.getUUID()))
            this.permissionsCache.put(player.getUUID(), player.hasPermission("rosechat.deletemessages.client"));

        Boolean hasPermissionCache = this.permissionsCache.getIfPresent(player.getUUID());
        if (hasPermissionCache == null || !hasPermissionCache) {
            player.getPlayerData().getMessageLog().addDeletableMessage(new DeletableMessage(messageId, messageJson, true));
            return true;
        }

        if (!this.groupCache.asMap().containsKey(player.getUUID())) {
            Permission vault = RoseChatAPI.getInstance().getVault();
            if (vault != null) {
                String group = player.getPermissionGroup();
                this.groupCache.put(player.getUUID(), group);
            }
        }

        String group = this.groupCache.getIfPresent(player.getUUID());
        player.setPermissionGroup(group == null ? "default" : group);
        return false;
    }

    // Ensure chat messages are added separately to differentiate between client and server messages.
    protected boolean updateLoggedMessage(RosePlayer player, PlayerData data, String messageJson, PacketEvent event) {
        DeletableMessage loggedMessage = data.getMessageLog().getDeletableMessage(messageJson);
        if (loggedMessage != null) {
            if (loggedMessage.isClient() || loggedMessage.getOriginal() != null)
                return true;

            // Update the logged message to keep the unedited message and the new edited message.
            loggedMessage.setOriginal(loggedMessage.getJson());
            String json = this.getServerMessageJson(loggedMessage, player);
            if (json == null)
                return true;

            loggedMessage.setJson(json);
            event.setPacket(this.createSystemPacket(json));
            return true;
        }

        return false;
    }

}
