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

package org.lizardirc.beancounter.games.testgame;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.gameframework.Game;
import org.lizardirc.beancounter.gameframework.GameEntrypoint;
import org.lizardirc.beancounter.gameframework.GameHandler;
import org.lizardirc.beancounter.gameframework.playermanagement.Player;
import org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager;
import org.lizardirc.beancounter.utils.IrcColors;

@GameEntrypoint(properName = "The Testing Game!", commandName = "testgame", minimumPlayers = 1, maximumPlayers = 1)
@SuppressWarnings("unused")
public class TestGame implements Game {
    private final GameHandler gameHandler;
    private final Channel channel;

    private boolean magicWord = false;

    public TestGame(GameHandler gameHandler, PlayerManager playerManager, Channel channel) {
        this.gameHandler = gameHandler;
        this.channel = channel;
    }

    @Override
    public Set<String> getSetupPhaseCommands(GenericMessageEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return ImmutableSet.of("magic");
        }

        return Collections.emptySet();
    }

    @Override
    public void handleSetupPhaseCommand(GenericMessageEvent event, Player player, List<String> commands, String remainder) {
        if (commands.size() == 1) {
            if ("magic".equals(commands.get(0))) {
                magicWord = true;
                event.respond("You used the ?magic command!");
            }
        }
    }

    @Override
    public Set<String> getActivePhaseCommands(GenericMessageEvent event, List<String> commands) {
        if (commands.size() == 0) {
            return ImmutableSet.of("iwin");
        }

        return Collections.emptySet();
    }

    @Override
    public void handleActivePhaseCommand(GenericMessageEvent event, Player player, List<String> commands, String remainder) {
        if (commands.size() == 1) {
            switch (commands.get(0)) {
                case "iwin":
                    event.respond("You win!");
                    gameHandler.signalGameStopped(this);
                    break;
            }
        }
    }

    @Override
    public void gameStart(GenericMessageEvent event, ScheduledExecutorService scheduledExecutorService) {
        GameHandler.sendChannelMessage(event, "Game started!  Welcome to " + IrcColors.BOLD + "The Testing Game!" + IrcColors.BOLD + "");
        GameHandler.sendChannelMessage(event, "To win, the player must simply type ?iwin");
    }

    @Override
    public boolean isGameStartable(GenericMessageEvent event) {
        if (magicWord) {
            return true;
        } else {
            event.respond("You didn't say the ?magic command!");
            return false;
        }
    }

    @Override
    public void gameHaltRequest(GenericMessageEvent event) {
        GameHandler.sendChannelMessage(event, "Game halted.  Nobody wins!");
    }

    @Override
    public void handlePlayerQuit(Player player) {
        GameHandler.sendChannelMessage(channel, "Wow, r00d.  " + IrcColors.BOLD + player.getNick() + IrcColors.BOLD + " somehow managed to lose the testing game.");
        gameHandler.signalGameStopped(this);
    }

    @Override
    public void playersCommandHook(GenericMessageEvent event) {
        event.respond("Hook test!");
    }
}
