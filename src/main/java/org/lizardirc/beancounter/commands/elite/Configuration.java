/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.elite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class Configuration {
    private HashSet<String> enabledChannels;
    private HashMap<String, HashSet<String>> trackedCommanders;
    private HashSet<String> channelsTrackingHutton;

    public static Configuration init() {
        Configuration retval = new Configuration();

        retval.enabledChannels = new HashSet<>();
        retval.trackedCommanders = new HashMap<>();
        retval.channelsTrackingHutton = new HashSet<>();

        return retval;
    }

    public Set<String> getEnabledChannels() {
        return ImmutableSet.copyOf(enabledChannels);
    }

    /**
     * Do not modify the result returned by this method in any way!
     *
     * @return The tracked commanders map, channel -> set of commander names
     */
    public Map<String, HashSet<String>> getTrackedCommandersMap() {
        return trackedCommanders;
    }

    public boolean isChannelEnabled(String channel) {
        return enabledChannels.contains(channel.toLowerCase());
    }

    public Set<String> commandersTrackedIn(String channel) {
        return trackedCommanders.get(channel.toLowerCase());
    }

    public void trackCommander(String channel, String cmdr) {
        channel = channel.toLowerCase();

        if (!trackedCommanders.containsKey(channel)) {
            trackedCommanders.put(channel, new HashSet<>());
        }
        trackedCommanders.get(channel.toLowerCase()).add(cmdr.toLowerCase());
    }

    public void untrackCommander(String channel, String cmdr) {
        channel = channel.toLowerCase();

        if (trackedCommanders.containsKey(channel)) {
            trackedCommanders.get(channel).remove(cmdr.toLowerCase());
        }
    }

    public boolean isChannelTrackingHutton(String channel) {
        return channelsTrackingHutton.contains(channel.toLowerCase());
    }

    public void enableOutputIn(String channel) {
        channel = channel.toLowerCase();

        enabledChannels.add(channel);

        if (!trackedCommanders.containsKey(channel)) {
            trackedCommanders.put(channel, new HashSet<>());
        }
    }

    public void disableOutputIn(String channel) {
        enabledChannels.remove(channel.toLowerCase());
    }

    public Set<String> getChannelsTrackingHutton() {
        return channelsTrackingHutton;
    }

    public void enableTrackingHutton(String channel) {
        channelsTrackingHutton.add(channel.toLowerCase());
    }

    public void disableTrackingHutton(String channel) {
        channelsTrackingHutton.remove(channel.toLowerCase());
    }
}
