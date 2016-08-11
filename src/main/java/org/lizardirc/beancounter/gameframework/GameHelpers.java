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

package org.lizardirc.beancounter.gameframework;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import org.pircbotx.Channel;

import org.lizardirc.beancounter.utils.Miscellaneous;

public final class GameHelpers {
    private GameHelpers() {
        throw new IllegalStateException("GameHelpers may not be instantiated.");
    }

    public static void voicePlayers(List<Player> players, Channel channel) {
        doVoiceAction('+', players, channel);
    }

    public static void devoicePlayers(List<Player> players, Channel channel) {
        doVoiceAction('-', players, channel);
    }

    private static void doVoiceAction(char operator, List<Player> players, Channel channel) {
        // More efficient to determine the number of modes we can set at a time and set the modes manually rather than
        // use PircBotX's built-in mode setting.

        int playerCount = players.size();
        int maxModes = channel.getBot().getServerInfo().getMaxModes();

        for (int i = 0; i < playerCount; i += maxModes) {
            Set<String> toVoice = new HashSet<>();

            for (int j = i; j < (i + maxModes) && j < playerCount; j++) {
                toVoice.add(players.get(j).getNick());
            }

            String v = Strings.repeat("v", toVoice.size());
            String nicks = Miscellaneous.getStringRepresentation(toVoice, " ");
            channel.getBot().sendRaw().rawLine("MODE " + channel.getName() + ' ' + operator + v + ' ' + nicks);
        }
    }

    public static Player getNormalizedPlayer(Player player, List<Player> playerList) {
        return playerList.stream()
            .filter(p -> p.equalsIgnoreNick(player))
            .findFirst()
            .orElse(null);
    }
}
