package org.lizardirc.beancounter.commands.entrymsg;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * This class represents an entry message that will be displayed to all users who join the channel.  Immutable; if
 * the message is changed, an entirely new object should be constructed.
 */
class EntryMessage {
    /** The message text itself */
    private final String message;

    /** The nick!user@host of the user who set the message */
    private final String settingUser;

    /** The Unix timestamp the message was set, for ease of serialization */
    private final long timestamp;

    /** Allows for caching of the timestamp in ZonedDateTime form */
    private transient ZonedDateTime parsedTimestamp = null;

    public EntryMessage(String message, String settingUser, Instant timestamp) {
        this.message = Objects.requireNonNull(message);
        this.settingUser = Objects.requireNonNull(settingUser);
        this.timestamp = timestamp.getEpochSecond();
    }

    public String getMessage() {
        return message;
    }

    public String getSettingUser() {
        return settingUser;
    }

    public ZonedDateTime getTimestamp() {
        if (parsedTimestamp == null) {
            parsedTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
        }

        return parsedTimestamp;
    }
}
