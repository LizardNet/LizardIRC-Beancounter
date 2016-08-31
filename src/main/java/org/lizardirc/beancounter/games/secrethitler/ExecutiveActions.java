/*
 * === BOT CODE LICENSE ===
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016-2017 by the LizardIRC Development Team. Some rights reserved.
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
import java.util.Map;
import java.util.function.Consumer;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;

import org.lizardirc.beancounter.gameframework.playermanagement.Player;

class ExecutiveActions<T extends PircBotX> {
    private final SecretHitler<T> parent;

    private final GameState gameState;
    private final Map<Player, PlayerState> playerStates;
    private final Channel channel;

    public ExecutiveActions(SecretHitler<T> parent) {
        this.parent = parent;
        this.gameState = parent.getGameState();
        this.playerStates = parent.getPlayerStates();
        this.channel = parent.getChannel();
    }

    public Map<Integer, ExecutiveAction> getExecutiveActionTrack(int initialPlayers) {
        Map<Integer, ExecutiveAction> retval = new HashMap<>();

        switch (initialPlayers) {
            case 5:
            case 6:
                retval.put(3, new PolicyPeek());
                retval.put(4, new Execution());
                retval.put(5, new Execution());
                break;
            case 7:
            case 8:
                retval.put(2, new InvestigateLoyalty());
                retval.put(3, new CallSpecialElection());
                retval.put(4, new Execution());
                retval.put(5, new Execution());
                break;
            case 9:
            case 10:
                retval.put(1, new InvestigateLoyalty());
                retval.put(2, new InvestigateLoyalty());
                retval.put(3, new CallSpecialElection());
                retval.put(4, new Execution());
                retval.put(5, new Execution());
                break;
        }

        return retval;
    }

    interface ExecutiveAction extends Consumer<Player> {
        String getInstructions();

        default boolean gameEnded() {
            return false;
        }
    }

    class InvestigateLoyalty implements ExecutiveAction {
        @Override
        public void accept(Player target) {
            // Note: We don't check if a player has already been investigated here.  That must be done by the caller...

            playerStates.get(target).setInvestigated();

            channel.send().message("The President has ordered the Abwehr to investigate \002" + target.getNick() + "\002's " +
                "party loyalties....");

            channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "The Abwehr's investigation " +
                "reveals \002" + target.getNick() + "\002 to be a member of the \002" + playerStates.get(target).getRole().getParty() +
                "\002.  The Abwehr report to you alone, and you may choose what to do with this information as you wish....");
        }

        @Override
        public String getInstructions() {
            return "You may instruct the Abwehr to investigate the party loyalties of a player of your choice.  Players " +
                "may only ever be investigated once.  To use this Executive Power, use the command \"" + SecretHitler.CMD_USE_ACTION +
                " playerName\".";
        }

        @Override
        public String toString() {
            return "Investigate Loyalty";
        }
    }

    class CallSpecialElection implements ExecutiveAction {
        @Override
        public void accept(Player player) {
            channel.send().message("The President has called a Special Election, and selected \002" + player.getNick() +
                "\002 as the next Presidential candidate.  (Note: No players are skipped by this action, and after the " +
                "next round, the player after the current President will be the Presidential Candidate.)");
            gameState.setNextPresidentialCandidateTo(player);
        }

        @Override
        public String getInstructions() {
            return "You may call a Special Election and select the next Presidential candidate instead of proceeding down" +
                " the player list sequentially.  No players will be skipped; after the Special Election, the player after you " +
                "will be the next Presidential candidate.  To use this Power, use the command \"" + SecretHitler.CMD_USE_ACTION +
                " playerName\".";
        }

        @Override
        public String toString() {
            return "Call Special Election";
        }
    }

    class PolicyPeek implements ExecutiveAction {
        @Override
        public void accept(Player player) {
            channel.send().message("Abwehr Intelligence has reported to the President what the next three Policies in " +
                "the Policy Deck are....  " + gameState.getPresidentialCandidate().getNick() + ", please check your PMs.");

            StringBuilder sb = new StringBuilder("The Abwehr has investigated what the next three policies in the Policy" +
                " Deck are.  They are, in this order: ");

            Policy[] policies = gameState.doPolicyPeek();

            for (Policy policy : policies) {
                sb.append('\002')
                    .append(policy)
                    .append("\002, ");
            }

            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);

            sb.append(".  The Abwehr reports to you alone, and you may do with this information as you wish....");

            channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), sb.toString());
        }

        @Override
        public String getInstructions() {
            return null; // This power is used automatically; therefore it needs no instructions.
        }

        @Override
        public String toString() {
            return "Policy Peek";
        }
    }

    class Execution implements ExecutiveAction {
        private boolean gameEnded = false;

        @Override
        public void accept(Player player) {
            channel.send().message("The President has formally instructed the Abwehr to execute \002" + player.getNick() +
                "\002.");

            gameState.executePlayer(player);
            channel.send().setMode("-v ", player.getNick());

            if (playerStates.get(player).getRole().equals(Role.HITLER)) {
                channel.send().message("The Abwehr march \002" + player.getNick() + "\002 into the square and execute " +
                    "them by firing squad.  After the gruesome event, it is determined that the executed player was the " +
                    "\002Hitler\002!");
                channel.send().message("Game over!  The Hitler has been executed, ensuring that the Fascists will never be " +
                    "able to seize power.  The \002Liberals\002 have won the game!");
                parent.gameEndCleanupAndStatsOutput(Party.LIBERALS);
                parent.getGameHandler().signalGameStopped(parent);
                gameEnded = true;
            } else {
                channel.send().message("Following a gruesome public execution, it is determined that the executed player" +
                    " was \002not\002 the Hitler.");
            }
        }

        @Override
        public String toString() {
            return "Execution";
        }

        @Override
        public String getInstructions() {
            return "You may instruct the Abwehr to execute the player of your choice.  If you execute the Hitler, the Liberals" +
                " will win the game.  Select the player to be executed by giving the command \"" + SecretHitler.CMD_USE_ACTION +
                " playerName\".";
        }

        @Override
        public boolean gameEnded() {
            return gameEnded;
        }
    }
}


