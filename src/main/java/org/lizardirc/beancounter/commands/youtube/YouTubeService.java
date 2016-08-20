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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;

import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.utils.IrcColors;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class YouTubeService {
    private static final String PERSISTENCE_APIKEY = "YouTubeApiKey";
    private static final String PERSISTENCE_ENABLED = "YouTubeEnabled";

    private String apiKey = "";
    private boolean enabled = false;

    private final PersistenceManager pm;

    public YouTubeService(PersistenceManager pm) {
        this.pm = pm;

        enabled = pm.getBoolean(PERSISTENCE_ENABLED).orElse(false);
        apiKey = pm.get(PERSISTENCE_APIKEY).orElse(null);
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        pm.set(PERSISTENCE_APIKEY, apiKey);

        if (apiKey == null || apiKey.equals("")) {
            // No API key = no worky 4 u.
            setEnabled(false);
        }
    }

    void setEnabled(boolean enabled) {
        if (enabled && (apiKey == null || apiKey.equals(""))) {
            throw new IllegalArgumentException("Cannot enable YouTube operations without an API key!");
        }

        this.enabled = enabled;
        pm.setBoolean(PERSISTENCE_ENABLED, enabled);
    }

    boolean isEnabled() {
        return enabled;
    }

    String getVideoInformation(String identifier) throws URISyntaxException, IOException {
        // QUOTA = 1 (base) + 0 (id) + 2 (statistics) + 2 (snippet) + 2 (contentDetails) = 7
        consumeQuota(7);

        URI apiCall = new URIBuilder("https://www.googleapis.com/youtube/v3/videos")
                .addParameter("part", "id,snippet,statistics,contentDetails")
                .addParameter("id", identifier)
                .addParameter("key", apiKey).build();

        String httpData = Miscellaneous.getHttpData(apiCall);

        JsonArray items = new JsonParser().parse(httpData)
                .getAsJsonObject()
                .getAsJsonArray("items");

        if (items.size() != 1) {
            // TODO: umm... wat
            return null;
        }

        JsonObject video = items.get(0).getAsJsonObject();

        JsonObject snippet = video.getAsJsonObject("snippet");
        Date publishedAt = new DateTime(snippet.get("publishedAt").getAsString()).toDate();
        String title = snippet.get("title").getAsString();
        String channelTitle = snippet.get("channelTitle").getAsString();

        JsonObject statistics = video.getAsJsonObject("statistics");
        long viewCount = statistics.get("viewCount").getAsLong();
        long likeCount = statistics.get("likeCount").getAsLong();
        long dislikeCount = statistics.get("dislikeCount").getAsLong();
        long commentCount = statistics.get("commentCount").getAsLong();

        JsonObject contentDetails = video.getAsJsonObject("contentDetails");
        Duration duration = Duration.parse(contentDetails.get("duration").getAsString());  // ISO 8601

        long totalSeconds = duration.getSeconds();
        long totalMinutes = totalSeconds / 60;
        long totalHours = totalMinutes / 60;

        long minutes = totalMinutes % 60;
        long seconds = totalSeconds % 60;

        String durationComponent;
        if(totalHours > 0){
            durationComponent = String.format("%1$02d:%2$02d:%3$02d", totalHours, minutes, seconds);
        }else{
            durationComponent = String.format("%2$02d:%3$02d", totalHours, minutes, seconds);
        }

        return String.format(
                "[YouTube] Title: %1$s | Uploader: %2$s | Uploaded: %3$tF %3$tT | Duration: %4$s | Views: %5$d | Comments: %6$d | %9$s%7$s+%11$s | %10$s%8$s-%11$s",
                title,
                channelTitle,
                publishedAt,
                durationComponent,
                viewCount,
                commentCount,
                likeCount,
                dislikeCount,
                IrcColors.GREEN,
                IrcColors.RED,
                IrcColors.RESET);
    }

    private void consumeQuota(int value) {
        // TODO: something.
    }
}
