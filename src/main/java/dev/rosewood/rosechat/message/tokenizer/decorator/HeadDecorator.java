package dev.rosewood.rosechat.message.tokenizer.decorator;

import dev.rosewood.rosegarden.utils.NMSUtil;
import java.util.UUID;

public record HeadDecorator(String name, UUID uuid, String texture, boolean outerLayer) implements TokenDecorator {

    public static final boolean VALID_VERSION = NMSUtil.getVersionNumber() > 21 || (NMSUtil.getVersionNumber() == 21 && NMSUtil.getMinorVersionNumber() >= 9);

    @Override
    public DecoratorType getType() {
        return DecoratorType.CONTENT;
    }

    @Override
    public boolean isOverwrittenBy(TokenDecorator newDecorator) {
        return newDecorator instanceof HeadDecorator;
    }

    @Override
    public boolean isMarker() {
        return !VALID_VERSION;
    }

    public static HeadDecorator named(String name) {
        return new HeadDecorator(name, null, null, true);
    }

    public static HeadDecorator named(String name, boolean outerLayer) {
        return new HeadDecorator(name, null, null, outerLayer);
    }

    public static HeadDecorator uuid(UUID uuid) {
        return new HeadDecorator(null, uuid, null, true);
    }

    public static HeadDecorator uuid(UUID uuid, boolean outerLayer) {
        return new HeadDecorator(null, uuid, null, outerLayer);
    }

    public static HeadDecorator textured(String texture) {
        return new HeadDecorator(null, null, texture, true);
    }

    public static HeadDecorator textured(String texture, boolean outerLayer) {
        return new HeadDecorator(null, null, texture, outerLayer);
    }

}
