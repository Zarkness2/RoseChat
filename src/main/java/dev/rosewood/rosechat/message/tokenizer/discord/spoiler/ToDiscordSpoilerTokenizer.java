package dev.rosewood.rosechat.message.tokenizer.discord.spoiler;

import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import dev.rosewood.rosechat.message.tokenizer.Tokenizers;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToDiscordSpoilerTokenizer extends Tokenizer {

    private final Pattern pattern;

    public ToDiscordSpoilerTokenizer() {
        super("to_discord_spoiler");

        String spoiler = Settings.MARKDOWN_FORMAT_SPOILER.get();
        String target = "%input_1%";
        if (!spoiler.contains(target)) {
            this.pattern = null;
            return;
        }

        int inputIndex = spoiler.indexOf(target);
        String prefix = spoiler.substring(0, inputIndex);
        String suffix = spoiler.substring(inputIndex + target.length());
        this.pattern = Pattern.compile(Pattern.quote(prefix) + "(.*?)" + Pattern.quote(suffix));
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        if (this.pattern == null)
            return null;

        String input = params.getInput();
        Matcher matcher = this.pattern.matcher(input);

        List<TokenizerResult> results = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();
            String content = matcher.group(1);

            if (!this.hasTokenPermission(params, "rosechat.spoiler"))
                return null;

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            results.add(new TokenizerResult(Token.group("||" + content + "||")
                    .ignoreTokenizer(this)
                    .ignoreTokenizer(Tokenizers.COLOR)
                    .ignoreTokenizer(Tokenizers.FORMAT)
                    .build(), start, match.length()));
        }

        return results;
    }

}
