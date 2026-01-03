package dev.rosewood.rosechat.message.tokenizer.sprite;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import dev.rosewood.rosechat.message.tokenizer.decorator.HeadDecorator;
import dev.rosewood.rosechat.message.tokenizer.decorator.SpriteDecorator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadTokenizer extends Tokenizer {

    public static final Pattern PATTERN = Pattern.compile("<head(?::([^:>]+))?(?::(true|false))?>");

    public HeadTokenizer() {
        super("head");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        String input = params.getInput();
        if (!checkPermission(params, "rosechat.head"))
            return null;

        List<TokenizerResult> results = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(input);
        while (matcher.find()) {
            HeadDecorator headDecorator = null;

            String content = matcher.group(1);
            String outerLayerStr = matcher.group(2);
            if (content == null) {
                headDecorator = HeadDecorator.named(params.getSender().getRealName());
            } else if (outerLayerStr == null) {
                if (content.equalsIgnoreCase("true") || content.equalsIgnoreCase("false"))
                    headDecorator = HeadDecorator.named(params.getSender().getRealName(), Boolean.parseBoolean(content));
                else
                    headDecorator = this.create(content, true);
            } else {
                boolean outerLayer = Boolean.parseBoolean(outerLayerStr);
                headDecorator = this.create(content, outerLayer);
            }

            results.add(new TokenizerResult(Token.decorator(headDecorator), matcher.start(), matcher.group().length()));
        }

        return results;
    }

    private HeadDecorator create(String content, boolean outerLayer) {
        if (content.contains("/")) {
            return HeadDecorator.textured(content, outerLayer);
        } else {
            try {
                UUID uuid = UUID.fromString(content);
                return HeadDecorator.uuid(uuid, outerLayer);
            } catch (IllegalArgumentException e) {
                return HeadDecorator.named(content, outerLayer);
            }
        }
    }

}
