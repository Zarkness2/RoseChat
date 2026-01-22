package dev.rosewood.rosechat.message.tokenizer.composer;

import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.TokenType;
import dev.rosewood.rosechat.message.tokenizer.composer.decorator.adventure.AdventureTokenDecorators;
import dev.rosewood.rosechat.message.tokenizer.content.HeadTokenContent;
import dev.rosewood.rosechat.message.tokenizer.content.SpriteTokenContent;
import dev.rosewood.rosechat.message.tokenizer.content.TextTokenContent;
import dev.rosewood.rosechat.message.tokenizer.content.TokenContent;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

public class FullyDecoratedAdventureChatComposer implements ChatComposer<Component> {

    public static final FullyDecoratedAdventureChatComposer INSTANCE = new FullyDecoratedAdventureChatComposer();

    protected FullyDecoratedAdventureChatComposer() {

    }

    @Override
    public Component compose(Token token) {
        return this.compose(token, this.createDecorators());
    }

    protected Component compose(Token token, AdventureTokenDecorators contextDecorators) {
        if (token.getType() != TokenType.GROUP)
            throw new IllegalStateException("Cannot convert a token that is not of type GROUP");

        Component componentBuilder = Component.empty();
        StringBuilder contentBuilder = new StringBuilder();

        for (Token child : token.getChildren()) {
            if ((child.getType() != TokenType.CONTENT || contextDecorators.blocksTextStitching() || !(child.getContent() instanceof TextTokenContent)) && !contentBuilder.isEmpty())
                componentBuilder = this.applyAndDecorate(componentBuilder, contentBuilder, child, contextDecorators);

            switch (child.getType()) {
                case CONTENT -> componentBuilder = this.appendContent(componentBuilder, contentBuilder, contextDecorators, child, child.getContent());
                case DECORATOR -> contextDecorators.add(child.getDecorators());
                case GROUP -> {
                    AdventureTokenDecorators childDecorators = child.shouldEncapsulate() ? this.createDecorators(contextDecorators) : contextDecorators;
                    componentBuilder = componentBuilder.append(this.compose(child, childDecorators));
                }
            }
        }

        if (!contentBuilder.isEmpty())
            componentBuilder = this.applyAndDecorate(componentBuilder, contentBuilder, token, contextDecorators);

        if (token.isPlain())
            return componentBuilder;

        Component wrapperComponent = Component.textOfChildren(componentBuilder);
        AdventureTokenDecorators wrapperDecorators = this.createDecorators();
        wrapperDecorators.add(token.getDecorators());
        wrapperComponent = wrapperDecorators.apply(wrapperComponent, token);
        return wrapperComponent;
    }

    protected AdventureTokenDecorators createDecorators() {
        return new AdventureTokenDecorators();
    }

    protected AdventureTokenDecorators createDecorators(AdventureTokenDecorators contextDecorators) {
        return new AdventureTokenDecorators(contextDecorators);
    }

    private Component appendContent(Component componentBuilder, StringBuilder contentBuilder, AdventureTokenDecorators contextDecorators, Token parent,TokenContent content) {
        if (!content.isValid())
            return componentBuilder;

        return switch (content) {
            case TextTokenContent(String s) -> {
                contentBuilder.append(s);
                yield componentBuilder;
            }
            case HeadTokenContent(String name, UUID uuid, String texture, boolean outerLayer) -> {
                PlayerHeadObjectContents.Builder builder = ObjectContents.playerHead();
                builder.hat(outerLayer);

                if (name != null) {
                    builder.name(name);
                } else if (uuid != null) {
                    builder.id(uuid);
                } else if (texture != null) {
                    builder.texture(Key.key(texture));
                } else {
                    yield componentBuilder;
                }

                Component headComponent = Component.object(builder.build());
                yield componentBuilder.append(contextDecorators.apply(headComponent, parent, !Settings.COLOR_HEADS_AND_SPRITES.get()));
            }
            case SpriteTokenContent(String atlas, String sprite) -> {
                Component spriteComponent = Component.object(ObjectContents.sprite(Key.key(atlas), Key.key(sprite)));
                yield componentBuilder.append(contextDecorators.apply(spriteComponent, parent, !Settings.COLOR_HEADS_AND_SPRITES.get()));
            }
        };
    }

    private Component applyAndDecorate(Component component, StringBuilder contentBuilder, Token token, AdventureTokenDecorators contextDecorators) {
        String content = contentBuilder.toString();
        contentBuilder.setLength(0);

        if (contextDecorators.blocksTextStitching()) {
            for (char c : content.toCharArray())
                component = component.append(contextDecorators.apply(Component.text(c), token));
            return component;
        } else {
            return component.append(contextDecorators.apply(Component.text(content), token));
        }
    }

    @Override
    public Component composeLegacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    @Override
    public Component composeJson(String json) {
        return GsonComponentSerializer.gson().deserialize(json);
    }

    @Override
    public Component composeBungee(BaseComponent[] components) {
        return BungeeComponentSerializer.get().deserialize(components);
    }

    @Override
    public Adventure composeAdventure() {
        return Adventure.INSTANCE;
    }

    public static class Adventure implements ChatComposer.Adventure<Component> {

        private static final Adventure INSTANCE = new Adventure();

        Adventure() {

        }

        @Override
        public Component compose(Component component) {
            return component;
        }

    }

}
