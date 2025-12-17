package dev.rosewood.rosechat.manager;

import dev.rosewood.rosechat.chat.filter.Filter;
import dev.rosewood.rosechat.chat.filter.Filter.MatchType;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;

public class FilterManager extends Manager {

    private final Map<String, Filter> filters;
    private final Map<String, List<Pattern>> compiledPatterns;

    public FilterManager(RosePlugin rosePlugin) {
        super(rosePlugin);

        this.filters = new HashMap<>();
        this.compiledPatterns = new HashMap<>();
    }

    @Override
    public void reload() {
        File filtersFolder = new File(this.rosePlugin.getDataFolder(), "filters/");
        if (!filtersFolder.exists()) {
            filtersFolder.mkdirs();
            this.rosePlugin.saveResource("filters/colors.yml", false);
            this.rosePlugin.saveResource("filters/fun.yml", false);
            this.rosePlugin.saveResource("filters/swears.yml", false);
        }

        File[] filterFiles = filtersFolder.listFiles();
        if (filterFiles == null)
            return;

        for (File file : filterFiles) {
            CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
            for (String id : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(id);
                if (section == null)
                    continue;

                Filter filter = this.parseFilter(id, section);
                this.addFilter(id, filter);
            }
        }
    }

    private Filter parseFilter(String id, ConfigurationSection section) {
        Filter filter = new Filter(id,
                section.getStringList("matches"),
                section.getString("match-type", "anywhere").equalsIgnoreCase("word") ? MatchType.WORD : MatchType.ANYWHERE,
                section.getString("prefix"), section.getString("suffix"),
                section.getStringList("inline-matches"),
                section.getString("inline-prefix"), section.getString("inline-suffix"),
                section.getString("stop"),
                section.getInt("sensitivity"),
                section.getBoolean("use-regex"), section.getBoolean("is-emoji"),
                section.getBoolean("block"),
                section.getString("message"), section.getString("sound"),
                section.getBoolean("can-toggle"), section.getBoolean("color-retention"),
                section.getBoolean("tag-players"),section.getBoolean("match-length"),
                section.getBoolean("notify-staff"),
                !section.contains("add-to-suggestions") || section.getBoolean("add-to-suggestions"),
                section.getBoolean("escapable"),
                section.getString("permission.bypass"), section.getString("permission.use"),
                section.getString("hover"), section.getString("font"),
                section.getString("replacement"), section.getString("discord-output"),
                section.getStringList("commands.server"), section.getStringList("commands.player"));

        if (section.getBoolean("is-tag"))
            this.filters.put(id + "-tag", filter.cloneAsTag());

        return filter;
    }

    private void precompilePatterns(String id, Filter filter) {
        if (filter.useRegex()) {
            this.compile(id, FilterPattern.REGEX_MATCHES, filter.matches(), false);
            this.compile(id, FilterPattern.INLINE_MATCHES, filter.inlineMatches(), false);

            if (filter.prefix() != null && filter.inlineMatches().isEmpty())
                this.compile(id, FilterPattern.PREFIX, filter.prefix(), false);
        }

        if (filter.inlinePrefix() != null && filter.inlineSuffix() != null && filter.prefix() != null && filter.suffix() != null) {
            String regex = "(?:" + Pattern.quote(filter.prefix()) + "(.*?)" + Pattern.quote(filter.suffix()) + ")"
                    + Pattern.quote(filter.inlinePrefix()) + "(.*?)" + Pattern.quote(filter.inlineSuffix());
            this.compile(id, FilterPattern.INLINE_PREFIX_SUFFIX, regex, false);
        }

        this.compile(id, FilterPattern.MATCHES, filter.matches(), true);
        this.compile(id, FilterPattern.STOP, filter.stop(), false);
    }

    @Override
    public void disable() {
        this.filters.clear();
        this.compiledPatterns.clear();
    }

    public Filter getFilter(String id) {
        return this.filters.get(id);
    }

    public void addFilter(String id, Filter filter) {
        this.precompilePatterns(id, filter);
        this.filters.put(id, filter);
    }

    public void deleteFilter(String id) {
        this.filters.remove(id);
    }

    public Map<String, Filter> getFilters() {
        return this.filters;
    }

    public List<Pattern> getCompiledPatterns(String id, FilterPattern patternType) {
        return this.compiledPatterns.getOrDefault(patternType.buildName(id), List.of());
    }

    public Pattern getCompiledPattern(String id, FilterPattern patternType) {
        List<Pattern> patterns = this.compiledPatterns.get(patternType.buildName(id));
        if (patterns == null || patterns.isEmpty())
            return null;
        return patterns.getFirst();
    }

    private void compile(String id, FilterPattern patternType, String pattern, boolean quotePattern) {
        if (pattern != null)
            this.compile(id, patternType, List.of(pattern), quotePattern);
    }

    private void compile(String id, FilterPattern patternType, List<String> patterns, boolean quotePattern) {
        if (patterns == null || patterns.isEmpty())
            return;

        List<Pattern> compiled = new ArrayList<>();
        for (String pattern : patterns) {
            if (quotePattern) {
                compiled.add(Pattern.compile(Pattern.quote(pattern)));
            } else {
                compiled.add(Pattern.compile(pattern));
            }
        }

        this.compiledPatterns.put(patternType.buildName(id), compiled);
    }

    public enum FilterPattern {
        REGEX_MATCHES("regex-matches"),
        MATCHES("matches"),
        INLINE_MATCHES("inline-matches"),
        INLINE_PREFIX_SUFFIX("inline-prefix-suffix"),
        PREFIX("prefix"),
        STOP("stop");

        private final String suffix;

        FilterPattern(String suffix) {
            this.suffix = suffix;
        }

        public String buildName(String filterId) {
            return filterId + "-" + this.suffix;
        }
    }

}
