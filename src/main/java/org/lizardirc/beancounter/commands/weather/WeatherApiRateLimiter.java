/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.weather;

import org.lizardirc.beancounter.persistence.PersistenceManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

class WeatherApiRateLimiter {
    private final PersistenceManager pm;

    private int apiCallsPerDay;
    private int apiCallsPerMinute;
    private int apiBurstSize;
    private LocalDateTime startOfApiMinute;
    private ZonedDateTime startOfApiDay;
    private int apiCallsDayRemaining;
    private int apiCallsMinuteRemaining;
    private int apiCallsThisBurst;
    private LocalDateTime burstRateLimitStart;
    private double burstRateLimitStandoffSeconds;

    public WeatherApiRateLimiter(PersistenceManager pm) {
        this.pm = pm;
        apiCallsPerDay = pm.getInt("apiCallsPerDay").orElse(0);
        apiCallsPerMinute = pm.getInt("apiCallsPerMinute").orElse(0);
        apiBurstSize = pm.getInt("apiBurstSize").orElse(1);

        apiCallsMinuteRemaining = apiCallsPerMinute;
        startOfApiMinute = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

        apiCallsDayRemaining = (pm.getInt("apiCallsDayRemaining").orElse(apiCallsPerDay));

        Optional<Long> tempStartOfApiDay = pm.getLong("startOfApiDay");
        if (tempStartOfApiDay.isPresent()) {
            startOfApiDay = ZonedDateTime.ofInstant(Instant.ofEpochSecond(tempStartOfApiDay.get()), ZoneId.of("America/New_York"));
        } else {
            startOfApiDay = ZonedDateTime.now(ZoneId.of("America/New_York"));
            startOfApiDay = startOfApiDay.withSecond(0).withMinute(0).withHour(0);
        }

        apiCallsThisBurst = 0;

        burstRateLimitStart = LocalDateTime.now();
        burstRateLimitStandoffSeconds = 0;
    }

    public int getApiCallsPerDay() {
        return apiCallsPerDay;
    }

    public int getApiCallsPerMinute() {
        return apiCallsPerMinute;
    }

    public int getApiBurstSize() {
        return apiBurstSize;
    }

    public int getApiCallsDayRemaining() {
        return apiCallsDayRemaining;
    }

    public int getApiCallsMinuteRemaining() {
        return apiCallsMinuteRemaining;
    }

    public int getApiCallsThisBurst() {
        return apiCallsThisBurst;
    }

    public double getBurstRateLimitStandoffSeconds() {
        return burstRateLimitStandoffSeconds;
    }

    public void setApiCallsPerDay(int apiCallsPerDay) {
        this.apiCallsPerDay = apiCallsPerDay;
        apiCallsDayRemaining = apiCallsPerDay;
        burstRateLimitStandoffSeconds = 0;
    }

    public void setApiCallsPerMinute(int apiCallsPerMinute) {
        this.apiCallsPerMinute = apiCallsPerMinute;
        apiCallsMinuteRemaining = apiCallsPerMinute;
    }

    public void setApiBurstSize(int apiBurstSize) {
        this.apiBurstSize = apiBurstSize;
    }

    public long getTimeRemainingThisMinute() {
        return 60 - startOfApiMinute.until(LocalDateTime.now(), ChronoUnit.SECONDS);
    }

    public long getTimeRemainingThisDay() {
        return 86400 - startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
    }

    public double getTimeRemainingThisBurstPeriod() {
        return burstRateLimitStandoffSeconds - (burstRateLimitStart.until(LocalDateTime.now(), ChronoUnit.SECONDS));
    }

    public void reset() {
        apiCallsDayRemaining = apiCallsPerDay;
        apiCallsMinuteRemaining = apiCallsPerMinute;
        burstRateLimitStandoffSeconds = 0;
    }

    public void check() throws ApiRateLimitException {
        // First, check the hard per-minute ratelimit
        if (startOfApiMinute.until(LocalDateTime.now(), ChronoUnit.SECONDS) >= 60) {
            startOfApiMinute = LocalDateTime.now();
            apiCallsMinuteRemaining = apiCallsPerMinute;
        }

        if (apiCallsMinuteRemaining > 0) {
            apiCallsMinuteRemaining--;
        } else {
            long timeSinceMinuteStart = startOfApiMinute.until(LocalDateTime.now(), ChronoUnit.SECONDS);
            throw new ApiRateLimitException("Per-minute rate limit exceeded; try again in " + (60 - timeSinceMinuteStart) + " seconds");
        }

        // Next, check the hard per-day ratelimit
        if (startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS) >= 86400) {
            startOfApiDay = ZonedDateTime.now(ZoneId.of("America/New_York"));
            startOfApiDay = startOfApiDay.withSecond(0).withMinute(0).withHour(0);
            apiCallsDayRemaining = apiCallsPerDay;
            apiCallsThisBurst = 0;
        }

        if (apiCallsDayRemaining > 0) {
            apiCallsDayRemaining--;
        } else {
            apiCallsMinuteRemaining++; // "Refund" the per-minute API call
            long timeSinceDayStart = startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
            throw new ApiRateLimitException("Per-day rate limit exceeded; try again in " + (86400 - timeSinceDayStart) + " seconds");
        }

        // Finally, check the progressive burst-based ratelimit
        // First, check if we're currently in a burst period
        if (burstRateLimitStandoffSeconds - (burstRateLimitStart.until(LocalDateTime.now(), ChronoUnit.SECONDS)) > 0) {
            // Yes, we're in a burst period.  Check if we've sent more API calls than allowed in a burst; if so, stand off
            if (apiCallsThisBurst >= apiBurstSize) {
                // We're standing off, refund the daily and minutely API calls and throw an exception
                apiCallsDayRemaining++;
                apiCallsMinuteRemaining++;
                throw new ApiRateLimitException("Progressive rate limit exceeded; try again in " +
                    (burstRateLimitStandoffSeconds - (burstRateLimitStart.until(LocalDateTime.now(), ChronoUnit.SECONDS))) + " seconds");
            } else {
                // We're not standing off, but we're still in a burst period.
                apiCallsThisBurst++;
            }
        } else {
            // We're not currently in a burst period.  Reset the burst count, and begin a new burst period.
            apiCallsThisBurst = 1;
            // Calculate the number of bursts remaining today, rounded up
            double todayRemainingBursts = Math.ceil((double) (apiCallsDayRemaining + 1) / apiBurstSize);
            // Calculate the number of seconds between bursts, rounded down
            long timeSinceDayStart = startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
            double secondsBetweenBursts = Math.floor((86400 - timeSinceDayStart) / todayRemainingBursts);
            burstRateLimitStart = LocalDateTime.now();
            burstRateLimitStandoffSeconds = secondsBetweenBursts;
        }

        sync();
    }

    public synchronized void sync() {
        pm.setInt("apiCallsPerDay", apiCallsPerDay);
        pm.setInt("apiCallsPerMinute", apiCallsPerMinute);
        pm.setInt("apiBurstSize", apiBurstSize);
        pm.setInt("apiCallsDayRemaining", apiCallsDayRemaining);
        pm.setLong("startOfApiDay", startOfApiDay.toEpochSecond());
        pm.sync();
    }
}
