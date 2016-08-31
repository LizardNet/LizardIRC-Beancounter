package org.lizardirc.beancounter.games.secrethitler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.gameframework.GameHelpers;
import org.lizardirc.beancounter.gameframework.playermanagement.Player;
import org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager;
import org.lizardirc.beancounter.utils.IrcColors;
import org.lizardirc.beancounter.utils.Miscellaneous;

class Commands<T extends PircBotX> {
    private final SecretHitler<T> parent;
    private final GameState gameState;
    private final Map<Player, PlayerState> playerStates;
    private final PlayerManager playerManager;
    private final Channel channel;
    private final Map<Integer, ExecutiveActions.ExecutiveAction> executiveActionTrack;

    public Commands(SecretHitler<T> parent) {
        this.parent = parent;
        gameState = parent.getGameState();
        playerStates = parent.getPlayerStates();
        playerManager = parent.getPlayerManager();
        channel = parent.getChannel();
        executiveActionTrack = parent.getExecutiveActionTrack();
    }

    public void handleMyrole(GenericMessageEvent<T> event, Player player) {
        // Send role information
        switch (playerStates.get(player).getRole()) {
            case LIBERAL:
                event.getUser().send().message(Role.LIBERAL.getDescription());
                break;
            case FASCIST:
                event.getUser().send().message(Role.FASCIST.getDescription());
                event.getUser().send().message(parent.getFascistPartyMembersAsInformativeString());
                break;
            case HITLER:
                event.getUser().send().message(Role.HITLER.getDescription());
                parent.sendHitlerExtraInformationIfAppropriate(event);
                break;
        }

        // Check if we're waiting for the player to do something, and if so, remind them
        switch (gameState.getRoundPhase()) {
            case ELECTION_NOMINATION:
                if (player.equals(gameState.getPresidentialCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "Presidential Candidate" +
                        IrcColors.BOLD + " this election - please select a Chancellor Candidate by using the " +
                        "command \"" + SecretHitler.CMD_NOMINATE + " player\".");
                }
                break;
            case ELECTION_VOTE:
                if (!gameState.getVotingMap().keySet().contains(player)) {
                    event.getUser().send().message("Waiting for you to vote on electing the Presidential " +
                        "candidate, " + IrcColors.BOLD + gameState.getPresidentialCandidate().getNick() + IrcColors.BOLD + ", and the " +
                        "Chancellor candidate, " + IrcColors.BOLD + gameState.getChancellorCandidate().getNick() + IrcColors.BOLD + ". " +
                        "Please PM me either \"" + SecretHitler.CMD_VOTE + ' ' + SecretHitler.VOTE_YES + "\" or \"" + SecretHitler.CMD_VOTE + ' ' +
                        SecretHitler.VOTE_NO + "\".");
                }
                break;
            case LEGISLATIVE_SESSION_PRESIDENT_VOTE:
                if (player.equals(gameState.getPresidentialCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "President" + IrcColors.BOLD + " this Legislative Session.  " +
                        "Waiting for you to discard a policy:");
                    parent.generateAndSendLegislativeSessionInfoForPresident(gameState.getLegislativeSessionPolicies());
                } else if (player.equals(gameState.getChancellorCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "Chancellor" + IrcColors.BOLD + " this Legislative Session.  " +
                        "You don't need to do anything right now; waiting on the President to discard a policy.");
                }
                break;
            case LEGISLATIVE_SESSION_CHANCELLOR_VOTE:
                if (player.equals(gameState.getChancellorCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "Chancellor" + IrcColors.BOLD + " this Legislative Session.  " +
                        "Waiting for you to enact a policy:");
                    parent.generateAndSendLegislativeSessionInfoForChancellor(gameState.getLegislativeSessionPolicies(), event);
                } else if (player.equals(gameState.getPresidentialCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "President" + IrcColors.BOLD + " this Legislative Session.  " +
                        "You don't need to do anything right now.");
                }
                break;
            case LEGISLATIVE_SESSION_VETO_POWER_CHECK:
                if (player.equals(gameState.getPresidentialCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "President" + IrcColors.BOLD + " this Legislative Session.  " +
                        "Waiting for you to approve or reject the Chancellor's use of their Veto Power:");
                    parent.sendVetoInformation(event);
                } else if (player.equals(gameState.getChancellorCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "Chancellor" + IrcColors.BOLD + " this Legislative Session.  " +
                        "You don't need to do anything right now - waiting for the President to either consent " +
                        "to or reject your request to use Veto Power.");
                }
                break;
            case EXECUTIVE_ACTION:
                if (player.equals(gameState.getPresidentialCandidate())) {
                    event.getUser().send().message("You are the " + IrcColors.BOLD + "President" + IrcColors.BOLD + ", and you have the Executive" +
                        "Action " + IrcColors.BOLD + gameState.getAvailableExecutiveAction() + IrcColors.BOLD + " at your diposal!");
                    parent.showExecutiveActionUsageInstructions(gameState.getAvailableExecutiveAction());
                }
                break;
        }
    }

    public void handleElectionResults(GenericMessageEvent<T> event, String remainder) {
        remainder = remainder.trim();

        if (remainder.isEmpty()) {
            event.respond("Specify the round number to get the election results for.  You are currently in round " +
                gameState.getRoundNumber() + ".  Syntax: \"" + SecretHitler.CMD_ELECTION_RESULTS + " roundNumber\"");
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
                .append(": Presidential candidate: ")
                .append(IrcColors.BOLD)
                .append(electionRecord.getPresidentialCandidate().getNick())
                .append(IrcColors.BOLD)
                .append("; Chancellor candidate: ")
                .append(IrcColors.BOLD)
                .append(electionRecord.getChancellorCandidate().getNick())
                .append(IrcColors.BOLD)
                .append("; Votes For (")
                .append(IrcColors.BOLD)
                .append(IrcColors.DARKGREEN)
                .append(electionRecord.getYesVotes().size())
                .append(IrcColors.RESET)
                .append("): ")
                .append(IrcColors.BOLD);

            Set<String> yesVotes = electionRecord.getYesVotes().stream()
                .map(Player::getNick)
                .collect(Collectors.toSet());

            Set<String> noVotes = electionRecord.getNoVotes().stream()
                .map(Player::getNick)
                .collect(Collectors.toSet());

            sb.append(Miscellaneous.getStringRepresentation(yesVotes))
                .append(IrcColors.BOLD)
                .append("; Votes Against (")
                .append(IrcColors.BOLD)
                .append(IrcColors.RED)
                .append(electionRecord.getNoVotes().size())
                .append(IrcColors.RESET)
                .append("): ")
                .append(IrcColors.BOLD)
                .append(Miscellaneous.getStringRepresentation(noVotes))
                .append(IrcColors.BOLD)
                .append("; Election result: ")
                .append(IrcColors.BOLD);

            if (electionRecord.wasElectionSuccessful()) {
                sb.append(IrcColors.DARKGREEN)
                    .append("3PASSED");
            } else {
                sb.append(IrcColors.RED)
                    .append("4FAILED");
            }

            event.respond(sb.append(IrcColors.RESET).toString());
            i++;
        }
    }

    public void handleLegislativeResults(GenericMessageEvent<T> event, String remainder) {
        remainder = remainder.trim();

        if (remainder.isEmpty()) {
            event.respond("Specify the round number to get the legislative session results for.  You are currently in round " +
                gameState.getRoundNumber() + ".  Syntax+ \"" + SecretHitler.CMD_LEGISLATIVE_RESULTS + " roundNumber\"");
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

        LegislativeSessionRecord legislativeSessionRecord = gameState.getLegislativeRecordForRound(round);

        if (legislativeSessionRecord == null) {
            event.respond("I don't have any legislative session records yet for round " + round + '.');
            return;
        }

        StringBuilder sb = new StringBuilder("Legislative session record for round ")
            .append(round)
            .append(": President: ")
            .append(IrcColors.BOLD);

        if (legislativeSessionRecord.getPresident() == null) {
            sb.append("(none)");
        } else {
            sb.append(legislativeSessionRecord.getPresident().getNick());
        }

        sb.append(IrcColors.BOLD)
            .append("; Chancellor: ")
            .append(IrcColors.BOLD);

        if (legislativeSessionRecord.getChancellor() == null) {
            sb.append("(none)");
        } else {
            sb.append(legislativeSessionRecord.getChancellor().getNick());
        }
        sb.append(IrcColors.BOLD)
            .append("; Policy enacted: ")
            .append(IrcColors.BOLD);

        if (legislativeSessionRecord.getEnactedPolicy() == null) {
            sb.append("(none)");
        } else {
            sb.append(legislativeSessionRecord.getEnactedPolicy());
        }

        sb.append(IrcColors.BOLD)
            .append("; Veto power used this session: ")
            .append(IrcColors.BOLD);

        if (legislativeSessionRecord.wasVetoPowerUsed()) {
            sb.append("YES");
        } else {
            sb.append("NO");
        }

        sb.append(IrcColors.BOLD);

        if (legislativeSessionRecord.wasEnactedThroughChaos()) {
            sb.append(", but ")
                .append(IrcColors.BOLD)
                .append(IrcColors.RED)
                .append("policy was enacted through chaos")
                .append(IrcColors.RESET)
                .append(".");
        }

        event.respond(sb.toString());
    }

    public void handleExecutiveActions(GenericMessageEvent<T> event, String remainder) {
        remainder = remainder.trim();

        if (remainder.isEmpty()) {
            event.respond("Specify the round number to get the Executive Action record for.  You are currently in round " +
                gameState.getRoundNumber() + ".  Syntax+ \"" + SecretHitler.CMD_EXECUTIVE_ACTIONS + " roundNumber\"");
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
            .append(", the Executive Action ")
            .append(IrcColors.BOLD)
            .append(executiveActionRecord.getExecutiveActionUsed())
            .append(IrcColors.BOLD)
            .append(" was used by President ")
            .append(IrcColors.BOLD)
            .append(executiveActionRecord.getPresident().getNick())
            .append(IrcColors.BOLD);

        if (executiveActionRecord.getTarget() != null) {
            sb3.append(" against ")
                .append(IrcColors.BOLD)
                .append(executiveActionRecord.getTarget().getNick())
                .append(IrcColors.BOLD);
        }

        sb3.append('.');

        event.respond(sb3.toString());
    }

    public void handleStatus(GenericMessageEvent<T> event) {
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
    }

    public void handleShowTrack(GenericMessageEvent<T> event) {
        event.respond("There were " + IrcColors.BOLD + gameState.getPlayerCountAtGameStart() + IrcColors.BOLD +
            " players at game start, so the following Executive Action track is in use:");

        StringBuilder sb4 = new StringBuilder();

        for (Map.Entry<Integer, ExecutiveActions.ExecutiveAction> entry : executiveActionTrack.entrySet()) {
            sb4.append("Unlocked at ")
                .append(IrcColors.BOLD)
                .append(entry.getKey())
                .append(IrcColors.BOLD)
                .append(" Fascist policies: ")
                .append(IrcColors.BOLD)
                .append(entry.getValue())
                .append(IrcColors.BOLD)
                .append("; ");
        }

        event.respond(sb4.substring(0, sb4.length() - 2));
    }

    public void handleNominate(GenericMessageEvent<T> event, Player player, List<String> commands) {
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

        channel.send().message(IrcColors.BOLD + player.getNick() + IrcColors.BOLD + ", the Presidential candidate, has nominated " +
            IrcColors.BOLD + chancellorNominee.getNick() + IrcColors.BOLD + " to be Chancellor.");
        channel.send().message(Miscellaneous.getStringRepresentation(playerManager.getIngamePlayerList().stream()
                .map(Player::getNick)
                .collect(Collectors.toList())
            ) + ": Time to vote to elect this President/Chancellor pair! All players - including the " +
                "candidates - please PM the bot \"" + SecretHitler.CMD_VOTE + ' ' + SecretHitler.VOTE_YES + "\" or \"" +
                SecretHitler.CMD_VOTE + ' ' + SecretHitler.VOTE_NO + "\" to cast your vote.  There must be more \"" +
                SecretHitler.VOTE_YES + "\" votes than \"" + SecretHitler.VOTE_NO + "\" votes for this pair to be elected."
        );
        gameState.setChancellorCandidate(chancellorNominee);
        gameState.phaseToElectionVote();
    }

    public void handleVote(GenericMessageEvent<T> event, Player player, List<String> commands) {
        if (!gameState.getRoundPhase().equals(RoundPhase.ELECTION_VOTE)) {
            event.respond("This command may only be used during the Election Voting phase.");
            return;
        }

        if (!(event instanceof PrivateMessageEvent)) {
            event.respond("You must issue this command in a private message to me.");
            return;
        }

        if (commands.size() != 2) {
            event.respond("You have to vote \"" + SecretHitler.VOTE_YES + "\" or \"" + SecretHitler.VOTE_NO + "\"!");
            return;
        }

        switch (commands.get(1)) {
            case SecretHitler.VOTE_YES:
                gameState.getVotingMap().put(player, true);
                event.respond("You have voted YES.");
                break;
            case SecretHitler.VOTE_NO:
                gameState.getVotingMap().put(player, false);
                event.respond("You have voted NO.");
                break;
            default:
                event.respond("I didn't understand your vote"); // Should never get here
                break;
        }

        if (gameState.allPlayersHaveVoted()) {
            StringBuilder sb = new StringBuilder("All players have now voted.  On the matter of electing ")
                .append(IrcColors.BOLD)
                .append(gameState.getPresidentialCandidate().getNick())
                .append(IrcColors.BOLD)
                .append(" to the office of President and ")
                .append(IrcColors.BOLD)
                .append(gameState.getChancellorCandidate().getNick())
                .append(IrcColors.BOLD)
                .append(" to the office of Chancellor, the Reichstag has voted: ")
                .append(IrcColors.BOLD);

            ElectionRecord electionRecord = gameState.getResultsAndCommitElectionToRecord();

            if (electionRecord.wasElectionSuccessful()) {
                sb.append("JA");
            } else {
                sb.append("NEIN");
            }

            sb.append('!')
                .append(IrcColors.BOLD)
                .append("The votes are as follows:");

            channel.send().message(sb.toString());

            Set<String> yesVotes = electionRecord.getYesVotes().stream()
                .map(Player::getNick)
                .collect(Collectors.toSet());

            Set<String> noVotes = electionRecord.getNoVotes().stream()
                .map(Player::getNick)
                .collect(Collectors.toSet());

            channel.send().message("Votes " + IrcColors.BOLD + IrcColors.DARKGREEN + "For" + IrcColors.RESET +
                ": " + Miscellaneous.getStringRepresentation(yesVotes));
            channel.send().message("Votes " + IrcColors.BOLD + IrcColors.RED + "Against" + IrcColors.RESET +
                ": " + Miscellaneous.getStringRepresentation(noVotes));

            if (electionRecord.wasElectionSuccessful()) {
                gameState.votePassed();
                // If 3 or more Fascist policies have been enacted, check if we've elected Hitler.  If so, game
                // over!
                if (gameState.canHitlerWinByElection()) {
                    if (parent.getHitler().equals(gameState.getChancellorCandidate())) {
                        channel.send().message("Game over!  After 3 or more Fascist policies were enacted, the " +
                            "Reichstag has elected the Hitler as their Chancellor and the Fascists seize power.  " +
                            "Dark days lie ahead for Germany... the " + IrcColors.BOLD + "Fascists" + IrcColors.BOLD + " have won.");
                        parent.gameEndCleanupAndStatsOutput(Party.FASCISTS);
                        parent.signalGameStopped();
                        return;
                    }
                }

                // Advance to the Legislative Session
                gameState.phaseElectionVotePassed();
                parent.beginLegislativeSession();
            } else {
                gameState.voteOrGovernmentFailed();

                if (gameState.isGovernmentInChaos()) {
                    parent.doChaos(false);
                } else {
                    gameState.setNextPresidentialCandidateSequentially();
                    gameState.phaseElectionVoteFailed();
                    parent.announceElectionPhase(false);
                }
            }
        }
    }

    public void handleDiscard(GenericMessageEvent<T> event, Player player, List<String> commands) {
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

        event.respond("Discarded policy " + policy + ", which was a " + IrcColors.BOLD + discardedPolicy.getPolicyType() +
            IrcColors.BOLD + " policy.");

        parent.generateAndSendLegislativeSessionInfoForChancellor(legislativeSessionPolicies, event);

        gameState.phaseToChancellorLegislation();
    }

    public void handleEnact(GenericMessageEvent<T> event, Player player, List<String> commands) {
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

        int policy;

        try {
            policy = Integer.parseInt(commands.get(1));
        } catch (NumberFormatException e) {
            // Should never happen
            event.respond("I didn't understand the policy number you provided: " + e.getMessage());
            return;
        }

        List<Policy> legislativeSessionPolicies = gameState.getLegislativeSessionPolicies();

        Policy policyToEnact = legislativeSessionPolicies.get(policy - 1);
        Policy policyToDiscard = legislativeSessionPolicies.get(0); // Only one left at this point

        gameState.discardPolicy(policyToDiscard);
        gameState.enactPolicy(policyToEnact);

        GameHelpers.voicePlayers(playerManager.getIngamePlayerList(), channel);

        channel.send().message("The Legislative Session has ended.  The President and Chancellor have enacted a " +
            IrcColors.BOLD + policyToEnact.getPolicyType() + IrcColors.BOLD + " policy.");

        if (parent.checkPolicyWinConditions()) {
            return;
        }

        gameState.reshufflePoliciesIfNecessary(channel);
        gameState.resetFailedGovernments();
        gameState.commitLegislativeSessionRecord(policyToEnact, false, false);

        ExecutiveActions.ExecutiveAction executiveAction = executiveActionTrack.get(gameState.getEnactedPolicies(PolicyType.FASCIST));

        if (policyToEnact.getPolicyType().equals(PolicyType.FASCIST) && executiveAction != null) {
            channel.send().message("The just-enacted Fascist policy has unlocked a use of the Executive Action " +
                IrcColors.BOLD + executiveAction + IrcColors.BOLD);
            gameState.phaseToExecutiveAction();
            parent.useExecutiveAction(executiveAction);
        } else {
            channel.send().message("No Executive Action is granted.");
            gameState.setNextPresidentialCandidateSequentially();
            gameState.nextRound(); // Advance round
            parent.announceElectionPhase(true);
        }
    }

    public void handleVeto(GenericMessageEvent<T> event, Player player, List<String> commands) {
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

                channel.send().message("The Chancellor " + IrcColors.BOLD + "wishes to veto this agenda" + IrcColors.BOLD + ".");

                parent.sendVetoInformation(event);

                gameState.phaseToVeto();
                break;
            case LEGISLATIVE_SESSION_VETO_POWER_CHECK:
                if (!player.equals(gameState.getPresidentialCandidate())) {
                    event.respond("Only the President may use this command!");
                    return;
                }

                if (commands.size() != 2) {
                    event.respond("You must specify \"" + SecretHitler.CMD_VETO + ' ' + SecretHitler.VOTE_YES + "\" or \"" + SecretHitler.CMD_VETO + " " +
                        SecretHitler.VOTE_NO + "\".");
                    return;
                }

                switch (commands.get(1)) {
                    case SecretHitler.VOTE_YES:
                        channel.send().message("The President agrees to the veto.  No policy is enacted.");

                        GameHelpers.voicePlayers(playerManager.getIngamePlayerList(), channel);

                        List<Policy> policies = gameState.getLegislativeSessionPolicies();
                        policies.forEach(gameState::discardPolicy);

                        gameState.voteOrGovernmentFailed();

                        if (gameState.isGovernmentInChaos()) {
                            parent.doChaos(true);
                        } else {
                            gameState.commitLegislativeSessionRecord(null, false, true);
                            gameState.reshufflePoliciesIfNecessary(channel);
                            // Round ends
                            // No policies played, so no Executive Action
                            gameState.setNextPresidentialCandidateSequentially();
                            gameState.nextRound(); // Advance round
                            parent.announceElectionPhase(true);
                        }
                        break;
                    case SecretHitler.VOTE_NO:
                        channel.send().message("The President has rejected the veto.");
                        event.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "The President " +
                            "has rejected your use of the Veto Power.");
                        gameState.vetoDenied();
                        parent.generateAndSendLegislativeSessionInfoForChancellor(gameState.getLegislativeSessionPolicies(), event);
                        break;
                }

                break;
        }
    }

    public void handleUseAction(GenericMessageEvent<T> event, Player player, List<String> commands) {
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

        ExecutiveActions.ExecutiveAction executiveAction = gameState.getAvailableExecutiveAction();

        if (executiveAction instanceof ExecutiveActions.InvestigateLoyalty) {
            if (playerStates.get(target).hasBeenInvestigated()) {
                event.respond("This player has been previously investigated and may not be investigated again.");
                return;
            }
        }

        executiveAction.accept(target);

        if (executiveAction.gameEnded()) {
            // Check if the executive action ended the game (Hitler was killed)
            return;
        }

        gameState.commitExecutiveActionRecord(executiveAction, target);

        // Advance to next round, but only select the next candidate sequentially if the Special Election was
        // not used
        if (!(executiveAction instanceof ExecutiveActions.CallSpecialElection)) {
            gameState.setNextPresidentialCandidateSequentially();
        }

        gameState.nextRound();
        parent.announceElectionPhase(true);
    }
}
