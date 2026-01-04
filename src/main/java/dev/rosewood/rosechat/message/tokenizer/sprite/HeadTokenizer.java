package dev.rosewood.rosechat.message.tokenizer.sprite;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import dev.rosewood.rosechat.message.tokenizer.content.HeadTokenContent;
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

        List<TokenizerResult> results = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(input);
        while (matcher.find()) {
            if (!checkPermission(params, "rosechat.head"))
                return null;

            String content = matcher.group(1);
            String outerLayerStr = matcher.group(2);

            HeadTokenContent headContent;
            if (content == null) {
                headContent = HeadTokenContent.named(params.getSender().getRealName());
            } else if (outerLayerStr == null) {
                if (content.equalsIgnoreCase("true") || content.equalsIgnoreCase("false"))
                    headContent = HeadTokenContent.named(params.getSender().getRealName(), Boolean.parseBoolean(content));
                else
                    headContent = this.create(content, true);
            } else {
                boolean outerLayer = Boolean.parseBoolean(outerLayerStr);
                headContent = this.create(content, outerLayer);
            }

            results.add(new TokenizerResult(Token.content(headContent), matcher.start(), matcher.group().length()));
        }

        return results;
    }

    private HeadTokenContent create(String content, boolean outerLayer) {
        if (content.contains("/")) {
            return HeadTokenContent.textured(content, outerLayer);
        } else {
            try {
                UUID uuid = UUID.fromString(content);
                return HeadTokenContent.uuid(uuid, outerLayer);
            } catch (IllegalArgumentException e) {
                return HeadTokenContent.named(content, outerLayer);
            }
        }
    }

}
