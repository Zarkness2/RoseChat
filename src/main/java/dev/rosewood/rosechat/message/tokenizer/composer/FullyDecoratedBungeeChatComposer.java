package dev.rosewood.rosechat.message.tokenizer.composer;

import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.TokenType;
import dev.rosewood.rosechat.message.tokenizer.composer.decorator.bungee.BungeeTokenDecorators;
import dev.rosewood.rosechat.message.tokenizer.content.HeadTokenContent;
import dev.rosewood.rosechat.message.tokenizer.content.SpriteTokenContent;
import dev.rosewood.rosechat.message.tokenizer.content.TextTokenContent;
import dev.rosewood.rosechat.message.tokenizer.content.TokenContent;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ObjectComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.objects.PlayerObject;
import net.md_5.bungee.api.chat.objects.SpriteObject;
import net.md_5.bungee.api.chat.player.Profile;
import net.md_5.bungee.api.chat.player.Property;
import org.bukkit.NamespacedKey;

public class FullyDecoratedBungeeChatComposer implements ChatComposer<BaseComponent[]> {

    public static final FullyDecoratedBungeeChatComposer INSTANCE = new FullyDecoratedBungeeChatComposer();

    protected FullyDecoratedBungeeChatComposer() {

    }

    @Override
    public BaseComponent[] compose(Token token) {
        return this.compose(token, this.createDecorators());
    }

    protected BaseComponent[] compose(Token token, BungeeTokenDecorators contextDecorators) {
        if (token.getType() != TokenType.GROUP)
            throw new IllegalStateException("Cannot convert a token that is not of type GROUP");

        ComponentBuilder componentBuilder = new ComponentBuilder();
        StringBuilder contentBuilder = new StringBuilder();

        for (Token child : token.getChildren()) {
            if ((child.getType() != TokenType.CONTENT || contextDecorators.blocksTextStitching() || !(child.getContent() instanceof TextTokenContent)) && !contentBuilder.isEmpty())
                this.applyAndDecorate(componentBuilder, contentBuilder, child, contextDecorators);

            switch (child.getType()) {
                case CONTENT -> this.appendContent(componentBuilder, contentBuilder, contextDecorators, child, child.getContent());
                case DECORATOR -> contextDecorators.add(child.getDecorators());
                case GROUP -> {
                    BungeeTokenDecorators childDecorators = child.shouldEncapsulate() ? this.createDecorators(contextDecorators) : contextDecorators;
                    for (BaseComponent component : this.compose(child, childDecorators))
                        componentBuilder.append(component, ComponentBuilder.FormatRetention.NONE);
                }
            }
        }

        if (!contentBuilder.isEmpty())
            this.applyAndDecorate(componentBuilder, contentBuilder, token, contextDecorators);

        BaseComponent[] components = componentBuilder.create();
        if (token.isPlain() || components.length == 0)
            return components;

        TextComponent wrapperComponent = new TextComponent(components);
        BungeeTokenDecorators wrapperDecorators = this.createDecorators();
        wrapperDecorators.add(token.getDecorators());
        wrapperDecorators.apply(wrapperComponent, token);
        return new BaseComponent[]{wrapperComponent};
    }

    protected BungeeTokenDecorators createDecorators() {
        return new BungeeTokenDecorators();
    }

    protected BungeeTokenDecorators createDecorators(BungeeTokenDecorators contextDecorators) {
        return new BungeeTokenDecorators(contextDecorators);
    }

    private void appendContent(ComponentBuilder componentBuilder, StringBuilder contentBuilder, BungeeTokenDecorators contextDecorators, Token parent, TokenContent content) {
        if (!content.isValid())
            return;

        switch (content) {
            case TextTokenContent(String s) -> contentBuilder.append(s);
            case HeadTokenContent(String name, UUID uuid, String texture, boolean outerLayer) -> {
                BaseComponent headComponent = ChatObjectHelper.createHeadComponent(name, uuid, texture, outerLayer);
                if (headComponent == null)
                    return;
                componentBuilder.append(headComponent, ComponentBuilder.FormatRetention.NONE);
                contextDecorators.apply(componentBuilder.getCurrentComponent(), parent, !Settings.COLOR_HEADS_AND_SPRITES.get());
            }
            case SpriteTokenContent(String atlas, String sprite) -> {
                BaseComponent spriteComponent = ChatObjectHelper.createSpriteComponent(atlas, sprite);
                componentBuilder.append(spriteComponent, ComponentBuilder.FormatRetention.NONE);
                contextDecorators.apply(componentBuilder.getCurrentComponent(), parent, !Settings.COLOR_HEADS_AND_SPRITES.get());
            }
        }
    }

    private void applyAndDecorate(ComponentBuilder componentBuilder, StringBuilder contentBuilder, Token token, BungeeTokenDecorators contextDecorators) {
        String content = contentBuilder.toString();
        contentBuilder.setLength(0);

        if (contextDecorators.blocksTextStitching()) {
            for (char c : content.toCharArray()) {
                componentBuilder.append(String.valueOf(c), ComponentBuilder.FormatRetention.NONE);
                contextDecorators.apply(componentBuilder.getCurrentComponent(), token);
            }
        } else {
            componentBuilder.append(content, ComponentBuilder.FormatRetention.NONE);
            contextDecorators.apply(componentBuilder.getCurrentComponent(), token);
        }
    }

    @Override
    public BaseComponent[] composeLegacy(String text) {
        return TextComponent.fromLegacyText(text);
    }

    @Override
    public BaseComponent[] composeJson(String json) {
        return MessageUtils.jsonToBungee(json);
    }

    @Override
    public BaseComponent[] composeBungee(BaseComponent[] components) {
        return components;
    }

    @Override
    public Adventure composeAdventure() {
        return Adventure.INSTANCE;
    }

    public static class Adventure implements ChatComposer.Adventure<BaseComponent[]> {

        private static final Adventure INSTANCE = new Adventure();

        Adventure() {

        }

        @Override
        public BaseComponent[] compose(Component component) {
            return BungeeComponentSerializer.get().serialize(component);
        }

    }

    /**
     * Used to avoid classloader errors on Paper and older versions of Spigot
     */
    private static class ChatObjectHelper {

        public static BaseComponent createHeadComponent(String name, UUID uuid, String texture, boolean outerLayer) {
            PlayerObject playerObject;
            if (name != null) {
                playerObject = new PlayerObject(new Profile(name), outerLayer);
            } else if (uuid != null) {
                playerObject = new PlayerObject(new Profile(uuid), outerLayer);
            } else if (texture != null) {
                // This doesn't actually work, seems like Bungee-chat might not support it?
                NamespacedKey key = NamespacedKey.fromString(texture);
                if (key == null)
                    return null;
                playerObject = new PlayerObject(new Profile(new Property[]{ new Property("texture", key.toString())}), outerLayer);
            } else {
                return null;
            }

            return new ObjectComponent(playerObject);
        }

        public static BaseComponent createSpriteComponent(String atlas, String sprite) {
            SpriteObject spriteObject = new SpriteObject(atlas, sprite);
            return new ObjectComponent(spriteObject);
        }

    }

}
