package dev.rosewood.rosechat.message.tokenizer.content;

public sealed interface TokenContent permits TextTokenContent, SpriteTokenContent, HeadTokenContent {

    boolean isValid();

}
