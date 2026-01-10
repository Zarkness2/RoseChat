package dev.rosewood.rosechat.message.tokenizer.composer.decorator.adventure;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.composer.decorator.TokenDecorators;
import dev.rosewood.rosechat.message.tokenizer.decorator.DecoratorType;
import net.kyori.adventure.text.Component;

public class AdventureTokenDecorators extends TokenDecorators<AdventureTokenDecorator<?>> {

    @SuppressWarnings("unchecked")
    public AdventureTokenDecorators() {
        super(AdventureTokenDecorator::from, (Class<AdventureTokenDecorator<?>>) (Class<?>) AdventureTokenDecorator.class);
    }

    public AdventureTokenDecorators(AdventureTokenDecorators decorators) {
        this();

        this.add(decorators.decorators);
    }

    public Component apply(Component component, Token parent) {
        return this.apply(component, parent, false);
    }

    public Component apply(Component component, Token parent, boolean stripStyling) {
        for (AdventureTokenDecorator<?> decorator : this.decorators)
            if (!stripStyling || decorator.getType() != DecoratorType.STYLING)
                component = decorator.apply(component, parent);
        return component;
    }

}
