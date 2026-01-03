package dev.rosewood.rosechat.message.tokenizer.composer.decorator.bungee;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.decorator.HeadDecorator;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ObjectComponent;
import net.md_5.bungee.api.chat.objects.PlayerObject;
import net.md_5.bungee.api.chat.player.Profile;
import net.md_5.bungee.api.chat.player.Property;

public class BungeeHeadDecorator extends BungeeTokenDecorator<HeadDecorator> {

    public BungeeHeadDecorator(HeadDecorator decorator) {
        super(decorator);
    }

    @Override
    public void apply(BaseComponent component, Token parent) {
        PlayerObject playerObject;
        if (this.decorator.name() != null)
            playerObject = new PlayerObject(new Profile(this.decorator.name()), this.decorator.outerLayer());
        else if (this.decorator.uuid() != null)
            playerObject = new PlayerObject(new Profile(this.decorator.uuid()), this.decorator.outerLayer());
        else if (this.decorator.texture() != null)
            playerObject = new PlayerObject(new Profile(new Property[]{ new Property("textures", this.decorator.texture())}), this.decorator.outerLayer());
        else
            return;

        component.addExtra(new ObjectComponent(playerObject));
    }

}
