package dev.rosewood.rosechat.message.tokenizer.sprite;

import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import dev.rosewood.rosechat.message.tokenizer.content.SpriteTokenContent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpriteTokenizer extends Tokenizer {

    public static final Pattern PATTERN = Pattern.compile("<sprite(?::([A-Za-z0-9._-]+|\"[A-Za-z0-9._:-]+\"))?:([^>]+)>");
    private static final String DEFAULT_ATLAS = "minecraft:blocks";

    public SpriteTokenizer() {
        super("sprite");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        String input = params.getInput();

        List<TokenizerResult> results = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(input);
        while (matcher.find()) {
            if (!checkPermission(params, "rosechat.sprite"))
                return null;

            String atlas = matcher.group(1) == null ? DEFAULT_ATLAS : matcher.group(1);
            atlas = atlas.startsWith("\"") && atlas.endsWith("\"") ? atlas.substring(1, atlas.length() - 1) : atlas;

            results.add(new TokenizerResult(Token.content(new SpriteTokenContent(atlas, matcher.group(2))),
                    matcher.start(), matcher.group().length()));
        }

        return results;
    }

}
