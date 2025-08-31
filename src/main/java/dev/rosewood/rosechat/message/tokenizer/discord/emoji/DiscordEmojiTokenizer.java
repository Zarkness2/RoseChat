package dev.rosewood.rosechat.message.tokenizer.discord.emoji;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.filter.Filter;
import dev.rosewood.rosechat.hook.discord.DiscordChatProvider;
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

public class DiscordEmojiTokenizer extends Tokenizer {

    private static final Pattern PATTERN = Pattern.compile("<a?(:[a-zA-Z0-9_\\-~]+:)[0-9]{17,19}>");

    public DiscordEmojiTokenizer() {
        super("discord_emoji");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        DiscordChatProvider discord = RoseChatAPI.getInstance().getDiscord();
        if (discord == null)
            return null;

        String input = params.getInput();
        Matcher matcher = PATTERN.matcher(input);

        List<TokenizerResult> results = new ArrayList<>();

        outer:
        while (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            String content = matcher.group(1);
            inner:
            for (Filter filter : RoseChatAPI.getInstance().getFilters()) {
                for (String filterMatch : filter.matches()) {
                    if (!filterMatch.equals(content))
                        continue;

                    if (filter.usePermission() != null && !this.hasExtendedTokenPermission(params, "rosechat.filters", filter.usePermission()))
                        continue inner;

                    content = filter.replacement();
                    results.add(new TokenizerResult(Token.group(content)
                            .ignoreTokenizer(this)
                            .ignoreTokenizer(Tokenizers.FILTER)
                            .build(), start, match.length()));
                    continue outer;
                }
            }

            results.add(new TokenizerResult(Token.text(content), start, match.length()));
        }

        return results;
    }

}
