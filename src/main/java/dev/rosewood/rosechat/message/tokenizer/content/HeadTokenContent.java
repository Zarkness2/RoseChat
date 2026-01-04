package dev.rosewood.rosechat.message.tokenizer.content;

import dev.rosewood.rosegarden.utils.NMSUtil;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record HeadTokenContent(String name, UUID uuid, String texture, boolean outerLayer) implements TokenContent {

    public static final boolean VALID_VERSION = NMSUtil.getVersionNumber() > 21 || (NMSUtil.getVersionNumber() == 21 && NMSUtil.getMinorVersionNumber() >= 9);

    @Override
    public boolean isValid() {
        return VALID_VERSION;
    }

    @Override
    public String toString() {
        return "[head]";
    }

    public static HeadTokenContent named(String name) {
        return new HeadTokenContent(name, null, null, true);
    }

    public static HeadTokenContent named(String name, boolean outerLayer) {
        return new HeadTokenContent(name, null, null, outerLayer);
    }

    public static HeadTokenContent uuid(UUID uuid) {
        return new HeadTokenContent(null, uuid, null, true);
    }

    public static HeadTokenContent uuid(UUID uuid, boolean outerLayer) {
        return new HeadTokenContent(null, uuid, null, outerLayer);
    }

    public static HeadTokenContent textured(String texture) {
        return new HeadTokenContent(null, null, texture, true);
    }

    public static HeadTokenContent textured(String texture, boolean outerLayer) {
        return new HeadTokenContent(null, null, texture, outerLayer);
    }

}
