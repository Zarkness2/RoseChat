package dev.rosewood.rosechat.message.tokenizer.composer.decorator.adventure;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.decorator.SpriteDecorator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;

public class AdventureSpriteDecorator extends AdventureTokenDecorator<SpriteDecorator> {

    public AdventureSpriteDecorator(SpriteDecorator decorator) {
        super(decorator);
    }

    @Override
    public Component apply(Component component, Token parent) {
        return component.append(Component.object(ObjectContents.sprite(Key.key(this.decorator.atlas()), Key.key(this.decorator.sprite()))));
    }


}
