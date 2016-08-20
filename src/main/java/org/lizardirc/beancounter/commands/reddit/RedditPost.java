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
import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.lizardirc.beancounter.utils.IrcColors;

public class RedditPost {
    private String subreddit;
    private String id;
    private String author;
    private String title;
    private String url;
    private int gilded;
    private int score;
    @SerializedName("num_comments")
    private int numComments;
    @SerializedName("upvote_ratio")
    private double upvoteRatio;
    @SerializedName("created_utc")
    private int createdUtc;
    @SerializedName("over_18")
    private boolean isNsfw;
    @SerializedName("is_self")
    private boolean isSelf;

    @Override
    public String toString() {
        String link;
        if (isSelf) {
            link = "(self." + subreddit + ")";
        } else {
            link = String.format("( %1$s to r/%2$s )", url, subreddit);
        }

        String percent = String.format(
                "%2$s%1$.2f%%%3$s",
                upvoteRatio * 100,
                score > 0 ? IrcColors.DARKGREEN : IrcColors.RED,
                IrcColors.RESET);

        return String.format(
                "[REDDIT] %1$s %2$s%3$s | %4$d points (%5$s) | %6$d comments | Posted by %7$s | Created at %8$tF %8$tT",
                title,
                link,
                isNsfw ? " [" + IrcColors.RED + "NSFW" + IrcColors.RESET + "]" : "",
                score,
                percent,
                numComments,
                author == null ? "[deleted]" : author,
                Date.from(Instant.ofEpochSecond(createdUtc)));
    }
}
