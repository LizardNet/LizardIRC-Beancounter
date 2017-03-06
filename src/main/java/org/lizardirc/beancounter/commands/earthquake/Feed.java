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

package org.lizardirc.beancounter.commands.earthquake;

import java.util.HashMap;
import java.util.Map;

enum Feed {
    FEED_ALL_EARTHQUAKES(1, "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson", "USGS, All Earthquakes"),
    FEED_MAGNITUDE_1_0(2, "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_day.geojson", "USGS, Earthquakes Magnitude 1.0+"),
    FEED_MAGNITUDE_2_5(3, "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson", "USGS, Earthquakes Magnitude 2.5+"),
    FEED_MAGNITUDE_4_5(4, "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson", "USGS, Earthquakes Magnitude 4.5+"),
    FEED_SIGNIFICANT_EARTHQUAKES(5, "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.geojson", "USGS, Significant Earthquakes");

    private final int intValue;
    private final String url;
    private final String humanName;
    private static final Map<Integer, Feed> intMap = new HashMap<>();

    static {
        for (Feed value : Feed.values()) {
            intMap.put(value.intValue, value);
        }
    }

    Feed(int intValue, String url, String humanName) {
        this.intValue = intValue;
        this.url = url;
        this.humanName = humanName;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return humanName;
    }

    public static Feed fromInt(int i) throws IllegalArgumentException {
        if (intMap.containsKey(i)) {
            return intMap.get(i);
        } else {
            throw new IllegalArgumentException("Invalid feed integer value");
        }
    }

    public int toInt() {
        return intValue;
    }
}
