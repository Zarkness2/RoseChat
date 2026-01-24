package dev.rosewood.rosechat.api.deletion;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.message.RosePlayer;
import java.util.UUID;

public interface MessageDeletionHelper {

    void deleteMessage(RoseChatAPI api, RosePlayer player, UUID uuid);

}
