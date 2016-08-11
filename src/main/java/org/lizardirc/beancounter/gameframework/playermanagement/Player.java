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

package org.lizardirc.beancounter.gameframework.playermanagement;

import java.util.Objects;
import java.util.UUID;

/**
 * Warning: This class has a natural ordering inconsistent with its equals()!
 */
public class Player implements Comparable<Player> {
    private final UUID uuid; // I'm so sorry
    private final PlayerManager producingFactory;

    private boolean playerRemoved = false;

    Player(PlayerManager producingFactory) {
        this.producingFactory = Objects.requireNonNull(producingFactory);

        uuid = UUID.randomUUID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, producingFactory);
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
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(producingFactory, that.producingFactory);
        }

        return result;
    }

    public String getNick() {
        return producingFactory.getNicknameOf(this);
    }

    @Deprecated
    public String getNickname() {
        return getNick();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Player o) {
        return getNick().compareToIgnoreCase(o.getNick());
    }

    public boolean isPlayerRemoved() {
        return playerRemoved;
    }

    public boolean isPlayerIngame() {
        return !playerRemoved; // Just to make things more idiomatic
    }

    void setPlayerRemoved(boolean playerRemoved) {
        this.playerRemoved = playerRemoved;
    }

    PlayerManager getProducingFactory() {
        return producingFactory;
    }
}
