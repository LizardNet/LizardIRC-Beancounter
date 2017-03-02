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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.gameframework.Game;
import org.lizardirc.beancounter.gameframework.GameEntrypoint;
import org.lizardirc.beancounter.gameframework.GameHandler;
import org.lizardirc.beancounter.gameframework.GameHelpers;
import org.lizardirc.beancounter.gameframework.playermanagement.Player;
import org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager;
import org.lizardirc.beancounter.utils.Miscellaneous;
import org.lizardirc.beancounter.utils.Pair;

@GameEntrypoint(properName = "Secret Hitler", commandName = "secrethitler", minimumPlayers = 5, maximumPlayers = 10,
    requiresChanops = true, voicePlayersDuringSetup = true, usesPrivateMessageCommands =  true,
    summary = "A secret identity game inspired by Werewolf and Mafia", gameUrl = "http://secrethitler.com/")
@SuppressWarnings("unused")
public class SecretHitler<T extends PircBotX> implements Game<T> {
    private static final String CMD_MYROLE = "myrole";
    private static final String CMD_NOMINATE  = "nominate";
    private static final String CMD_VOTE = "vote";
    private static final String CMD_ELECTION_RESULTS = "electionresults";
    private static final String CMD_STATUS = "status";
    private static final String CMD_SHOWTRACK = "showtrack";
    private static final String CMD_DISCARD = "discard";
    private static final String CMD_ENACT = "enact";
    private static final String CMD_VETO = "veto";
    private static final String CMD_LEGISLATIVE_RESULTS = "legislativeresults";
    private static final String CMD_EXECUTIVE_ACTIONS = "executiveactions";
    static final String CMD_USE_ACTION = "useaction"; // Package-local so ExecutiveActions can use this
    private static final String VOTE_YES = "yes";
    private static final String VOTE_NO = "no";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_MYROLE, CMD_NOMINATE, CMD_VOTE, CMD_ELECTION_RESULTS,
        CMD_DISCARD, CMD_VETO, CMD_LEGISLATIVE_RESULTS, CMD_EXECUTIVE_ACTIONS, CMD_USE_ACTION, CMD_STATUS, CMD_SHOWTRACK,
        CMD_ENACT);
    private static final Set<String> VOTE_SUBCMDS = ImmutableSet.of(VOTE_YES, VOTE_NO);

    private final GameHandler<T> gameHandler;
    private final PlayerManager playerManager;
    private final Channel channel;

    private final Map<Player, PlayerState> playerStates = new HashMap<>();
    private final GameState gameState;

    private Map<Integer, ExecutiveActions.ExecutiveAction> executiveActionTrack;

    public SecretHitler(GameHandler<T> gameHandler, PlayerManager playerManager, Channel channel) {
        this.gameHandler = gameHandler;
        this.playerManager = playerManager;
        this.channel = channel;
        gameState = new GameState(this.playerManager);
    }

    @Override
    public Set<String> getActivePhaseCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1) {
            switch (commands.get(0)) {
                case CMD_NOMINATE:
                case CMD_USE_ACTION:
                    return ImmutableSet.copyOf(playerManager.getIngamePlayerList().stream()
                        .map(Player::getNick)
                        .collect(Collectors.toSet())
                    );
                case CMD_VOTE:
                case CMD_VETO:
                    return VOTE_SUBCMDS;
                case CMD_DISCARD:
                    return ImmutableSet.of("1", "2", "3");
                case CMD_ENACT:
                    return ImmutableSet.of("1", "2");
            }
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleActivePhaseCommand(GenericMessageEvent<T> event, Player player, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_MYROLE:
                // Send role information
                switch (playerStates.get(player).getRole()) {
                    case LIBERAL:
                        event.getUser().send().message(Role.LIBERAL.getDescription());
                        break;
                    case FASCIST:
                        event.getUser().send().message(Role.FASCIST.getDescription());
                        event.getUser().send().message(getFascistPartyMembersAsInformativeString());
                        break;
                    case HITLER:
                        event.getUser().send().message(Role.HITLER.getDescription());
                        sendHitlerExtraInformationIfAppropriate(event);
                        break;
                }

                // Check if we're waiting for the player to do something, and if so, remind them
                switch (gameState.getRoundPhase()) {
                    case ELECTION_NOMINATION:
                        if (player.equals(gameState.getPresidentialCandidate())) {
                            event.getUser().send().message("You are the \002Presidential Candidate\002 this election - " +
                                "please select a Chancellor Candidate by using the command \"" + CMD_NOMINATE + " player\".");
                        }
                        break;
                    case ELECTION_VOTE:
                        if (!gameState.getVotingMap().keySet().contains(player)) {
                            event.getUser().send().message("Waiting for you to vote on electing the Presidential " +
                                "candidate, \002" + gameState.getPresidentialCandidate().getNick() + "\002, and the " +
                                "Chancellor candidate, \002" + gameState.getChancellorCandidate().getNick() + "\002. " +
                                "Please PM me either \"" + CMD_VOTE + ' ' + VOTE_YES + "\" or \"" + CMD_VOTE + ' ' +
                                VOTE_NO + "\".");
                        }
                        break;
                    case LEGISLATIVE_SESSION_PRESIDENT_VOTE:
                        if (player.equals(gameState.getPresidentialCandidate())) {
                            event.getUser().send().message("You are the \002President\002 this Legislative Session.  " +
                                "Waiting for you to discard a policy:");
                            generateAndSendLegislativeSessionInfoForPresident(gameState.getLegislativeSessionPolicies());
                        } else if (player.equals(gameState.getChancellorCandidate())) {
                            event.getUser().send().message("You are the \002Chancellor\002 this Legislative Session.  " +
                                "You don't need to do anything right now; waiting on the President to discard a policy.");
                        }
                        break;
                    case LEGISLATIVE_SESSION_CHANCELLOR_VOTE:
                        if (player.equals(gameState.getChancellorCandidate())) {
                            event.getUser().send().message("You are the \002Chancellor\002 this Legislative Session.  " +
                                "Waiting for you to enact a policy:");
                            generateAndSendLegislativeSessionInfoForChancellor(gameState.getLegislativeSessionPolicies(), event);
                        } else if (player.equals(gameState.getPresidentialCandidate())) {
                            event.getUser().send().message("You are the \002President\002 this Legislative Session.  " +
                                "You don't need to do anything right now.");
                        }
                        break;
                    case LEGISLATIVE_SESSION_VETO_POWER_CHECK:
                        if (player.equals(gameState.getPresidentialCandidate())) {
                            event.getUser().send().message("You are the \002President\002 this Legislative Session.  " +
                                "Waiting for you to approve or reject the Chancellor's use of their Veto Power:");
                            sendVetoInformation(event);
                        } else if (player.equals(gameState.getChancellorCandidate())) {
                            event.getUser().send().message("You are the \002Chancellor\002 this Legislative Session.  " +
                                "You don't need to do anything right now - waiting for the President to either consent " +
                                "to or reject your request to use Veto Power.");
                        }
                        break;
                    case EXECUTIVE_ACTION:
                        if (player.equals(gameState.getPresidentialCandidate())) {
                            event.getUser().send().message("You are the \002President\002, and you have the Executive" +
                                "Action \002" + gameState.getAvailableExecutiveAction() + "\002 at your diposal!");
                            showExecutiveActionUsageInstructions(gameState.getAvailableExecutiveAction());
                        }
                        break;
                }
                break;
            case CMD_ELECTION_RESULTS:
                remainder = remainder.trim();

                if (remainder.isEmpty()) {
                    event.respond("Specify the round number to get the election results for.  You are currently in round " +
                        gameState.getRoundNumber() + ".  Syntax: \"" + CMD_ELECTION_RESULTS + " roundNumber\"");
                    return;
                }

                int round;

                try {
                    round = Integer.parseInt(remainder);
                } catch (NumberFormatException e) {
                    event.respond("Error: I didn't understand the round number you provided: " + e.getMessage());
                    return;
                }

                if (round < 1) {
                    event.respond("Error: Round number must be greater than or equal to 1");
                    return;
                }

                if (round > gameState.getRoundNumber()) {
                    event.respond("I can't predict the future, skrub.");
                    return;
                }

                List<ElectionRecord> electionRecords = gameState.getElectionRecordForRound(round);

                if (electionRecords == null) {
                    event.respond("I don't have any election records yet for round " + round + '.');
                    return;
                }

                event.respond("Election records for round " + round + ":");
                int i = 1;

                for (ElectionRecord electionRecord : electionRecords) {
                    StringBuilder sb = new StringBuilder("Election ")
                        .append(i)
                        .append(": Presidential candidate: \002")
                        .append(electionRecord.getPresidentialCandidate().getNick())
                        .append("\002; Chancellor candidate: \002")
                        .append(electionRecord.getChancellorCandidate().getNick())
                        .append("\002; Votes For (\002\00303")
                        .append(electionRecord.getYesVotes().size())
                        .append("\003\002): \002");

                    Set<String> yesVotes = electionRecord.getYesVotes().stream()
                        .map(Player::getNick)
                        .collect(Collectors.toSet());

                    Set<String> noVotes = electionRecord.getNoVotes().stream()
                        .map(Player::getNick)
                        .collect(Collectors.toSet());

                    sb.append(Miscellaneous.getStringRepresentation(yesVotes))
                        .append("\002; Votes Against (\002\00304")
                        .append(electionRecord.getNoVotes().size())
                        .append("\003\002): \002")
                        .append(Miscellaneous.getStringRepresentation(noVotes))
                        .append("\002; Election result: \002\003");

                    if (electionRecord.wasElectionSuccessful()) {
                        sb.append("3PASSED");
                    } else {
                        sb.append("4FAILED");
                    }

                    event.respond(sb.append("\003\002").toString());
                    i++;
                }
                break;
            case CMD_LEGISLATIVE_RESULTS:
                remainder = remainder.trim();

                if (remainder.isEmpty()) {
                    event.respond("Specify the round number to get the legislative session results for.  You are currently in round " +
                        gameState.getRoundNumber() + ".  Syntax+ \"" + CMD_LEGISLATIVE_RESULTS + " roundNumber\"");
                    return;
                }

                int round2;

                try {
                    round2 = Integer.parseInt(remainder);
                } catch (NumberFormatException e) {
                    event.respond("Error: I didn't understand the round number you provided: " + e.getMessage());
                    return;
                }

                if (round2 < 1) {
                    event.respond("Error: Round number must be greater than or equal to 1");
                    return;
                }

                if (round2 > gameState.getRoundNumber()) {
                    event.respond("I can't predict the future, skrub.");
                    return;
                }

                LegislativeSessionRecord legislativeSessionRecord = gameState.getLegislativeRecordForRound(round2);

                if (legislativeSessionRecord == null) {
                    event.respond("I don't have any legislative session records yet for round " + round2 + '.');
                    return;
                }

                StringBuilder sb2 = new StringBuilder("Legislative session record for round ")
                    .append(round2)
                    .append(": President: \002");

                if (legislativeSessionRecord.getPresident() == null) {
                    sb2.append("(none)");
                } else {
                    sb2.append(legislativeSessionRecord.getPresident().getNick());
                }

                sb2.append("\002; Chancellor: \002");

                if (legislativeSessionRecord.getChancellor() == null) {
                    sb2.append("(none)");
                } else {
                    sb2.append(legislativeSessionRecord.getChancellor().getNick());
                }
                sb2.append("\002; Policy enacted: \002");

                if (legislativeSessionRecord.getEnactedPolicy() == null) {
                    sb2.append("(none)");
                } else {
                    sb2.append(legislativeSessionRecord.getEnactedPolicy());
                }

                sb2.append("\002; Veto power used this session: \002");

                if (legislativeSessionRecord.wasVetoPowerUsed()) {
                    sb2.append("YES");
                } else {
                    sb2.append("NO");
                }

                sb2.append('\002');

                if (legislativeSessionRecord.wasEnactedThroughChaos()) {
                    sb2.append(", but \002policy was enacted through chaos\002.");
                }

                event.respond(sb2.toString());
                break;
            case CMD_EXECUTIVE_ACTIONS:
                remainder = remainder.trim();

                if (remainder.isEmpty()) {
                    event.respond("Specify the round number to get the Executive Action record for.  You are currently in round " +
                        gameState.getRoundNumber() + ".  Syntax+ \"" + CMD_EXECUTIVE_ACTIONS+ " roundNumber\"");
                    return;
                }

                int round3;

                try {
                    round3 = Integer.parseInt(remainder);
                } catch (NumberFormatException e) {
                    event.respond("Error: I didn't understand the round number you provided: " + e.getMessage());
                    return;
                }

                if (round3 < 1) {
                    event.respond("Error: Round number must be greater than or equal to 1");
                    return;
                }

                if (round3 > gameState.getRoundNumber()) {
                    event.respond("I can't predict the future, skrub.");
                    return;
                }

                ExecutiveActionRecord executiveActionRecord = gameState.getExecutiveActionRecordForRound(round3);

                if (executiveActionRecord == null) {
                    event.respond("No Executive Actions were used in round " + round3 + " (yet).");
                    return;
                }
                
                StringBuilder sb3 = new StringBuilder("For round ")
                    .append(round3)
                    .append(", the Executive Action \002")
                    .append(executiveActionRecord.getExecutiveActionUsed())
                    .append("\002 was used by President \002")
                    .append(executiveActionRecord.getPresident().getNick())
                    .append('\002');
                
                if (executiveActionRecord.getTarget() != null) {
                    sb3.append(" against \002")
                        .append(executiveActionRecord.getTarget().getNick())
                        .append('\002');
                }
                
                sb3.append('.');
                
                event.respond(sb3.toString());
                break;
            case CMD_STATUS:
                switch (gameState.getRoundPhase()) {
                    case ELECTION_NOMINATION:
                        event.respond("Waiting for " + gameState.getPresidentialCandidate().getNick() + " to nominate someone to be Chancellor.");
                        break;
                    case ELECTION_VOTE:
                        Set<Player> notYetVoted = new HashSet<>(playerManager.getIngamePlayerList());
                        gameState.getVotingMap().keySet().forEach(notYetVoted::remove);

                        event.respond("Waiting for " + Miscellaneous.getStringRepresentation(notYetVoted.stream()
                            .map(Player::getNick)
                            .collect(Collectors.toSet())) + " to vote on the President/Chancellor pair."
                        );
                        break;
                    case LEGISLATIVE_SESSION_PRESIDENT_VOTE:
                        event.respond("Waiting for " + gameState.getPresidentialCandidate().getNick() + " to discard a policy.");
                        break;
                    case LEGISLATIVE_SESSION_CHANCELLOR_VOTE:
                        event.respond("Waiting for " + gameState.getChancellorCandidate().getNick() + " to enact a policy.");
                        break;
                    case LEGISLATIVE_SESSION_VETO_POWER_CHECK:
                        event.respond("Waiting for " + gameState.getPresidentialCandidate().getNick() + " to confirm or reject " +
                            gameState.getChancellorCandidate().getNick() + "'s veto request.");
                        break;
                    case EXECUTIVE_ACTION:
                        event.respond("Waiting for " + gameState.getPresidentialCandidate().getNick() + " to use their Executive Action.");
                        break;
                }
                break;
            case CMD_SHOWTRACK:
                event.respond("There were \002" + gameState.getPlayerCountAtGameStart() + "\002 players at game start, so the following " +
                    "Executive Action track is in use:");

                StringBuilder sb4 = new StringBuilder();

                for (Map.Entry<Integer, ExecutiveActions.ExecutiveAction> entry : executiveActionTrack.entrySet()) {
                    sb4.append("Unlocked at \002")
                        .append(entry.getKey())
                        .append("\002 Fascist policies: \002")
                        .append(entry.getValue())
                        .append("\002; ");
                }

                event.respond(sb4.substring(0, sb4.length() - 2));
                break;
            case CMD_NOMINATE:
                if (!gameState.getRoundPhase().equals(RoundPhase.ELECTION_NOMINATION)) {
                    event.respond("This command may only be used during the Election Nomination phase.");
                    return;
                }

                if (!player.equals(gameState.getPresidentialCandidate())) {
                    event.respond("Only the Presidential Candidate may nominate a Chancellor Candidate!");
                    return;
                }

                if (commands.size() != 2) {
                    event.respond("You have to specify who you want to nominate.");
                    return;
                }

                Player chancellorNominee = playerManager.getPlayerOfUser(event.getBot().getUserChannelDao().getUser(commands.get(1)));

                if (chancellorNominee.equals(player)) {
                    event.respond("You can't nominate yourself to be the Chancellor.");
                    return;
                }

                if (!gameState.isChancellorCandidateEligible(chancellorNominee)) {
                    event.respond(chancellorNominee.getNick() + " isn't eligible to be the Chancellor candidate.");
                    return;
                }

                channel.send().message('\002' + player.getNick() + "\002, the Presidential candidate, has nominated \002" +
                    chancellorNominee.getNick() + "\002 to be Chancellor.");
                channel.send().message(Miscellaneous.getStringRepresentation(playerManager.getIngamePlayerList().stream()
                        .map(Player::getNick)
                        .collect(Collectors.toList())
                    ) + ": Time to vote to elect this President/Chancellor pair! All players - including the " +
                    "candidates - please PM the bot \"" + CMD_VOTE + ' ' + VOTE_YES + "\" or \"" + CMD_VOTE + ' ' + VOTE_NO +
                    "\" to cast your vote.  There must be more \"" + VOTE_YES + "\" votes than \"" + VOTE_NO + "\" votes " +
                    "for this pair to be elected."
                );
                gameState.setChancellorCandidate(chancellorNominee);
                gameState.phaseToElectionVote();
                break;
            case CMD_VOTE:
                if (!gameState.getRoundPhase().equals(RoundPhase.ELECTION_VOTE)) {
                    event.respond("This command may only be used during the Election Voting phase.");
                    return;
                }

                if (!(event instanceof PrivateMessageEvent)) {
                    event.respond("You must issue this command in a private message to me.");
                    return;
                }

                if (commands.size() != 2) {
                    event.respond("You have to vote \"" + VOTE_YES + "\" or \"" + VOTE_NO + "\"!");
                    return;
                }

                switch (commands.get(1)) {
                    case VOTE_YES:
                        gameState.getVotingMap().put(player, true);
                        event.respond("You have voted YES.");
                        break;
                    case VOTE_NO:
                        gameState.getVotingMap().put(player, false);
                        event.respond("You have voted NO.");
                        break;
                    default:
                        event.respond("I didn't understand your vote"); // Should never get here
                        break;
                }

                if (gameState.allPlayersHaveVoted()) {
                    StringBuilder sb = new StringBuilder("All players have now voted.  On the matter of electing \002")
                        .append(gameState.getPresidentialCandidate().getNick())
                        .append("\002 to the office of President and \002")
                        .append(gameState.getChancellorCandidate().getNick())
                        .append("\002 to the office of Chancellor, the Reichstag has voted: \002");

                    ElectionRecord electionRecord = gameState.getResultsAndCommitElectionToRecord();

                    if (electionRecord.wasElectionSuccessful()) {
                        sb.append("JA");
                    } else {
                        sb.append("NEIN");
                    }

                    channel.send().message(sb.append("!\002  The votes are as follows:").toString());

                    Set<String> yesVotes = electionRecord.getYesVotes().stream()
                        .map(Player::getNick)
                        .collect(Collectors.toSet());

                    Set<String> noVotes = electionRecord.getNoVotes().stream()
                        .map(Player::getNick)
                        .collect(Collectors.toSet());

                    channel.send().message("Votes \002\00303For\003\002: " + Miscellaneous.getStringRepresentation(yesVotes));
                    channel.send().message("Votes \002\00304Against\003\002: " + Miscellaneous.getStringRepresentation(noVotes));

                    if (electionRecord.wasElectionSuccessful()) {
                        gameState.votePassed();
                        // If 3 or more Fascist policies have been enacted, check if we've elected Hitler.  If so, game
                        // over!
                        if (gameState.canHitlerWinByElection()) {
                            if (getHitler().equals(gameState.getChancellorCandidate())) {
                                channel.send().message("Game over!  After 3 or more Fascist policies were enacted, the " +
                                    "Reichstag has elected the Hitler as their Chancellor and the Fascists seize power.  " +
                                    "Dark days lie ahead for Germany... the \002Fascists\002 have won.");
                                gameEndCleanupAndStatsOutput(Party.FASCISTS);
                                gameHandler.signalGameStopped(this);
                                return;
                            }
                        }

                        // Advance to the Legislative Session
                        gameState.phaseElectionVotePassed();
                        beginLegislativeSession();
                    } else {
                        gameState.voteOrGovernmentFailed();

                        if (gameState.isGovernmentInChaos()) {
                            doChaos(false);
                        } else {
                            gameState.setNextPresidentialCandidateSequentially();
                            gameState.phaseElectionVoteFailed();
                            beginElectionPhase(false);
                        }
                    }
                }
                break;
            case CMD_DISCARD:
                if (!RoundPhase.LEGISLATIVE_SESSION_PRESIDENT_VOTE.equals(gameState.getRoundPhase()) || gameState.getLegislativeSessionPolicies() == null) {
                    event.respond("This command may only be used during the Legislative Session for the President's Vote!");
                    return;
                }

                if (!player.equals(gameState.getPresidentialCandidate())) {
                    event.respond("Only the President may use this command!");
                    return;
                }

                if (!(event instanceof PrivateMessageEvent)) {
                    event.respond("This command must be given in a private message.");
                    return;
                }

                if (commands.size() != 2) {
                    event.respond("You must specify which policy to discard (1, 2, or 3)");
                    return;
                }

                int policy;

                try {
                    policy = Integer.parseInt(commands.get(1));
                } catch (NumberFormatException e) {
                    // Should never happen
                    event.respond("I didn't understand the policy number you provided: " + e.getMessage());
                    return;
                }

                List<Policy> legislativeSessionPolicies = gameState.getLegislativeSessionPolicies();
                Policy discardedPolicy = legislativeSessionPolicies.remove(policy - 1);
                gameState.discardPolicy(discardedPolicy);

                event.respond("Discarded policy " + policy + ", which was a \002" + discardedPolicy.getPolicyType() + "\002 policy.");

                generateAndSendLegislativeSessionInfoForChancellor(legislativeSessionPolicies, event);

                gameState.phaseToChancellorLegislation();
                break;
            case CMD_ENACT:
                if (!RoundPhase.LEGISLATIVE_SESSION_CHANCELLOR_VOTE.equals(gameState.getRoundPhase())) {
                    event.respond("This command may only be used during the Legislative Session for the Chancellor's Vote!");
                    return;
                }

                if (!player.equals(gameState.getChancellorCandidate())) {
                    event.respond("Only the Chancellor may use this command!");
                    return;
                }

                if (!(event instanceof PrivateMessageEvent)) {
                    event.respond("This command must be given in a private message.");
                    return;
                }

                if (commands.size() != 2) {
                    event.respond("You must specify which policy to enact (1 or 2)");
                    return;
                }

                int policy2; // I'm so sorry

                try {
                    policy2 = Integer.parseInt(commands.get(1));
                } catch (NumberFormatException e) {
                    // Should never happen
                    event.respond("I didn't understand the policy number you provided: " + e.getMessage());
                    return;
                }

                List<Policy> legislativeSessionPolicies2 = gameState.getLegislativeSessionPolicies();

                Policy policyToEnact = legislativeSessionPolicies2.get(policy2 - 1);
                Policy policyToDiscard = legislativeSessionPolicies2.get(0); // Only one left at this point

                gameState.discardPolicy(policyToDiscard);
                gameState.enactPolicy(policyToEnact);

                GameHelpers.voicePlayers(playerManager.getIngamePlayerList(), channel);

                channel.send().message("The Legislative Session has ended.  The President and Chancellor have enacted a \002" +
                    policyToEnact.getPolicyType() + "\002 policy.");

                if (checkPolicyWinConditions()) {
                    return;
                }

                gameState.reshufflePoliciesIfNecessary(channel);
                gameState.resetFailedGovernments();
                gameState.commitLegislativeSessionRecord(policyToEnact, false, false);

                ExecutiveActions.ExecutiveAction executiveAction = executiveActionTrack.get(gameState.getEnactedPolicies(PolicyType.FASCIST));

                if (policyToEnact.getPolicyType().equals(PolicyType.FASCIST) && executiveAction != null) {
                    channel.send().message("The just-enacted Fascist policy has unlocked a use of the Executive Action \002" + executiveAction + '\002');
                    gameState.phaseToExecutiveAction();
                    useExecutiveAction(executiveAction);
                } else {
                    channel.send().message("No Executive Action is granted.");
                    gameState.setNextPresidentialCandidateSequentially();
                    gameState.nextRound(); // Advance round
                    beginElectionPhase(true);
                }
                break;
            case CMD_VETO:
                if (!RoundPhase.LEGISLATIVE_SESSION_CHANCELLOR_VOTE.equals(gameState.getRoundPhase()) &&
                    !RoundPhase.LEGISLATIVE_SESSION_VETO_POWER_CHECK.equals(gameState.getRoundPhase())
                    ) {
                    event.respond("This command may only be used during the Legislative Session for the Chancellor's Vote!");
                    return;
                }

                if (!(event instanceof PrivateMessageEvent)) {
                    event.respond("This command must be given in a private message.");
                    return;
                }

                if (!gameState.canVeto()) {
                    event.respond("The conditions for using Veto Power have not yet been met.  5 Fascist policies must " +
                        "be passed for Veto Power to become available.");
                    return;
                }

                if (gameState.isVetoUsedThisRound()) {
                    event.respond("Veto Power was already used (and rejected) this round.");
                    return;
                }

                switch (gameState.getRoundPhase()) {
                    case LEGISLATIVE_SESSION_CHANCELLOR_VOTE:
                        if (!player.equals(gameState.getChancellorCandidate())) {
                            event.respond("Only the Chancellor may use this command!");
                            return;
                        }

                        channel.send().message("The Chancellor \002wishes to veto this agenda\002.");

                        sendVetoInformation(event);

                        gameState.phaseToVeto();
                        break;
                    case LEGISLATIVE_SESSION_VETO_POWER_CHECK:
                        if (!player.equals(gameState.getPresidentialCandidate())) {
                            event.respond("Only the President may use this command!");
                            return;
                        }

                        if (commands.size() != 2) {
                            event.respond("You must specify \"" + CMD_VETO + ' ' + VOTE_YES + "\" or \"" + CMD_VETO + " " +
                                VOTE_NO + "\".");
                            return;
                        }

                        switch (commands.get(1)) {
                            case VOTE_YES:
                                channel.send().message("The President agrees to the veto.  No policy is enacted.");

                                GameHelpers.voicePlayers(playerManager.getIngamePlayerList(), channel);

                                List<Policy> policies = gameState.getLegislativeSessionPolicies();
                                policies.forEach(gameState::discardPolicy);

                                gameState.voteOrGovernmentFailed();

                                if (gameState.isGovernmentInChaos()) {
                                    doChaos(true);
                                } else {
                                    gameState.commitLegislativeSessionRecord(null, false, true);
                                    gameState.reshufflePoliciesIfNecessary(channel);
                                    // Round ends
                                    // No policies played, so no Executive Action
                                    gameState.setNextPresidentialCandidateSequentially();
                                    gameState.nextRound(); // Advance round
                                    beginElectionPhase(true);
                                }
                                break;
                            case VOTE_NO:
                                channel.send().message("The President has rejected the veto.");
                                event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "The President " +
                                    "has rejected your use of the Veto Power.");
                                gameState.vetoDenied();
                                generateAndSendLegislativeSessionInfoForChancellor(gameState.getLegislativeSessionPolicies(), event);
                                break;
                        }

                        break;
                }
                break;
            case CMD_USE_ACTION:
                if (!RoundPhase.EXECUTIVE_ACTION.equals(gameState.getRoundPhase()) || gameState.getAvailableExecutiveAction() == null) {
                    event.respond("This command may only be used during the Executive Action phase.");
                    return;
                }

                if (!player.equals(gameState.getPresidentialCandidate())) {
                    event.respond("Only the President may use this command!");
                    return;
                }

                if (commands.size() != 2) {
                    event.respond("You must specify the player to use the Executive Action on.");
                    return;
                }

                Player target = playerManager.getPlayerOfUser(event.getBot().getUserChannelDao().getUser(commands.get(1)));

                if (target.equals(player)) {
                    event.respond("You can't use an Executive Action on yourself.");
                    return;
                }

                ExecutiveActions.ExecutiveAction executiveAction1 = gameState.getAvailableExecutiveAction();

                if (executiveAction1 instanceof ExecutiveActions.InvestigateLoyalty) {
                    if (playerStates.get(target).hasBeenInvestigated()) {
                        event.respond("This player has been previously investigated and may not be investigated again.");
                        return;
                    }
                }

                executiveAction1.accept(target);

                if (executiveAction1.gameEnded()) {
                    // Check if the executive action ended the game (Hitler was killed)
                    return;
                }

                gameState.commitExecutiveActionRecord(executiveAction1, target);

                // Advance to next round, but only select the next candidate sequentially if the Special Election was
                // not used
                if (!(executiveAction1 instanceof ExecutiveActions.CallSpecialElection)) {
                    gameState.setNextPresidentialCandidateSequentially();
                }

                gameState.nextRound();
                beginElectionPhase(true);
                break;
        }
    }

    @Override
    public void gameStart(GenericMessageEvent<T> event, ScheduledExecutorService scheduledExecutorService) {
        String everyone = Miscellaneous.getStringRepresentation(playerManager.getIngamePlayerList().stream()
            .map(Player::getNick)
            .collect(Collectors.toList()), " ");

        gameState.gameStartHook();
        int playerCount = playerManager.getIngamePlayerCount();

        GameHelpers.voicePlayers(playerManager.getIngamePlayerList(), channel);
        channel.send().setModerated(channel); // pircbotx wat

        channel.send().message(everyone + ": Welcome to Secret Hitler, the hidden identity game inspired by Werewolf and Mafia.  Can you find and kill the Secret Hitler before it's too late?");

        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);
        ExecutiveActions<T> executiveActions = new ExecutiveActions<>(this);
        executiveActionTrack = executiveActions.getExecutiveActionTrack(playerCount);

        channel.send().message("There are \002" + playerCount + "\002 players: \002" + roleCount.get(Role.LIBERAL) +
            "\002 Liberals, \002" + roleCount.get(Role.FASCIST) + "\002 Fascists, and \002" + roleCount.get(Role.HITLER) +
            "\002 Hitler.");

        channel.send().message("\002Please note\002: Players are \002not allowed\002 to exchange any game-related information outside of this channel, including in private message, at any time.");

        channel.send().message("Please wait while I assign roles send out role information to all players....");

        doRoleAssignment(roleCount);

        getLiberals().forEach(player -> event.getBot().sendIRC().message(player.getNick(), Role.LIBERAL.getDescription()));
        getFascists().forEach(player -> event.getBot().sendIRC().message(player.getNick(), Role.FASCIST.getDescription()));
        event.getBot().sendIRC().message(getHitler().getNick(), Role.HITLER.getDescription());

        getFascists().forEach(player -> event.getBot().sendIRC().message(player.getNick(), getFascistPartyMembersAsInformativeString()));

        sendHitlerExtraInformationIfAppropriate(event);

        channel.send().message("All players, please now check your PMs for your role information.  If you forget, you can use the \"" + CMD_MYROLE + "\" command to be reminded.");

        channel.send().message("(Note: The \"" + CMD_ELECTION_RESULTS + "\" command can be used to see all past " +
            "election results this game, the \"" + CMD_LEGISLATIVE_RESULTS + "\" command can be used to see all " +
            "past Legislative Session results this game, and the \"" + CMD_EXECUTIVE_ACTIONS + "\" command can be used " +
            "to see all past Executive Actions this game.");
        channel.send().message("The \"" + CMD_SHOWTRACK + "\" command can be used at any time to show the Executive Action track " +
            "currently in play.)");

        gameState.setNextPresidentialCandidateRandomly();

        beginElectionPhase(true);
    }

    @Override
    public void gameHaltRequest(GenericMessageEvent<T> event) {
        channel.send().message("The game is being forcibly stopped.  Nobody wins.");
        gameEndCleanupAndStatsOutput();
    }

    @Override
    public void handlePlayerQuit(Player player, boolean voteToContinue) {
        channel.send().message('\002' + player.getNick() + "\002 has left the game.  Unfortunately, the game cannot continue and it must now end.  Nobody wins.");
        gameEndCleanupAndStatsOutput();
        gameHandler.signalGameStopped(this);
    }

    @Override
    public void playersCommandHook(GenericMessageEvent<T> event) {
        int playerCount = gameState.getPlayerCountAtGameStart();
        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);

        event.respond("At game start, there were \002" + playerCount + "\002 players: \002" + roleCount.get(Role.LIBERAL) +
            "\002 Liberals, \002" + roleCount.get(Role.FASCIST) + "\002 Fascists, and \002" + roleCount.get(Role.HITLER) +
            "\002 Hitler.");
    }

    @Override
    public void handlePlayerSubstitution(String newNickname, String oldNickname) {
        channel.send().setMode("+v-v ", newNickname, oldNickname);
    }

    private void gameEndCleanupAndStatsOutput() {
        gameEndCleanupAndStatsOutput(null);
    }

    void gameEndCleanupAndStatsOutput(Party winners) {
        channel.send().message("The Liberals were: " + Miscellaneous.getStringRepresentation(getLiberals().stream()
            .map(Player::getNick)
            .collect(Collectors.toList())
        ));
        channel.send().message("The Fascists were: " + Miscellaneous.getStringRepresentation(getFascists().stream()
            .map(Player::getNick)
            .collect(Collectors.toList())
        ));
        channel.send().message("The Hitler was: " + getHitler().getNick());

        if (winners != null) {
            Set<String> winnersSet = playerStates.entrySet().stream()
                .filter(e -> e.getValue().getRole().getParty().equals(winners))
                .map(Map.Entry::getKey)
                .map(Player::getNick)
                .collect(Collectors.toSet());

            channel.send().message("The winners are: " + Miscellaneous.getStringRepresentation(winnersSet));
        }

        channel.send().removeModerated(channel);
        GameHelpers.devoicePlayers(playerManager.getIngamePlayerList(), channel);
    }

    String getFascistPartyMembersAsInformativeString() {
        return "The Fascists are: " +
            Miscellaneous.getStringRepresentation(getFascists().stream()
                .map(player -> new Pair<>(player, playerManager.getIngamePlayerList().contains(player)))
                .map(pair -> pair.getLeft().getNick() + (!pair.getRight() ? " (dead)" : ""))
                .collect(Collectors.toList())
            ) + "; the Hitler is: " + getHitler().getNick();
    }

    void sendHitlerExtraInformationIfAppropriate(GenericEvent<T> event) {
        int playerCount = gameState.getPlayerCountAtGameStart();

        if (playerCount >= 5 && playerCount <= 6) {
            event.getBot().sendIRC().message(getHitler().getNick(), "The Fascists are: " +
                Miscellaneous.getStringRepresentation(getFascists().stream()
                    .map(Player::getNick)
                    .collect(Collectors.toList())
                )
            );
        }
    }

    private void doRoleAssignment(Map<Role, Integer> roleCount) {
        int numHitlers = 0;
        int numFascists = 0;
        int numLiberals = 0;
        Random random = new Random();

        for (Player player : playerManager.getIngamePlayerList()) {
            outer: while (true) {
                int role = random.nextInt(3);

                switch (role) {
                    case 0:
                        // Liberal
                        if (numLiberals < roleCount.get(Role.LIBERAL)) {
                            playerStates.put(player, new PlayerState(Role.LIBERAL));
                            numLiberals++;
                            break outer;
                        }
                        break;
                    case 1:
                        // Fascist
                        if (numFascists < roleCount.get(Role.FASCIST)) {
                            playerStates.put(player, new PlayerState(Role.FASCIST));
                            numFascists++;
                            break outer;
                        }
                        break;
                    case 2:
                        // Hitler!
                        if (numHitlers < roleCount.get(Role.HITLER)) {
                            playerStates.put(player, new PlayerState(Role.HITLER));
                            numHitlers++;
                            break outer;
                        }
                        break;
                }
            }
        }
    }

    private List<Player> getPlayersWithRole(Role role) {
        Objects.requireNonNull(role);

        return playerStates.entrySet().stream()
            .filter(e -> role.equals(e.getValue().getRole()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private List<Player> getLiberals() {
        return getPlayersWithRole(Role.LIBERAL);
    }

    private List<Player> getFascists() {
        return getPlayersWithRole(Role.FASCIST);
    }

    private Player getHitler() {
        return getPlayersWithRole(Role.HITLER).get(0); // There's only ever one hitler
    }

    private void beginElectionPhase(boolean introduceNewRound) {
        if (introduceNewRound) {
            channel.send().message("Round \002" + gameState.getRoundNumber() + "\002; Election Phase");
        }

        channel.send().message("The Presidential Candidate is \002" + gameState.getPresidentialCandidate().getNick() + "\002.  " +
            gameState.getPresidentialCandidate().getNick() + ", please discuss with everyone who you want to be chancellor, " +
            "and when you're ready, nominate them with the command \"" + CMD_NOMINATE + " [playerName]\"."
        );

        StringBuilder sb = new StringBuilder("The following players are ineligible to be nominated Chancellor, since they " +
            "were " + (playerManager.getIngamePlayerCount() > 5 ? "either " : "") + "the previously elected " +
            (playerManager.getIngamePlayerCount() > 5 ? "President or " : "") + "Chancellor: ");

        boolean playersAreIneligible = false;

        if (gameState.getPreviousElectedChancellor() != null) {
            sb.append(gameState.getPreviousElectedChancellor().getNick());
            sb.append(' ');
            playersAreIneligible = true;
        }

        if (gameState.getPreviousElectedPresident() != null && playerManager.getIngamePlayerCount() > 5) {
            sb.append(gameState.getPreviousElectedPresident().getNick());
            playersAreIneligible = true;
        }

        if (playersAreIneligible) {
            channel.send().message(sb.toString().trim());
        }

        if (gameState.getFailedGovernments() == 2) {
            channel.send().message("WARNING - If this government (President/Chancellor pair) is not elected, the government" +
                " will be thrown into chaos!  The top policy in the policy deck will be enacted, for better or for worse....");
        }

        if (gameState.getEnactedPolicies(PolicyType.FASCIST) >= 3) {
            channel.send().message("WARNING - 3 or more Fascist policies have been enacted - if the Hitler is elected Chancellor, " +
                "the Fascists win!");
        }
    }

    private void doChaos(boolean becauseOfVeto) {
        channel.send().message("Because three governments in a row have failed, the country is now " +
            "in Chaos.  The people, frustrated, rise up and enact the next policy in the deck, for " +
            "better or for worse....");
        if (RoundPhase.ELECTION_VOTE.equals(gameState.getRoundPhase())) {
            gameState.phaseToPresidentLegislation(); // Legislative Session
        }
        Policy toEnact = gameState.drawTopPolicy();
        channel.send().message("The people have enacted, by force, a \002" + toEnact.getPolicyType() + "\002 policy.  Because " +
            "the people enacted this policy, no Executive Action is granted.");
        gameState.enactPolicy(toEnact);
        gameState.commitLegislativeSessionRecord(toEnact, true, becauseOfVeto);

        channel.send().message("There have now been \002" + gameState.getEnactedPolicies(PolicyType.LIBERAL) + "\002 " +
            "Liberal policies enacted, and \002" + gameState.getEnactedPolicies(PolicyType.FASCIST) + "\002 Fascist " +
            "policies have been enacted.");

        if (toEnact.getPolicyType().equals(PolicyType.FASCIST)) {
            ExecutiveActions.ExecutiveAction executiveAction = executiveActionTrack.get(gameState.getEnactedPolicies(PolicyType.FASCIST));

            if (executiveAction != null) {
                channel.send().message("(This policy enactment would have unlocked a usage of the \002" + executiveAction.toString() +
                    "\002 Executive Action)");
            }
        }
        gameState.reshufflePoliciesIfNecessary(channel);
        if (checkPolicyWinConditions()) {
            return;
        }

        // Ignore any Executive Action
        gameState.resetTermLimits();
        gameState.resetFailedGovernments();
        gameState.setNextPresidentialCandidateSequentially();
        gameState.nextRound(); // Advance round
        beginElectionPhase(true); // New round
    }

    private void beginLegislativeSession() {
        channel.send().message("Round \002" + gameState.getRoundNumber() + "\002; Legislative Session");
        GameHelpers.devoicePlayers(playerManager.getIngamePlayerList(), channel);
        channel.send().message("\002Quiet please\002 - All player communication - \002including private messages\002 - " +
            "is \002forbidden\002 while the Reichstag is in session.");
        channel.send().message("President and Chancellor, please check your private messages for instructions.  Everyone" +
            " else, please wait for the Legislative Session to end.");
        channel.send().message("So far, \002" + gameState.getEnactedPolicies(PolicyType.LIBERAL) + "\002 Liberal policies have been enacted, and \002" +
            gameState.getEnactedPolicies(PolicyType.FASCIST) + "\002 Fascist policies have been enacted.");


        channel.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "Please wait for the President to make policy selections.");

        List<Policy> policies = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            policies.add(gameState.drawTopPolicy());
        }

        generateAndSendLegislativeSessionInfoForPresident(policies);

        gameState.setLegislativeSessionPolicies(policies);
    }

    private boolean checkPolicyWinConditions() {
        if (gameState.liberalsWinByPolicies()) {
            channel.send().message("Game over!  Five Liberal policies have been enacted, causing the Liberals to " +
                "secure their place as the rightful leaders of Germany.  The \002Liberals\002 win the game!");
            gameEndCleanupAndStatsOutput(Party.LIBERALS);
            gameHandler.signalGameStopped(this);
            return true;
        } else if (gameState.fascistsWinByPolicies()) {
            channel.send().message("Game over!  Six Fascist policies have been enacted, allowing the Fascists to " +
                "install the Hitler as the Chancellor by force.  The coup is but a hint of things to come, for the " +
                "\002Fascists\002 have won the game.");
            gameEndCleanupAndStatsOutput(Party.FASCISTS);
            gameHandler.signalGameStopped(this);
            return true;
        } else {
            return false;
        }
    }

    GameState getGameState() {
        return gameState;
    }

    Map<Player, PlayerState> getPlayerStates() {
        return playerStates;
    }

    Channel getChannel() {
        return channel;
    }

    GameHandler<T> getGameHandler() {
        return gameHandler;
    }

    private void useExecutiveAction(ExecutiveActions.ExecutiveAction executiveAction) {
        channel.send().message("Round \002" + gameState.getRoundNumber() + "\002; Executive Action");

        if (executiveAction instanceof ExecutiveActions.PolicyPeek) {
            executiveAction.accept(null); // This one doesn't require any arguments, so immediately use it then proceed to next round
            gameState.commitExecutiveActionRecord(executiveAction, null);
            gameState.setNextPresidentialCandidateSequentially();
            gameState.nextRound(); // Begin new round
            beginElectionPhase(true);
        } else {
            channel.send().message("President, please check your private messages for instructions.");

            showExecutiveActionUsageInstructions(executiveAction);

            gameState.setAvailableExecutiveAction(executiveAction);
        }
    }

    private void generateAndSendLegislativeSessionInfoForPresident(List<Policy> policies) {
        StringBuilder sb = new StringBuilder("President, the following policies are up selection this round: ");

        for (int i = 0; i < policies.size(); i++) {
            sb.append("\002[")
                .append(i + 1)
                .append("]\002: ")
                .append(policies.get(i))
                .append("; ");
        }

        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), sb.substring(0, sb.length() - 2));
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "Please PM me the command \"" +
            CMD_DISCARD + " number\", where \"number\" is the policy number you wish to \002discard\002.  The remaining " +
            "two policies will be sent to the Chancellor, who will pick one from the two to enact.");
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "Remember - you MAY NOT communicate " +
            "with the Chancellor in ANY WAY during the Legislative Session, INCLUDING private messages!");
    }

    private void generateAndSendLegislativeSessionInfoForChancellor(List<Policy> legislativeSessionPolicies, GenericMessageEvent<T> event) {
        StringBuilder sb = new StringBuilder("Chancellor, the President has discarded a policy and presented " +
            "you with the following policy choices: ");

        for (int i = 0; i < legislativeSessionPolicies.size(); i++) {
            sb.append("\002[")
                .append(i + 1)
                .append("]\002: ")
                .append(legislativeSessionPolicies.get(i))
                .append("; ");
        }

        event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), sb.substring(0, sb.length() - 2));

        event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "You must now select a " +
            "policy to enact by PMing me the command \"" + CMD_ENACT + " number\", where \"number\" is the " +
            "number of the policy you wish to enact.");

        if (gameState.canVeto() && !gameState.isVetoUsedThisRound()) {
            event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "Alternatively, since " +
                "5 Fascist policies have been enacted, you can exercise Veto Power by sending me the command \"" +
                CMD_VETO + "\" in PM.  Vetoing will result in no policies being enacted, but only if the President " +
                "concurs, and a veto will count as a failed government.");

            if (gameState.getFailedGovernments() >= 2) {
                event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "WARNING - If you " +
                    "successfully use your Veto Power, the government will be considered failed and be thrown " +
                    "into chaos!  The people will rise up and enact the next policy in the policy deck, for better" +
                    " or for worse....");
            }
        }

        event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "Remember - you MAY NOT communicate " +
            "with the President in ANY WAY during the Legislative Session, INCLUDING private messages!");
    }

    private void sendVetoInformation(GenericMessageEvent<T> event) {
        event.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(),
            "President, the Chancellor has voted to exercise veto power this round.  Vetoing will cause " +
                "both the policies you passed to the Chancellor to be discarded.  However, the government will" +
                "be considered a failed one.  If you reject, the Chancellor will be forced to enact a policy anyway."
        );

        if (gameState.getFailedGovernments() >= 2) {
            event.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "WARNING - If you " +
                "approve the use of Veto Power, the government will be considered failed and be thrown " +
                "into chaos!  The people will rise up and enact the next policy in the policy deck, for better" +
                " or for worse....");
        }

        event.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "To approve the " +
            "veto, PM me \"" + CMD_VETO + ' ' + VOTE_YES + "\".  To reject, PM me \"" + CMD_VETO + " " +
            VOTE_NO + "\".");
    }

    private void showExecutiveActionUsageInstructions(ExecutiveActions.ExecutiveAction executiveAction) {
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), executiveAction.getInstructions());
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "You must use this executive " +
            "action here and now - passing is not allowed!");
    }
}
