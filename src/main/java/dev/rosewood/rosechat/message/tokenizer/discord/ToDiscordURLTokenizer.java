package dev.rosewood.rosechat.message.tokenizer.discord;

import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToDiscordURLTokenizer extends Tokenizer {

    private static final Pattern PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^()\\s]+(?:\\([^()\\s]+\\)[^()\\s]*)*)\\)");

    public ToDiscordURLTokenizer() {
        super("to_discord_url");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        String input = params.getInput();
        Matcher matcher = PATTERN.matcher(input);

        List<TokenizerResult> results = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();

            if (!this.hasTokenPermission(params, "rosechat.url"))
                return null;

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR && checkPermission(params, "rosechat.escape")) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            results.add(new TokenizerResult(Token.text(match), start, match.length()));
        }

        return results;
    }

}
