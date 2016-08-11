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

import java.util.Objects;

import org.pircbotx.User;

/**
 * Warning: This class has a natural ordering inconsistent with its equals()!
 */
public class Player implements Comparable<Player> {
    private final String username;
    private final String host;

    private String nickname;

    public Player(User user) {
        Objects.requireNonNull(user);

        nickname = user.getNick();
        username = user.getLogin();
        host = user.getHostmask();
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase(), host.toLowerCase());
    }

    public boolean canEquals(Object o) {
        return o instanceof Player;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;

        if (o instanceof Player) {
            Player that = (Player) o;
            result = that.canEquals(this) &&
                nickname.equalsIgnoreCase(that.nickname) &&
                username.equalsIgnoreCase(that.username) &&
                host.equalsIgnoreCase(that.host);
        }

        return result;
    }

    public boolean equalsIgnoreNick(Player that) {
        return username.equalsIgnoreCase(that.username) &&
            host.equalsIgnoreCase(that.host);
    }

    public String getNick() {
        return getNickname();
    }

    public String getNickname() {
        return nickname;
    }

    void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public int compareTo(Player o) {
        return nickname.compareToIgnoreCase(o.nickname);
    }
}
