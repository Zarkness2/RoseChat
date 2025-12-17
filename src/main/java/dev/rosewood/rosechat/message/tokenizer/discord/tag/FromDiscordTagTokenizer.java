package dev.rosewood.rosechat.message.tokenizer.discord.tag;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.filter.Filter;
import dev.rosewood.rosechat.config.Settings;
import dev.rosewood.rosechat.hook.discord.DiscordChatProvider;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.RosePlayer;
import dev.rosewood.rosechat.message.tokenizer.Token;
import dev.rosewood.rosechat.message.tokenizer.Tokenizer;
import dev.rosewood.rosechat.message.tokenizer.TokenizerParams;
import dev.rosewood.rosechat.message.tokenizer.TokenizerResult;
import dev.rosewood.rosechat.message.tokenizer.Tokenizers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class FromDiscordTagTokenizer extends Tokenizer {

    private static final Pattern TAG_PATTERN = Pattern.compile("<@([0-9]{17,19})>");
    private static final Pattern ROLE_TAG_PATTERN = Pattern.compile("<@&([0-9]{17,19})>");

    public FromDiscordTagTokenizer() {
        super("from_discord_tag");
    }

    private void collectUserTags(DiscordChatProvider discord, TokenizerParams params, List<TokenizerResult> results) {
        String input = params.getInput();
        Matcher matcher = TAG_PATTERN.matcher(input);

        while (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();
            String id = matcher.group(1);

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            UUID uuid = discord.getUUIDFromId(id);
            if (uuid == null) {
                results.add(new TokenizerResult(Token.group('@' + discord.getUserFromId(id))
                        .ignoreTokenizer(Tokenizers.FILTER)
                        .ignoreTokenizer(Tokenizers.BUNGEE_PAPI_PLACEHOLDER)
                        .ignoreTokenizer(Tokenizers.PAPI_PLACEHOLDER)
                        .ignoreTokenizer(Tokenizers.ROSECHAT_PLACEHOLDER)
                        .ignoreTokenizer(Tokenizers.FORMAT)
                        .ignoreTokenizer(Tokenizers.RAINBOW)
                        .ignoreTokenizer(Tokenizers.GRADIENT)
                        .encapsulate()
                        .build(), start, match.length()));
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                RosePlayer player = new RosePlayer(offlinePlayer);

                if (player.isOffline()) {
                    results.add(new TokenizerResult(Token.text('@' + discord.getUserFromId(id)), start, match.length()));
                } else {
                    results.add(new TokenizerResult(Token.group('@' + player.getName()).encapsulate().build(), start, match.length()));
                }
            }
        }
    }

    private void collectRoleTags(DiscordChatProvider discord, TokenizerParams params, List<TokenizerResult> results) {
        String input = params.getInput();
        Matcher matcher = ROLE_TAG_PATTERN.matcher(input);

        while (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();
            String id = matcher.group(1);

            if (start > 0 && input.charAt(start - 1) == MessageUtils.ESCAPE_CHAR) {
                results.add(new TokenizerResult(Token.text(match), start - 1, match.length() + 1));
                continue;
            }

            String role = discord.getRoleFromId(id);
            if (role == null)
                continue;

            params.getOutputs().getTaggedPlayers().addAll(discord.getPlayersWithRole(id));

            // Format and play the tag sound appropriately if a role is tagged.
            for (Filter filter : RoseChatAPI.getInstance().getFilters()) {
                if (filter.prefix() == null)
                    continue;

                if (!filter.tagPlayers())
                    continue;

                if (!filter.prefix().equals("@"))
                    continue;

                if (filter.sound() != null)
                    params.getOutputs().setSound(filter.sound());
            }

            results.add(new TokenizerResult(Token.group('@' + role)
                    .ignoreTokenizer(Tokenizers.FILTER)
                    .ignoreTokenizer(Tokenizers.BUNGEE_PAPI_PLACEHOLDER)
                    .ignoreTokenizer(Tokenizers.PAPI_PLACEHOLDER)
                    .ignoreTokenizer(Tokenizers.ROSECHAT_PLACEHOLDER)
                    .encapsulate().build(), start, match.length()));
        }
    }

    @Override
    public List<TokenizerResult> tokenize(TokenizerParams params) {
        DiscordChatProvider discord = RoseChatAPI.getInstance().getDiscord();
        if (discord == null)
            return null;

        List<TokenizerResult> results = new ArrayList<>();

        this.collectUserTags(discord, params, results);
        this.collectRoleTags(discord, params, results);

        return results;
    }

}
