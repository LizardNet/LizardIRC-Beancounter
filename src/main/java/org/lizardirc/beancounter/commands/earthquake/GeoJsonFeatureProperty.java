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

package org.lizardirc.beancounter.commands.earthquake;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

class GeoJsonFeatureProperty {
    @SerializedName("mag")
    public float magnitude;
    public String place;
    @SerializedName("time")
    public long eventTime;
    public long updated;
    public String url;
    @SerializedName("cdi")
    public float reportedIntensity;
    @SerializedName("mmi")
    public float measuredIntensity;
    public String alert;
    public String status;
    public int tsunami;
    private String ids;
    public String magType;
    public String type;

    public ZonedDateTime getEventTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTime), ZoneId.systemDefault());
    }

    public ZonedDateTime getUpdatedTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(updated), ZoneId.systemDefault());
    }

    public Stream<String> getAlternateIds() {
        return Stream.of(StringUtils.strip(ids, ", ").split(","))
            .distinct();
    }
}
