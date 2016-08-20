/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.reddit;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.google.gson.annotations.SerializedName;

import org.lizardirc.beancounter.utils.IrcColors;

public class RedditUser {
    @SerializedName("name")
    private String username;

    private String id;

    @SerializedName("is_friend")
    private boolean isFriend;

    @SerializedName("hide_from_robots")
    private boolean hideFromRobots;

    @SerializedName("is_gold")
    private boolean isGold;

    @SerializedName("is_mod")
    private boolean isMod;

    @SerializedName("has_verified_email")
    private boolean hasVerifiedEmail;

    private int created;

    @SerializedName("created_utc")
    private int createdUtc;

    @SerializedName("link_karma")
    private int linkKarma;

    @SerializedName("comment_karma")
    private int commentKarma;

    private boolean isCakeDay() {
        // Cake day begins at exact account creation time, per Sopel.

        Instant regDate = Instant.ofEpochSecond(createdUtc);

        ZonedDateTime now = Instant.now().atZone(ZoneId.systemDefault());
        ZonedDateTime cakeDayStart = regDate.atZone(ZoneId.systemDefault()).with(Year.now());
        ZonedDateTime cakeDayEnd = cakeDayStart.plus(1, ChronoUnit.DAYS);

        return cakeDayStart.compareTo(now) <= 0 && cakeDayEnd.compareTo(now) > 0;
    }

    @Override
    public String toString() {
        String cakeday = isCakeDay() ? " | " + IrcColors.MAGENTA + "Cake day" + IrcColors.RESET : "";

        String gold = isGold ? " | " + IrcColors.YELLOW + "Gold" + IrcColors.RESET : "";

        // is a moderator of at least one subreddit
        String mod = isMod ? " | " + IrcColors.GREEN + "Mod" + IrcColors.RESET : "";

        return String.format(
                "[REDDITOR] %1$s%2$s%3$s%4$s | Link: %5$d | Comment: %6$d",
                username,
                cakeday,
                gold,
                mod,
                linkKarma,
                commentKarma);
    }
}
