package dev.rosewood.rosechat.message.tokenizer;

import dev.rosewood.rosechat.message.RosePlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A mutable object that represents outputs from parsing a message
 */
public class MessageOutputs {

    private final Set<UUID> taggedPlayers;
    private final Map<String, Boolean> checkedPermissions;
    private final List<String> serverCommands;
    private final List<String> playerCommands;
    private String sound;
    private String message;
    private RosePlayer placeholderTarget;

    public MessageOutputs() {
        this.taggedPlayers = new HashSet<>();
        this.checkedPermissions = new LinkedHashMap<>();
        this.serverCommands = new ArrayList<>();
        this.playerCommands = new ArrayList<>();
    }

    public Set<UUID> getTaggedPlayers() {
        return this.taggedPlayers;
    }

    public List<String> getServerCommands() {
        return this.serverCommands;
    }

    public List<String> getPlayerCommands() {
        return this.playerCommands;
    }

    public String getSound() {
        return this.sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<String> getMissingPermissions() {
        return this.checkedPermissions.entrySet().stream()
                .filter(x -> !x.getValue()).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Map<String, Boolean> getCheckedPermissions() {
        return this.checkedPermissions;
    }

    public RosePlayer getPlaceholderTarget() {
        return this.placeholderTarget;
    }

    public void setPlaceholderTarget(RosePlayer placeholderTarget) {
        this.placeholderTarget = placeholderTarget;
    }

}
