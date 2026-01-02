package dev.rosewood.rosechat.message.tokenizer.composer.decorator.bungee;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.decorator.SpriteDecorator;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ObjectComponent;
import net.md_5.bungee.api.chat.objects.SpriteObject;

public class BungeeSpriteDecorator extends BungeeTokenDecorator<SpriteDecorator> {

    public BungeeSpriteDecorator(SpriteDecorator decorator) {
        super(decorator);
    }

    @Override
    public void apply(BaseComponent component, Token parent) {
        SpriteObject spriteObject = new SpriteObject(this.decorator.atlas(), this.decorator.sprite());
        component.addExtra(new ObjectComponent(spriteObject));
    }

}
