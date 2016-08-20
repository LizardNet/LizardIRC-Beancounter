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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;

import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class YouTubeService {
    private static final String PERSISTENCE_API_KEY = "YouTubeApiKey";
    private static final String PERSISTENCE_ENABLED = "YouTubeEnabled";
    private static final String PERSISTENCE_RATE_LIMIT_START = "YouTubeRateLimitDate";
    private static final String PERSISTENCE_RATE_LIMIT_REMAINING = "YouTubeRateLimitRemaining";

    private static final int QUOTA_MAX = 1000000;

    private String apiKey = "";
    private boolean enabled = false;

    private ZonedDateTime rateLimitDate;
    private int rateLimitRemaining;

    private final PersistenceManager pm;

    public YouTubeService(PersistenceManager pm) {
        this.pm = pm;

        enabled = pm.getBoolean(PERSISTENCE_ENABLED).orElse(false);
        apiKey = pm.get(PERSISTENCE_API_KEY).orElse(null);

        // Get the last midnight PT. (because google.)
        ZonedDateTime lastRolloverTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/Los_Angeles"))
                .truncatedTo(ChronoUnit.DAYS);

        // Get the epoch-based time of the last rollover
        Optional<Long> persistedRateLimitStart = pm.getLong(PERSISTENCE_RATE_LIMIT_START);
        if (persistedRateLimitStart.isPresent()) {
            // Reconstruct the stored rate limit.
            this.rateLimitDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(persistedRateLimitStart.get()),
                    ZoneOffset.UTC);

            // Check this hasn't rolled over:
            if (this.rateLimitDate.isBefore(lastRolloverTime)) {
                // Rollover! reset.
                this.rateLimitDate = lastRolloverTime;
                this.rateLimitRemaining = QUOTA_MAX;
            } else {
                this.rateLimitRemaining = pm.getInt(PERSISTENCE_RATE_LIMIT_REMAINING).orElse(QUOTA_MAX);
            }
        } else {
            this.rateLimitDate = lastRolloverTime;
            this.rateLimitRemaining = QUOTA_MAX;
        }

        saveRateLimits();
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        pm.set(PERSISTENCE_API_KEY, apiKey);

        if (apiKey == null || apiKey.isEmpty()) {
            // No API key = no worky 4 u.
            setEnabled(false);
        }

        pm.sync();
    }

    void setEnabled(boolean enabled) {
        if (enabled && (apiKey == null || apiKey.equals(""))) {
            throw new IllegalStateException("Cannot enable YouTube operations without an API key!");
        }

        this.enabled = enabled;
        pm.setBoolean(PERSISTENCE_ENABLED, enabled);

        pm.sync();
    }

    boolean isEnabled() {
        return enabled;
    }

    public YouTubeVideo getVideoInformation(String identifier) throws URISyntaxException, IOException {
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
            // not found.
            return null;
        }

        JsonObject video = items.get(0).getAsJsonObject();

        JsonObject snippet = video.getAsJsonObject("snippet");
        JsonObject statistics = video.getAsJsonObject("statistics");
        JsonObject contentDetails = video.getAsJsonObject("contentDetails");

        return constructVideoObject(identifier, snippet, statistics, contentDetails);
    }

    private YouTubeVideo getVideoInformationFromPartial(String identifier, JsonObject snippet) throws URISyntaxException, IOException {
        // QUOTA = 1 (base) + 2 (statistics) + 2 (contentDetails) = 5
        consumeQuota(5);

        URI apiCall = new URIBuilder("https://www.googleapis.com/youtube/v3/videos")
                .addParameter("part", "statistics,contentDetails")
                .addParameter("id", identifier)
                .addParameter("key", apiKey).build();

        String httpData = Miscellaneous.getHttpData(apiCall);

        JsonArray items = new JsonParser().parse(httpData)
                .getAsJsonObject()
                .getAsJsonArray("items");

        if (items.size() != 1) {
            // not found.
            return null;
        }

        JsonObject video = items.get(0).getAsJsonObject();
        JsonObject statistics = video.getAsJsonObject("statistics");
        JsonObject contentDetails = video.getAsJsonObject("contentDetails");

        return constructVideoObject(identifier, snippet, statistics, contentDetails);
    }

    private YouTubeVideo constructVideoObject(String identifier, JsonObject snippet, JsonObject statistics, JsonObject contentDetails) {
        ZonedDateTime publishedAt = ZonedDateTime.parse(snippet.get("publishedAt").getAsString());
        String title = snippet.get("title").getAsString();
        String channelTitle = snippet.get("channelTitle").getAsString();

        long viewCount = statistics.get("viewCount").getAsLong();
        long likeCount = statistics.get("likeCount").getAsLong();
        long dislikeCount = statistics.get("dislikeCount").getAsLong();
        long commentCount = statistics.get("commentCount").getAsLong();


        Duration duration = Duration.parse(contentDetails.get("duration").getAsString());  // ISO 8601

        return new YouTubeVideo(identifier,
                title,
                channelTitle,
                publishedAt,
                duration,
                viewCount,
                likeCount,
                dislikeCount,
                commentCount);
    }

    YouTubeVideo search(String query) throws URISyntaxException, IOException {
        consumeQuota(100); // OUCH.

        URI apiCall = new URIBuilder("https://www.googleapis.com/youtube/v3/search")
                .addParameter("part", "id,snippet")
                .addParameter("maxResults", "1")
                .addParameter("type", "video")
                .addParameter("q", query)
                .addParameter("key", apiKey).build();

        String httpData = Miscellaneous.getHttpData(apiCall);

        JsonArray items = new JsonParser().parse(httpData)
                .getAsJsonObject()
                .getAsJsonArray("items");

        if (items.size() == 0) {
            // not found.
            return null;
        }

        JsonObject video = items.get(0).getAsJsonObject();
        JsonObject id = video.getAsJsonObject("id");
        String videoId = id.get("videoId").getAsString();

        JsonObject snippet = video.getAsJsonObject("snippet");

        // No, we can't return all the information we need from the search result. :|
        return getVideoInformationFromPartial(videoId, snippet);
    }

    private void consumeQuota(int value) throws RateLimitExceededException {
        // Get the last midnight PT. (because google.)
        ZonedDateTime lastRolloverTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/Los_Angeles"))
                .truncatedTo(ChronoUnit.DAYS);

        // Check this hasn't rolled over:
        if (this.rateLimitDate.isBefore(lastRolloverTime)) {
            // Rollover! reset.
            this.rateLimitDate = lastRolloverTime;
            this.rateLimitRemaining = QUOTA_MAX;
        }

        if (rateLimitRemaining < value) {
            throw new RateLimitExceededException();
        }

        rateLimitRemaining -= value;

        saveRateLimits();
    }

    private void saveRateLimits() {
        pm.setLong(PERSISTENCE_RATE_LIMIT_START, rateLimitDate.toInstant().getEpochSecond());
        pm.setInt(PERSISTENCE_RATE_LIMIT_REMAINING, rateLimitRemaining);

        pm.sync();
    }
}
