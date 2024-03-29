/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.remind;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.lizardirc.beancounter.utils.Bases;
import org.lizardirc.beancounter.utils.Miscellaneous;

class Reminder {
    protected final String from; // Full nick@user@host mask of the sending user
    protected final String target; // user@host of the target user, or just a nickname (usually because target user was offline when the reminder was requested)
    protected final String message;
    protected final String channel; // Channel name, including prefix, if reminder was requested in a channel, otherwise an empty string (means reminder was probably requested in PM)
    protected final ZonedDateTime enteredTime;

    public Reminder(String from, String target, String message, String channel, ZonedDateTime enteredTime) {
        this.from = Miscellaneous.requireNonEmpty(from);
        this.target = Miscellaneous.requireNonEmpty(target).toLowerCase();
        this.message = Miscellaneous.requireNonEmpty(message);
        this.channel = Objects.requireNonNull(channel).toLowerCase(); // Allowed to be empty, just can't be null
        this.enteredTime = Objects.requireNonNull(enteredTime);

        if (this.from.endsWith("!@")) {
            throw new IllegalArgumentException("Failure constructing reminder: Could not set sender information.  Please restart the bot.");
        }

        if (this.target.equals("@")) {
            throw new IllegalArgumentException("Failure constructing reminder: Could not get target information.  Please restart the bot.");
        }
    }

    @Override
    public String toString() {
        return getResponseMessagePrefix(true)
            .append(": ")
            .append(message)
            .toString();
    }

    public StringBuilder getResponseMessagePrefix(boolean showRequestTimeBreakdown) {
        String[] sender = from.split("!");

        StringBuilder response = new StringBuilder();
        if (sender[1].equalsIgnoreCase(target) || sender[0].equalsIgnoreCase(target)) {
            response.append("YOU");
        } else {
            response.append(sender[0])
                .append(" (")
                .append(sender[1])
                .append(")");
        }

        response.append(" asked me ");

        response.append("at ")
            .append(enteredTime.format(DateTimeFormatter.RFC_1123_DATE_TIME))
            .append(" ");

        if (showRequestTimeBreakdown) {
            response.append(breakDownTime(this.enteredTime)).append(" ");
        }

        response.append("to remind you");

        return response;
    }

    protected StringBuilder breakDownTime(ZonedDateTime enteredTime) {
        long seconds = enteredTime.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
        long days = seconds / (24 * 60 * 60);
        seconds %= (24 * 60 * 60);
        long hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder response = new StringBuilder();

        response.append("(")
            .append(days)
            .append(" days, ")
            .append(hours)
            .append(" hours, ")
            .append(minutes)
            .append(" minutes, and ")
            .append(seconds)
            .append(" seconds ago)");

        return response;
    }

    public String toSerializedString() {
        return Bases.base64encode(from) + ':' + Bases.base64encode(target) + ':' + Bases.base64encode(message) + ':' +
            Bases.base64encode(channel) + ':' + enteredTime.toEpochSecond();
    }

    public static Reminder fromSerializedString(String stringRepresentation) {
        String[] s = stringRepresentation.split(":");
        return new Reminder(Bases.base64decode(s[0]), Bases.base64decode(s[1]), Bases.base64decode(s[2]),
            Bases.base64decode(s[3]), ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s[4])), ZoneId.systemDefault()));
    }

    public String getTarget() {
        return target;
    }

    public String getChannel() {
        return channel;
    }

    // Okay, now for the hard part
    @Override
    public int hashCode() {
        return Objects.hash(from.toLowerCase(), target, message, channel, enteredTime);
    }

    public boolean canEqual(Object other) {
        return other instanceof Reminder;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof Reminder) {
            Reminder that = (Reminder) other;
            result = that.canEqual(this)
                && this.from.equalsIgnoreCase(that.from)
                && this.target.equalsIgnoreCase(that.target)
                && this.message.equals(that.message)
                && this.channel.equalsIgnoreCase(that.channel)
                && this.enteredTime.equals(that.enteredTime);
        }

        return result;
    }
}
