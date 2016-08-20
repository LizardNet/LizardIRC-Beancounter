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

package org.lizardirc.beancounter.commands.youtube;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.lizardirc.beancounter.utils.IrcColors;

public class YouTubeVideo {
    private final String id;
    private final String title;
    private final String channelTitle;
    private final ZonedDateTime publishedAt;
    private final Duration duration;
    private final long viewCount;
    private final long likeCount;
    private final long dislikeCount;
    private final long commentCount;
    private final boolean commentsEnabled;

    YouTubeVideo(String id, String title, String channelTitle, ZonedDateTime publishedAt, Duration duration, long viewCount, long likeCount, long dislikeCount, long commentCount, boolean commentsEnabled) {
        this.id = id;
        this.title = title;
        this.channelTitle = channelTitle;
        this.publishedAt = publishedAt;
        this.duration = duration;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.commentCount = commentCount;
        this.commentsEnabled = commentsEnabled;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        long totalSeconds = duration.getSeconds();
        long totalMinutes = totalSeconds / 60;
        long totalHours = totalMinutes / 60;

        long minutes = totalMinutes % 60;
        long seconds = totalSeconds % 60;

        String durationComponent;
        if (totalHours > 0) {
            durationComponent = String.format("%1$02d:%2$02d:%3$02d", totalHours, minutes, seconds);
        } else {
            durationComponent = String.format("%2$02d:%3$02d", totalHours, minutes, seconds);
        }

        String comments;
        if(commentsEnabled){
            comments = String.format("Comments: %1$d", commentCount);
        } else {
            comments = "Comments disabled";
        }

        return String.format(
                "[YouTube] Title: %1$s | Uploader: %2$s | Uploaded: %3$tF %3$tT | Duration: %4$s | Views: %5$d | %6$s | %9$s%7$s+%11$s | %10$s%8$s-%11$s",
                title,
                channelTitle,
                publishedAt,
                durationComponent,
                viewCount,
                comments,
                likeCount,
                dislikeCount,
                IrcColors.GREEN,
                IrcColors.RED,
                IrcColors.RESET);
    }
}
