package dev.rosewood.rosechat.message.tokenizer.content;

import dev.rosewood.rosegarden.utils.NMSUtil;

public record SpriteTokenContent(String atlas, String sprite) implements TokenContent {

    public static final boolean VALID_VERSION = NMSUtil.getVersionNumber() > 21 || (NMSUtil.getVersionNumber() == 21 && NMSUtil.getMinorVersionNumber() >= 9);

    @Override
    public boolean isValid() {
        return VALID_VERSION;
    }

    @Override
    public String toString() {
        return "[sprite]";
    }

}
