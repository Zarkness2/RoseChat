package dev.rosewood.rosechat.message.tokenizer.sprite;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import dev.rosewood.rosechat.message.tokenizer.content.HeadTokenContent;
import dev.rosewood.rosegarden.utils.NMSUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadTokenizer extends Tokenizer {

    private static final Pattern PATTERN = Pattern.compile("<head(?::([a-zA-Z0-9_/]+))?(?::(true|false))?>");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    public HeadTokenizer() {
        super("head");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        if (!HeadTokenContent.VALID_VERSION)
            return List.of();

        String input = params.getInput();

        List<TokenizerResult> results = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(input);
        while (matcher.find()) {
            if (!this.hasTokenPermission(params, "rosechat.head"))
                return null;

            String content = matcher.group(1);
            String outerLayerStr = matcher.group(2);

            HeadTokenContent headContent;
            if (content == null) {
                headContent = HeadTokenContent.named(this.getRealName(params));
            } else if (outerLayerStr == null) {
                if (content.equalsIgnoreCase("true") || content.equalsIgnoreCase("false"))
                    headContent = HeadTokenContent.named(this.getRealName(params), Boolean.parseBoolean(content));
                else
                    headContent = this.create(content, true);
            } else {
                boolean outerLayer = Boolean.parseBoolean(outerLayerStr);
                headContent = this.create(content, outerLayer);
            }

            if (headContent != null) {
                results.add(new TokenizerResult(Token.content(headContent), matcher.start(), matcher.group().length()));
            } else {
                String group = matcher.group();
                results.add(new TokenizerResult(Token.text(group), matcher.start(), group.length()));
            }
        }

        return results;
    }

    private String getRealName(TokenizerParams params) {
        if (params.getSender().isConsole()) {
            return params.getReceiver().getRealName();
        } else {
            return params.getSender().getRealName();
        }
    }

    private HeadTokenContent create(String content, boolean outerLayer) {
        if (content.contains("/")) {
            if (!NMSUtil.isPaper())
                return null;
            return HeadTokenContent.textured(content, outerLayer);
        } else {
            try {
                UUID uuid = UUID.fromString(content);
                return HeadTokenContent.uuid(uuid, outerLayer);
            } catch (IllegalArgumentException e) {
                if (!USERNAME_PATTERN.matcher(content).matches())
                    return null;
                return HeadTokenContent.named(content, outerLayer);
            }
        }
    }

}
