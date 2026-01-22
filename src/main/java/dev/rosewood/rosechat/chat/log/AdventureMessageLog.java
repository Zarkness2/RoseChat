package dev.rosewood.rosechat.chat.log;

import dev.rosewood.rosechat.message.DeletableMessage;
import dev.rosewood.rosechat.message.tokenizer.composer.ChatComposer;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class AdventureMessageLog extends PlayerMessageLog {

    public AdventureMessageLog(UUID sender) {
        super(sender);
    }

    @Override
    public DeletableMessage getDeletableMessage(String json) {
        if (this.getDeletableMessages().isEmpty())
            return null;

        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().build();
        Component one = ChatComposer.adventure().decorated().composeJson(json);
        String legacyOne = serializer.serialize(one);
        for (int i = this.getDeletableMessages().size() - 1; i >= 0; i--) {
            DeletableMessage message = this.getDeletableMessages().get(i);
            Component two = ChatComposer.adventure().decorated().composeJson(message.getJson());
            String legacyTwo = serializer.serialize(two);

            if (legacyOne.equalsIgnoreCase(legacyTwo))
                return message;
        }

        return null;
    }

}
