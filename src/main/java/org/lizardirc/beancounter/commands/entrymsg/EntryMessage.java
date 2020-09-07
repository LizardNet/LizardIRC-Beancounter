/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016-2020 by the LizardIRC Development Team. Some rights reserved.
 *
 * License GPLv3+: GNU General Public License version 3 or later (at your choice):
 * <http://gnu.org/licenses/gpl.html>. This is free software: you are free to
 * change and redistribute it at your will provided that your redistribution, with
 * or without modifications, is also licensed under the GNU GPL. (Although not
 * required by the license, we also ask that you attribute us!) There is NO
 * WARRANTY FOR THIS SOFTWARE to the extent permitted by law.
 *
 * Note that this is an official project of the LizardIRC IRC network.  For more
 * information about LizardIRC, please visit our website at
 * <https://www.lizardirc.org>.
 *
 * This is an open source project. The source Git repositories, which you are
 * welcome to contribute to, can be found here:
 * <https://gerrit.fastlizard4.org/r/gitweb?p=LizardIRC%2FBeancounter.git;a=summary>
 * <https://git.fastlizard4.org/gitblit/summary/?r=LizardIRC/Beancounter.git>
 *
 * Gerrit Code Review for the project:
 * <https://gerrit.fastlizard4.org/r/#/q/project:LizardIRC/Beancounter,n,z>
 *
 * Alternatively, the project source code can be found on the PUBLISH-ONLY mirror
 * on GitHub: <https://github.com/LizardNet/LizardIRC-Beancounter>
 *
 * Note: Pull requests and patches submitted to GitHub will be transferred by a
 * developer to Gerrit before they are acted upon.
 */

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
