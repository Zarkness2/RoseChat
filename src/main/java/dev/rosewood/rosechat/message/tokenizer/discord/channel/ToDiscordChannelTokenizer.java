package dev.rosewood.rosechat.message.tokenizer.discord.channel;

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

public class ToDiscordChannelTokenizer extends Tokenizer {

    private static final Pattern PATTERN = Pattern.compile("#(?!\\s)(?!.*\\s$).{1,100}");

    public ToDiscordChannelTokenizer() {
        super("to_discord_channel");
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

            if (!this.hasTokenPermission(params, "rosechat.discordchannel"))
                return null;

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            DiscordChatProvider.DetectedMention channel = discord.matchPartialChannel(match.substring(1));
            if (channel == null)
                continue;

            results.add(new TokenizerResult(Token.text(channel.mention()), start, match.length()));
        }

        return results;
    }

}
