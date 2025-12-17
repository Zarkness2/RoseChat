package dev.rosewood.rosechat.message.tokenizer.discord.spoiler;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.hook.discord.DiscordChatProvider;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FromDiscordSpoilerTokenizer extends Tokenizer {

    private static final Pattern PATTERN = Pattern.compile("\\|\\|(.*?)\\|\\|");

    public FromDiscordSpoilerTokenizer() {
        super("from_discord");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        DiscordChatProvider discord = RoseChatAPI.getInstance().getDiscord();
        if (discord == null)
            return null;

        String input = params.getInput();
        Matcher matcher = PATTERN.matcher(input);

        List<TokenizerResult> results = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();
            String content = matcher.group(1);

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            String format = Settings.MARKDOWN_FORMAT_SPOILER.get();
            String output = format.contains("%input_1%") ? format.replace("%input_1%", content) : format + content;

            results.add(new TokenizerResult(Token.group(output).ignoreTokenizer(this).build(), start, match.length()));
        }

        return results;
    }

}
