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

package org.lizardirc.beancounter.games.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.pircbotx.PircBotX;

public class ChannelState<T extends PircBotX> {
    private final Map<ScheduledFutureType, ScheduledFuture> scheduledFutureMap = new HashMap<>();

    private GamePhase gamePhase;
    private Game<T> activeGame;
    private List<Player> players;

    public ChannelState() {
        setGamePhaseInactive();
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public Game<T> getActiveGame() {
        return activeGame;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setGamePhaseInactive() {
        gamePhase = GamePhase.INACTIVE;
        activeGame = null;
        players = Collections.emptyList();
        scheduledFutureMap.values().forEach(future -> future.cancel(true));
        scheduledFutureMap.clear();
    }

    public void setGamePhaseSetup(Game<T> activeGame, List<Player> players) {
        if (!GamePhase.INACTIVE.equals(gamePhase)) {
            throw new IllegalStateException("Game phase may only advance to SETUP from INACTIVE.");
        }

        gamePhase = GamePhase.SETUP;
        this.activeGame = Objects.requireNonNull(activeGame);
        this.players = players;
    }

    public void setGamePhaseActive() {
        if (!GamePhase.SETUP.equals(gamePhase)) {
            throw new IllegalStateException("Game phase may only advance to ACTIVE from SETUP.");
        }

        gamePhase = GamePhase.ACTIVE;
    }

    public Map<ScheduledFutureType, ScheduledFuture> getScheduledFutureMap() {
        return scheduledFutureMap;
    }

    public List<String> getAllPlayerNicks() {
        return players.stream()
            .map(Player::getNick)
            .collect(Collectors.toList());
    }

    public String updatePlayerNickname(Player player, String newNickname) throws IllegalArgumentException {
        if (players.contains(player)) {
            Player oldPlayer = players.get(players.indexOf(player));
            String oldNick = oldPlayer.getNick();
            oldPlayer.setNickname(newNickname);
            return oldNick;
        } else {
            throw new IllegalArgumentException("No such player is in the game!");
        }
    }

    List<Player> initializePlayerList() {
        return Collections.synchronizedList(new ArrayList<>());
    }
}
