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

import com.google.gson.annotations.SerializedName;

public class RedditPost {
    String subreddit;
    String id;
    String author;
    String title;
    String url;
    int gilded;
    int score;
    @SerializedName("num_comments")
    int numComments;
    int ups;
    int downs;
    @SerializedName("created_utc")
    int createdUtc;
    @SerializedName("over_18")
    boolean isNsfw;
    @SerializedName("is_self")
    boolean isSelf;

    public String getUrl() {
        return url;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public int getGilded() {
        return gilded;
    }

    public int getScore() {
        return score;
    }

    public int getNumComments() {
        return numComments;
    }

    public int getUps() {
        return ups;
    }

    public int getDowns() {
        return downs;
    }

    public int getCreatedUtc() {
        return createdUtc;
    }

    public boolean isNsfw() {
        return isNsfw;
    }

    public boolean isSelf() {
        return isSelf;
    }
}
