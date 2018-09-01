/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016-2020 by the LizardIRC Development Team. Some rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.lizardirc.beancounter.gameframework.playermanagement.Player;
import org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager;

public class ChannelState {
    private final Map<ScheduledFutureType, ScheduledFuture<?>> scheduledFutureMap = new HashMap<>();

    private GamePhase gamePhase;
    private Game activeGame;
    private PlayerManager playerManager;
    private boolean gameHasPmCommands;

    public ChannelState() {
        setGamePhaseInactive();
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public Game getActiveGame() {
        return activeGame;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public void setGamePhaseInactive() {
        gamePhase = GamePhase.INACTIVE;
        activeGame = null;
        if (playerManager != null) {
            playerManager.nukePlayers();
        }
        playerManager = null;
        scheduledFutureMap.values().forEach(future -> future.cancel(true));
        scheduledFutureMap.clear();
        gameHasPmCommands = false;
    }

    public void setGamePhaseSetup(Game activeGame, PlayerManager playerManager, boolean gameHasPmCommands) {
        if (!GamePhase.INACTIVE.equals(gamePhase)) {
            throw new IllegalStateException("Game phase may only advance to SETUP from INACTIVE.");
        }

        gamePhase = GamePhase.SETUP;
        this.activeGame = Objects.requireNonNull(activeGame);
        this.playerManager = Objects.requireNonNull(playerManager);
        this.gameHasPmCommands = gameHasPmCommands;
    }

    public void setGamePhaseActive() {
        if (!GamePhase.SETUP.equals(gamePhase)) {
            throw new IllegalStateException("Game phase may only advance to ACTIVE from SETUP.");
        }

        playerManager.signalGameStarted();
        gamePhase = GamePhase.ACTIVE;
    }

    public Map<ScheduledFutureType, ScheduledFuture<?>> getScheduledFutureMap() {
        return scheduledFutureMap;
    }

    public List<String> getAllPlayerNicks() {
        return playerManager.getIngamePlayerList().stream()
            .map(Player::getNick)
            .collect(Collectors.toList());
    }

    public boolean gameHasPmCommands() {
        return gameHasPmCommands;
    }
}
