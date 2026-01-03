package dev.rosewood.rosechat.message.tokenizer.composer.decorator.adventure;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.decorator.HeadDecorator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

public class AdventureHeadDecorator extends AdventureTokenDecorator<HeadDecorator> {

    public AdventureHeadDecorator(HeadDecorator decorator) {
        super(decorator);
    }

    @Override
    public Component apply(Component component, Token parent) {
        PlayerHeadObjectContents.Builder builder = ObjectContents.playerHead();
        builder.hat(this.decorator.outerLayer());

        if (this.decorator.name() != null)
            builder.name(this.decorator.name());
        else if (this.decorator.uuid() != null)
            builder.id(this.decorator.uuid());
        else if (this.decorator.texture() != null)
            builder.texture(Key.key(this.decorator.texture()));
        else
            return component;

        return component.append(Component.object(builder.build()));
    }


}
