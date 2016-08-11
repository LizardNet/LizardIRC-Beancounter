/*
 * === BOT CODE LICENSE ===
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
 *
 * === GAME CODE LICENSE ===
 * Secret Hitler was created by Mike Boxleiter, Tommy Maranges, Max Temkin, and Mac Schubert.
 * <http://www.secrethitler.com/>
 *
 * Secret Hitler is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 *
 * YOU ARE FREE TO:
 * • Share
 *  — copy and redistribute the game in any medium or format
 * • Adapt
 *  — remix, transform, and build upon the game
 *
 * UNDER THE FOLLOWING TERMS:
 * • Attribution
 *  — If you make something using our game, you need to give us credit and link back to us, and you need to explain what
 *    you changed.
 * • Non-Commercial
 *  — You can’t use our game to make money.
 * • Share Alike
 *  — If you remix, transform, or build upon our game, you have to release your work under the same Creative Commons
 *    license that we use (BY-NC-SA 4.0).
 * • No additional restrictions
 *  — You can’t apply legal terms or technological measures to your work that legally restrict others from doing anything
 *    our license allows. That means you can’t submit anything using our game to any app store without our approval
 *
 * You can learn more about Creative Commons at CreativeCommons.org. (Our license is available at
 * CreativeCommons.org/licenses/by-nc-sa/4.0/legalcode).
 *
 * == SUMMARY OF CHANGES MADE FROM ORIGINAL GAME IN BEANCOUNTER ==
 * The changes made from the original game in Beancounter are basically those necessary to make the game work on the IRC
 * medium.  As much as possible, gameplay follows the exact same rules as the original game.
 */

package org.lizardirc.beancounter.games.secrethitler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.lizardirc.beancounter.games.handler.Player;

class GameState {
    private final List<Player> playerList;

    private int playerCountAtGameStart;

    private int roundNumber = 1;
    private RoundPhase roundPhase = RoundPhase.ELECTION_NOMINATION;

    private int failedGovernments = 0;

    private int presidentialCandidatePtr = 0;
    private Player presidentialCandidate;

    // These players are ineligible to become the next Chancellor
    private Player previousElectedPresident = null;
    private Player previousElectedChancellor = null;

    private Map<Player, Boolean> votingMap = null;

    public GameState(List<Player> playerList) {
        this.playerList = Objects.requireNonNull(playerList);
    }

    public void gameStartHook() {
        if (playerCountAtGameStart != 0) {
            throw new IllegalStateException("gameStartHook() may not be called more than once");
        } else {
            playerCountAtGameStart = playerList.size();
        }
    }

    public int getPlayerCountAtGameStart() {
        return playerCountAtGameStart;
    }

    public Player getPresidentialCandidate() {
        return presidentialCandidate;
    }

    public void setNextPresidentialCandidateSequentially() {
        presidentialCandidatePtr++;
        if (presidentialCandidatePtr >= playerList.size()) {
            presidentialCandidatePtr = 0;
        }

        presidentialCandidate = playerList.get(presidentialCandidatePtr);
    }

    public void setNextPresidentialCandidateTo(Player player) {
        presidentialCandidate = player;
    }

    public void setNextPresidentialCandidateRandomly() {
        // Only used at game start
        Random random = new Random();
        presidentialCandidatePtr = random.nextInt(playerList.size());
        presidentialCandidate = playerList.get(presidentialCandidatePtr);
    }

    public void governmentFailed() {
        failedGovernments++;
    }

    public boolean isGovernmentInChaos() {
        return failedGovernments >= 3;
    }

    public int getFailedGovernments() {
        return failedGovernments;
    }

    public RoundPhase getRoundPhase() {
        return roundPhase;
    }

    public void nextPhase() {
        switch (roundPhase) {
            case ELECTION_NOMINATION:
                roundPhase = RoundPhase.ELECTION_VOTE;
                votingMap = new HashMap<>();
                break;
            case ELECTION_VOTE:
                roundPhase = RoundPhase.LEGISLATIVE_SESSION;
                votingMap = null;
                break;
            case LEGISLATIVE_SESSION:
                roundPhase = RoundPhase.EXECUTIVE_ACTION;
                break;
            case EXECUTIVE_ACTION:
                roundNumber++;
                roundPhase = RoundPhase.ELECTION_NOMINATION;
                break;
        }
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isChancellorCandidateEligible(Player player) {
        Objects.requireNonNull(player);

        return !(player.equals(previousElectedPresident) || player.equals(previousElectedChancellor));
    }

    public Player getPreviousElectedPresident() {
        return previousElectedPresident;
    }

    public Player getPreviousElectedChancellor() {
        return previousElectedChancellor;
    }

    public void resetFailedGovernments() {
        failedGovernments = 0;
    }

    public void executePlayer(Player player) {
        int executedPlayersOrdinalPosition = playerList.indexOf(player);

        if (executedPlayersOrdinalPosition == -1) {
            throw new IllegalArgumentException("Specified player is not in game.");
        }

        if (presidentialCandidatePtr >= executedPlayersOrdinalPosition) {
            presidentialCandidatePtr--;
        }

        playerList.remove(player);
    }

    public Map<Player, Boolean> getVotingMap() {
        if (RoundPhase.ELECTION_VOTE.equals(roundPhase)) {
            return votingMap;
        } else {
            throw new IllegalStateException("The voting map is only accessible during the ELECTION_VOTE phase.");
        }
    }
}
