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

class TimedReminder extends Reminder implements Comparable<TimedReminder> {
    private final ZonedDateTime deliverAt;

    public TimedReminder(String from, String target, String message, String channel, ZonedDateTime enteredTime, ZonedDateTime deliverAt) {
        super(from, target, message, channel, enteredTime);
        this.deliverAt = Objects.requireNonNull(deliverAt); // Deliver reminder at or after this time
    }

    public TimedReminder(Reminder reminder, ZonedDateTime deliverAt) {
        super(reminder.from, reminder.target, reminder.message, reminder.channel, reminder.enteredTime);
        this.deliverAt = deliverAt;
    }

    @Override
    public int compareTo(TimedReminder o) {
        return Long.compare(this.getDeliveryTimeEpoch(), o.getDeliveryTimeEpoch());
    }

    public StringBuilder getResponseMessagePrefix(boolean showRequestTimeBreakdown, boolean showDeliverTime) {
        StringBuilder response = super.getResponseMessagePrefix(showRequestTimeBreakdown);

        if (showDeliverTime) {
            response.append(" at ")
                .append(deliverAt.format(DateTimeFormatter.RFC_1123_DATE_TIME));
        }

        return response;
    }

    @Override
    public String toSerializedString() {
        String s = super.toSerializedString();
        return s + ':' + deliverAt.toEpochSecond();
    }

    public static TimedReminder fromSerializedString(String stringRepresentation) {
        String[] s = stringRepresentation.split(":");
        return new TimedReminder(Reminder.fromSerializedString(stringRepresentation),
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s[5])), ZoneId.systemDefault()));
    }

    public ZonedDateTime getDeliveryTime() {
        return deliverAt;
    }

    public long getDeliveryTimeEpoch() {
        return deliverAt.toEpochSecond();
    }

    public boolean isDeliveryTime() {
        return deliverAt.until(ZonedDateTime.now(), ChronoUnit.SECONDS) >= 0;
    }
}
