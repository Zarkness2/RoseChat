package dev.rosewood.rosechat.message.tokenizer.discord.emoji;

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

public class DiscordCustomEmojiTokenizer extends Tokenizer {

    private static final Pattern PATTERN = Pattern.compile(":([a-zA-Z_]+):");

    public DiscordCustomEmojiTokenizer() {
        super("discord_custom_emoji");
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

            if (!this.hasTokenPermission(params, "rosechat.filters"))
                return null;

            String name = matcher.group(1);
            if (!this.hasExtendedTokenPermission(params, "rosechat.filters", "rosechat.filter." + name))
                continue;

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            String emoji = RoseChatAPI.getInstance().getDiscord().getCustomEmoji(name);
            results.add(new TokenizerResult(Token.text(emoji), start, match.length()));
        }

        return results;
    }

}
