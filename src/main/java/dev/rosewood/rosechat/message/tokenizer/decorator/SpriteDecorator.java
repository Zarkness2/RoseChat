package dev.rosewood.rosechat.message.tokenizer.decorator;

import dev.rosewood.rosegarden.utils.NMSUtil;

public record SpriteDecorator(String atlas, String sprite) implements TokenDecorator {

    public static final boolean VALID_VERSION = NMSUtil.getVersionNumber() > 21 || (NMSUtil.getVersionNumber() == 21 && NMSUtil.getMinorVersionNumber() >= 9);

    @Override
    public DecoratorType getType() {
        return DecoratorType.CONTENT;
    }

    @Override
    public boolean isOverwrittenBy(TokenDecorator newDecorator) {
        return newDecorator instanceof SpriteDecorator;
    }

    @Override
    public boolean isMarker() {
        return !VALID_VERSION;
    }

}
