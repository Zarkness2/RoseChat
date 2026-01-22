package dev.rosewood.rosechat.message.tokenizer.content;

import org.jetbrains.annotations.NotNull;

public record TextTokenContent(String text) implements TokenContent {

    @Override
    public boolean isValid() {
        return true;
    }

    @NotNull
    @Override
    public String toString() {
        return this.text;
    }

}
