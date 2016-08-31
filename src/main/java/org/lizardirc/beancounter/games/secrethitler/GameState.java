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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;

import org.lizardirc.beancounter.gameframework.playermanagement.Player;
import org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager;

class GameState {
    private static final int TOTAL_LIBERAL_ARTICLES = 6;
    private static final int TOTAL_FASCIST_ARTICLES = 11;

    private final PlayerManager playerManager;

    private final Map<PolicyType, Integer> enactedPolicies = new EnumMap<>(PolicyType.class);

    private LinkedList<Policy> policyDeck = new LinkedList<>();
    private LinkedList<Policy> discardDeck = new LinkedList<>();

    private int playerCountAtGameStart;

    private int roundNumber = 1;
    private RoundPhase roundPhase = RoundPhase.ELECTION_NOMINATION;

    private int failedGovernments = 0;

    private int presidentialCandidatePtr = 0;
    private Player presidentialCandidate;
    private Player chancellorCandidate;

    // These players are ineligible to become the next Chancellor
    private Player previousElectedPresident = null;
    private Player previousElectedChancellor = null;

    private Map<Player, Boolean> votingMap = null;

    private List<Policy> legislativeSessionPolicies = null;

    private boolean vetoUsedThisRound = false;

    private ExecutiveActions.ExecutiveAction availableExecutiveAction = null;

    // This is a map of roundNumber -> List<ElectionRecord>, where the List<ElectionRecord> is all the elctions that
    // took place in that round, in order - generally, the final election in the list will be the successful one
    private Map<Integer, List<ElectionRecord>> perRoundElectionRecords = new HashMap<>();
    // Same, but for legislative records
    private Map<Integer, LegislativeSessionRecord> perRoundLegislationRecords = new HashMap<>();
    // Same, but for executive actions (not every round will have one of these, so expect nulls even for past rounds)
    private Map<Integer, ExecutiveActionRecord> perRoundExecutiveActions = new HashMap<>();

    public GameState(PlayerManager playerManager) {
        this.playerManager = Objects.requireNonNull(playerManager);
        enactedPolicies.put(PolicyType.LIBERAL, 0);
        enactedPolicies.put(PolicyType.FASCIST, 0);

        for (int i = 0; i < TOTAL_LIBERAL_ARTICLES; i++) {
            policyDeck.add(new Policy(PolicyType.LIBERAL));
        }

        for (int i = 0; i < TOTAL_FASCIST_ARTICLES; i++) {
            policyDeck.add(new Policy(PolicyType.FASCIST));
        }

        Collections.shuffle(policyDeck);
    }

    public void gameStartHook() {
        if (playerCountAtGameStart != 0) {
            throw new IllegalStateException("gameStartHook() may not be called more than once");
        } else {
            playerCountAtGameStart = playerManager.getIngamePlayerCount();
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
        if (presidentialCandidatePtr >= playerManager.getIngamePlayerCount()) {
            presidentialCandidatePtr = 0;
        }

        presidentialCandidate = playerManager.getIngamePlayerList().get(presidentialCandidatePtr);
    }

    public void setNextPresidentialCandidateTo(Player player) {
        presidentialCandidate = player;
    }

    public void setNextPresidentialCandidateRandomly() {
        // Only used at game start
        Random random = new Random();
        presidentialCandidatePtr = random.nextInt(playerManager.getIngamePlayerCount());
        presidentialCandidate = playerManager.getIngamePlayerList().get(presidentialCandidatePtr);
    }

    public void voteOrGovernmentFailed() {
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

    public void phaseToElectionVote() {
        roundPhase = RoundPhase.ELECTION_VOTE;
        votingMap = new HashMap<>();
    }

    public void phaseToPresidentLegislation() {
        roundPhase = RoundPhase.LEGISLATIVE_SESSION_PRESIDENT_VOTE;
        votingMap = null;
    }

    public void phaseElectionVotePassed() {
        phaseToPresidentLegislation();
    }

    public void phaseElectionVoteFailed() {
        roundPhase = RoundPhase.ELECTION_NOMINATION;
        votingMap = null;
    }

    public void phaseToChancellorLegislation() {
        roundPhase = RoundPhase.LEGISLATIVE_SESSION_CHANCELLOR_VOTE;
    }

    public void phaseToExecutiveAction() {
        roundPhase = RoundPhase.EXECUTIVE_ACTION;
        legislativeSessionPolicies = null;
        vetoUsedThisRound = false;
    }

    public void nextRound() {
        roundNumber++;
        availableExecutiveAction = null;
        roundPhase = RoundPhase.ELECTION_NOMINATION;
    }

    public boolean canVeto() {
        return enactedPolicies.get(PolicyType.FASCIST) >= 5;
    }

    public boolean canHitlerWinByElection() {
        return enactedPolicies.get(PolicyType.FASCIST) >= 3;
    }

    public void votePassed() {
        previousElectedChancellor = chancellorCandidate;
        previousElectedPresident = presidentialCandidate;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isChancellorCandidateEligible(Player player) {
        Objects.requireNonNull(player);

        return !((player.equals(previousElectedPresident) && playerManager.getIngamePlayerCount() > 5) || player.equals(previousElectedChancellor));
    }

    public Player getPreviousElectedPresident() {
        return previousElectedPresident;
    }

    public Player getPreviousElectedChancellor() {
        return previousElectedChancellor;
    }

    public Player getChancellorCandidate() {
        return chancellorCandidate;
    }

    public void setChancellorCandidate(Player player) {
        chancellorCandidate = player;
    }

    public void resetFailedGovernments() {
        failedGovernments = 0;
    }

    public void resetTermLimits() {
        previousElectedPresident = null;
        previousElectedChancellor = null;
    }

    public void executePlayer(Player player) {
        int executedPlayersOrdinalPosition = playerManager.getIngamePlayerList().indexOf(player);

        if (executedPlayersOrdinalPosition == -1) {
            throw new IllegalArgumentException("Specified player is not in game.");
        }

        if (presidentialCandidatePtr >= executedPlayersOrdinalPosition) {
            presidentialCandidatePtr--;
        }

        playerManager.removePlayer(player);
    }

    public Map<Player, Boolean> getVotingMap() {
        if (RoundPhase.ELECTION_VOTE.equals(roundPhase)) {
            return votingMap;
        } else {
            throw new IllegalStateException("The voting map is only accessible during the ELECTION_VOTE phase.");
        }
    }

    public boolean allPlayersHaveVoted() {
        if (!RoundPhase.ELECTION_VOTE.equals(roundPhase)) {
            throw new IllegalStateException("The voting map is only accessible during the ELECTION_VOTE phase.");
        }

        Set<Player> playerSet = new HashSet<>(playerManager.getIngamePlayerList());

        return playerSet.equals(votingMap.keySet());
    }

    public ElectionRecord getResultsAndCommitElectionToRecord() {
        if (!RoundPhase.ELECTION_VOTE.equals(roundPhase)) {
            throw new IllegalStateException("The voting map is only accessible during the ELECTION_VOTE phase.");
        }

        List<ElectionRecord> electionRecords = perRoundElectionRecords.get(roundNumber);

        if (electionRecords == null) {
            electionRecords = new ArrayList<>();
            perRoundElectionRecords.put(roundNumber, electionRecords);
        }

        Set<Player> yesVotes = ImmutableSet.copyOf(votingMap.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet()));

        Set<Player> noVotes = ImmutableSet.copyOf(votingMap.entrySet().stream()
            .filter(e -> !e.getValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet()));

        ElectionRecord electionRecord = new ElectionRecord(yesVotes, noVotes, presidentialCandidate, chancellorCandidate);
        electionRecords.add(electionRecord);

        return electionRecord;
    }

    public List<ElectionRecord> getElectionRecordForRound(int round) {
        return perRoundElectionRecords.get(round);
    }

    public int getEnactedPolicies(PolicyType policyType) {
        return enactedPolicies.get(policyType);
    }

    public Policy drawTopPolicy() {
        return policyDeck.pop();
    }

    public Policy[] doPolicyPeek() {
        return new Policy[]{ policyDeck.get(0), policyDeck.get(1), policyDeck.get(2) };
    }

    public void reshufflePoliciesIfNecessary(Channel channel) {
        if (policyDeck.size() < 3) {
            channel.send().message("\002The policy deck has less than 3 cards.  Discards have been added back to the policy deck, and the deck has been reshuffled.\002");

            discardDeck.addAll(policyDeck);
            Collections.shuffle(discardDeck);
            policyDeck = discardDeck;
            discardDeck = new LinkedList<>();
        }
    }

    public void enactPolicy(Policy policy) {
        switch (policy.getPolicyType()) {
            case LIBERAL:
                int i = enactedPolicies.get(PolicyType.LIBERAL);
                enactedPolicies.put(PolicyType.LIBERAL, i + 1);
                break;
            case FASCIST:
                int j = enactedPolicies.get(PolicyType.FASCIST);
                enactedPolicies.put(PolicyType.FASCIST, j + 1);
                break;
        }
    }

    public void discardPolicy(Policy policy) {
        discardDeck.push(policy);
    }

    public void setLegislativeSessionPolicies(List<Policy> policies) {
        legislativeSessionPolicies = policies;
    }

    public List<Policy> getLegislativeSessionPolicies() {
        return legislativeSessionPolicies;
    }

    public boolean liberalsWinByPolicies() {
        return enactedPolicies.get(PolicyType.LIBERAL) >= 5;
    }

    public boolean fascistsWinByPolicies() {
        return enactedPolicies.get(PolicyType.FASCIST) >= 6;
    }

    public void phaseToVeto() {
        roundPhase = RoundPhase.LEGISLATIVE_SESSION_VETO_POWER_CHECK;
    }

    public void vetoDenied() {
        if (!RoundPhase.LEGISLATIVE_SESSION_VETO_POWER_CHECK.equals(roundPhase)) {
            throw new IllegalStateException("The vetoDenied() method may only be called during the Veto Power Check phase");
        } else {
            roundPhase = RoundPhase.LEGISLATIVE_SESSION_CHANCELLOR_VOTE;
            vetoUsedThisRound = true;
        }
    }

    public boolean isVetoUsedThisRound() {
        return vetoUsedThisRound;
    }

    public void commitLegislativeSessionRecord(Policy enactedPolicy, boolean enactedInChaos, boolean vetoPowerUsed) {
        LegislativeSessionRecord legislativeRecord;

        if (RoundPhase.ELECTION_VOTE.equals(roundPhase)) {
            legislativeRecord = new LegislativeSessionRecord(null, null, enactedPolicy.getPolicyType(), true, false);
        } else {
            legislativeRecord = new LegislativeSessionRecord(presidentialCandidate, chancellorCandidate, enactedPolicy.getPolicyType(), enactedInChaos, vetoPowerUsed);
        }

        perRoundLegislationRecords.put(roundNumber, legislativeRecord);
    }

    public LegislativeSessionRecord getLegislativeRecordForRound(int round) {
        return perRoundLegislationRecords.get(round);
    }

    public ExecutiveActions.ExecutiveAction getAvailableExecutiveAction() {
        return availableExecutiveAction;
    }

    public void setAvailableExecutiveAction(ExecutiveActions.ExecutiveAction availableExecutiveAction) {
        this.availableExecutiveAction = availableExecutiveAction;
    }
    
    public void commitExecutiveActionRecord(ExecutiveActions.ExecutiveAction executiveAction, Player target) {
        perRoundExecutiveActions.put(roundNumber, new ExecutiveActionRecord(executiveAction, presidentialCandidate, target));
    }
    
    public ExecutiveActionRecord getExecutiveActionRecordForRound(int round) {
        return perRoundExecutiveActions.get(round);
    }
}
