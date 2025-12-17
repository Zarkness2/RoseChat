package dev.rosewood.rosechat.message.tokenizer.discord.tag;

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

public class ToDiscordTagTokenizer extends Tokenizer {

    public ToDiscordTagTokenizer() {
        super("discord_tag");
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        DiscordChatProvider discord = RoseChatAPI.getInstance().getDiscord();
        if (discord == null || !Settings.CAN_TAG_MEMBERS.get())
            return null;

        String input = params.getInput();

        List<TokenizerResult> results = new ArrayList<>();

        int index = -1;
        while ((index = input.indexOf('@', index)) != -1) {
            if (!this.hasTokenPermission(params, "rosechat.tag"))
                return null;

            if (index > 0 && input.charAt(index - 1) == MessageUtils.ESCAPE_CHAR) {
                index++;
                continue;
            }

            String name = input.substring(index + 1);
            if (name.isEmpty())
                return results;

            DiscordChatProvider.DetectedMention member = discord.matchPartialMember(name);
            if (member == null) {
                index++;
                continue;
            }

            results.add(new TokenizerResult(Token.text(member.mention()), index, member.consumedTextLength() + 1));
            index += member.consumedTextLength();
        }

        return results;
    }

}
