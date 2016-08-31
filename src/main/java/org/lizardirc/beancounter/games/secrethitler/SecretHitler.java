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
import org.pircbotx.hooks.types.GenericEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.gameframework.Game;
import org.lizardirc.beancounter.gameframework.GameEntrypoint;
import org.lizardirc.beancounter.gameframework.GameHandler;
import org.lizardirc.beancounter.gameframework.GameHelpers;
import org.lizardirc.beancounter.gameframework.playermanagement.Player;
import org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager;
import org.lizardirc.beancounter.utils.IrcColors;
import org.lizardirc.beancounter.utils.Miscellaneous;
import org.lizardirc.beancounter.utils.Pair;

@GameEntrypoint(properName = "Secret Hitler", commandName = "secrethitler", minimumPlayers = 5, maximumPlayers = 10,
    requiresChanops = true, voicePlayersDuringSetup = true, usesPrivateMessageCommands =  true,
    summary = "A secret identity game inspired by Werewolf and Mafia", gameUrl = "http://secrethitler.com/")
@SuppressWarnings("unused")
public class SecretHitler<T extends PircBotX> implements Game<T> {
    static final String CMD_MYROLE = "myrole";
    static final String CMD_NOMINATE  = "nominate";
    static final String CMD_VOTE = "vote";
    static final String CMD_ELECTION_RESULTS = "electionresults";
    static final String CMD_STATUS = "status";
    static final String CMD_SHOWTRACK = "showtrack";
    static final String CMD_DISCARD = "discard";
    static final String CMD_ENACT = "enact";
    static final String CMD_VETO = "veto";
    static final String CMD_LEGISLATIVE_RESULTS = "legislativeresults";
    static final String CMD_EXECUTIVE_ACTIONS = "executiveactions";
    static final String CMD_USE_ACTION = "useaction"; // Package-local so ExecutiveActions can use this
    static final String VOTE_YES = "yes";
    static final String VOTE_NO = "no";
    static final Set<String> COMMANDS = ImmutableSet.of(CMD_MYROLE, CMD_NOMINATE, CMD_VOTE, CMD_ELECTION_RESULTS,
        CMD_DISCARD, CMD_VETO, CMD_LEGISLATIVE_RESULTS, CMD_EXECUTIVE_ACTIONS, CMD_USE_ACTION, CMD_STATUS, CMD_SHOWTRACK,
        CMD_ENACT);
    static final Set<String> VOTE_SUBCMDS = ImmutableSet.of(VOTE_YES, VOTE_NO);

    private final GameHandler<T> gameHandler;
    private final PlayerManager playerManager;
    private final Channel channel;

    private final Map<Player, PlayerState> playerStates = new HashMap<>();
    private final GameState gameState;
    private final Commands<T> commandResponder;

    private Map<Integer, ExecutiveActions.ExecutiveAction> executiveActionTrack;

    public SecretHitler(GameHandler<T> gameHandler, PlayerManager playerManager, Channel channel) {
        this.gameHandler = gameHandler;
        this.playerManager = playerManager;
        this.channel = channel;
        gameState = new GameState(this.playerManager);
        commandResponder = new Commands<>(this);
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
                commandResponder.handleMyrole(event, player);
                break;
            case CMD_ELECTION_RESULTS:
                commandResponder.handleElectionResults(event, remainder);
                break;
            case CMD_LEGISLATIVE_RESULTS:
                commandResponder.handleLegislativeResults(event, remainder);
                break;
            case CMD_EXECUTIVE_ACTIONS:
                commandResponder.handleExecutiveActions(event, remainder);
                break;
            case CMD_STATUS:
                commandResponder.handleStatus(event);
                break;
            case CMD_SHOWTRACK:
                commandResponder.handleShowTrack(event);
                break;
            case CMD_NOMINATE:
                commandResponder.handleNominate(event, player, commands);
                break;
            case CMD_VOTE:
                commandResponder.handleVote(event, player, commands);
                break;
            case CMD_DISCARD:
                commandResponder.handleDiscard(event, player, commands);
                break;
            case CMD_ENACT:
                commandResponder.handleEnact(event, player, commands);
                break;
            case CMD_VETO:
                commandResponder.handleVeto(event, player, commands);
                break;
            case CMD_USE_ACTION:
                commandResponder.handleUseAction(event, player, commands);
                break;
        }
    }

    @Override
    public void gameStart(GenericMessageEvent<T> event, ScheduledExecutorService scheduledExecutorService) {
        String everyone = Miscellaneous.getStringRepresentation(playerManager.getIngamePlayerList().stream()
            .map(Player::getNick)
            .collect(Collectors.toList()), " ");

        gameState.gameStarted();
        int playerCount = playerManager.getIngamePlayerCount();

        GameHelpers.voicePlayers(playerManager.getIngamePlayerList(), channel);
        channel.send().setModerated(channel); // pircbotx wat

        channel.send().message(everyone + ": Welcome to Secret Hitler, the hidden identity game inspired by Werewolf and Mafia.  Can you find and kill the Secret Hitler before it's too late?");

        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);
        ExecutiveActions<T> executiveActions = new ExecutiveActions<>(this);
        executiveActionTrack = executiveActions.getExecutiveActionTrack(playerCount);

        channel.send().message("There are " + IrcColors.BOLD + playerCount + IrcColors.BOLD + " players: " +
            IrcColors.BOLD + roleCount.get(Role.LIBERAL) + IrcColors.BOLD + " Liberals, " + IrcColors.BOLD +
            roleCount.get(Role.FASCIST) + IrcColors.BOLD + " Fascists, and " + IrcColors.BOLD +
            roleCount.get(Role.HITLER) + IrcColors.BOLD + " Hitler.");

        channel.send().message(IrcColors.BOLD + "Please note" + IrcColors.BOLD + ": Players are " + IrcColors.BOLD +
            "not allowed" + IrcColors.BOLD + " to exchange any game-related information outside of this channel, " +
            "including in private message, at any time.");

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

        announceElectionPhase(true);
    }

    @Override
    public void gameHaltRequest(GenericMessageEvent<T> event) {
        channel.send().message("The game is being forcibly stopped.  Nobody wins.");
        gameEndCleanupAndStatsOutput();
    }

    @Override
    public void handlePlayerQuit(Player player) {
        channel.send().message(IrcColors.BOLD + player.getNick() + IrcColors.BOLD + " has left the game.  Unfortunately," +
            " the game cannot continue and it must now end.  Nobody wins.");
        gameEndCleanupAndStatsOutput();
        gameHandler.signalGameStopped(this);
    }

    @Override
    public void playersCommandHook(GenericMessageEvent<T> event) {
        int playerCount = gameState.getPlayerCountAtGameStart();
        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);

        event.respond("At game start, there were " + IrcColors.BOLD + playerCount + IrcColors.BOLD + " players: " + IrcColors.BOLD + roleCount.get(Role.LIBERAL) +
            "" + IrcColors.BOLD + " Liberals, " + IrcColors.BOLD + roleCount.get(Role.FASCIST) + IrcColors.BOLD + " Fascists, and " + IrcColors.BOLD + roleCount.get(Role.HITLER) +
            "" + IrcColors.BOLD + " Hitler.");
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

    List<Player> getLiberals() {
        return getPlayersWithRole(Role.LIBERAL);
    }

    List<Player> getFascists() {
        return getPlayersWithRole(Role.FASCIST);
    }

    Player getHitler() {
        return getPlayersWithRole(Role.HITLER).get(0); // There's only ever one hitler
    }

    void announceElectionPhase(boolean introduceNewRound) {
        if (introduceNewRound) {
            channel.send().message("Round " + IrcColors.BOLD + gameState.getRoundNumber() + IrcColors.BOLD + "; Election Phase");
        }

        channel.send().message("The Presidential Candidate is " + IrcColors.BOLD + gameState.getPresidentialCandidate().getNick() +
            IrcColors.BOLD + ".  " + gameState.getPresidentialCandidate().getNick() + ", please discuss with everyone " +
            "who you want to be chancellor, and when you're ready, nominate them with the command \"" + CMD_NOMINATE + " [playerName]\"."
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

    void doChaos(boolean becauseOfVeto) {
        channel.send().message("Because three governments in a row have failed, the country is now " +
            "in Chaos.  The people, frustrated, rise up and enact the next policy in the deck, for " +
            "better or for worse....");
        if (RoundPhase.ELECTION_VOTE.equals(gameState.getRoundPhase())) {
            gameState.phaseToPresidentLegislation(); // Legislative Session
        }
        Policy toEnact = gameState.drawTopPolicy();
        channel.send().message("The people have enacted, by force, a " + IrcColors.BOLD + toEnact.getPolicyType() +
            IrcColors.BOLD + " policy.  Because the people enacted this policy, no Executive Action is granted.");
        gameState.enactPolicy(toEnact);
        gameState.commitLegislativeSessionRecord(toEnact, true, becauseOfVeto);

        channel.send().message("There have now been " + IrcColors.BOLD + gameState.getEnactedPolicies(PolicyType.LIBERAL) +
            IrcColors.BOLD + " Liberal policies enacted, and " + IrcColors.BOLD +
            gameState.getEnactedPolicies(PolicyType.FASCIST) + IrcColors.BOLD + " Fascist policies have been enacted.");

        if (toEnact.getPolicyType().equals(PolicyType.FASCIST)) {
            ExecutiveActions.ExecutiveAction executiveAction = executiveActionTrack.get(gameState.getEnactedPolicies(PolicyType.FASCIST));

            if (executiveAction != null) {
                channel.send().message("(This policy enactment would have unlocked a usage of the " + IrcColors.BOLD +
                    executiveAction.toString() + IrcColors.BOLD + " Executive Action)");
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
        announceElectionPhase(true); // New round
    }

    void beginLegislativeSession() {
        channel.send().message("Round " + IrcColors.BOLD + gameState.getRoundNumber() + IrcColors.BOLD + "; Legislative Session");
        GameHelpers.devoicePlayers(playerManager.getIngamePlayerList(), channel);
        channel.send().message(IrcColors.BOLD + "Quiet please" + IrcColors.BOLD + "- All player communication - " +
            IrcColors.BOLD + "including private messages" + IrcColors.BOLD + " - is " + IrcColors.BOLD + "forbidden" +
            IrcColors.BOLD + " while the Reichstag is in session.");
        channel.send().message("President and Chancellor, please check your private messages for instructions.  Everyone" +
            " else, please wait for the Legislative Session to end.");
        channel.send().message("So far, " + IrcColors.BOLD + gameState.getEnactedPolicies(PolicyType.LIBERAL) +
            IrcColors.BOLD + " Liberal policies have been enacted, and " + IrcColors.BOLD +
            gameState.getEnactedPolicies(PolicyType.FASCIST) + IrcColors.BOLD + " Fascist policies have been enacted.");


        channel.getBot().sendIRC().message(gameState.getChancellorCandidate().getNick(), "Please wait for the President to make policy selections.");

        List<Policy> policies = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            policies.add(gameState.drawTopPolicy());
        }

        generateAndSendLegislativeSessionInfoForPresident(policies);

        gameState.setLegislativeSessionPolicies(policies);
    }

    boolean checkPolicyWinConditions() {
        if (gameState.liberalsWinByPolicies()) {
            channel.send().message("Game over!  Five Liberal policies have been enacted, causing the Liberals to " +
                "secure their place as the rightful leaders of Germany.  The " + IrcColors.BOLD + "Liberals" +
                IrcColors.BOLD + " win the game!");
            gameEndCleanupAndStatsOutput(Party.LIBERALS);
            gameHandler.signalGameStopped(this);
            return true;
        } else if (gameState.fascistsWinByPolicies()) {
            channel.send().message("Game over!  Six Fascist policies have been enacted, allowing the Fascists to " +
                "install the Hitler as the Chancellor by force.  The coup is but a hint of things to come, for the " +
                IrcColors.BOLD + "Fascists" + IrcColors.BOLD + " have won the game.");
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

    Map<Integer, ExecutiveActions.ExecutiveAction> getExecutiveActionTrack() {
        return executiveActionTrack;
    }

    PlayerManager getPlayerManager() {
        return playerManager;
    }

    void useExecutiveAction(ExecutiveActions.ExecutiveAction executiveAction) {
        channel.send().message("Round " + IrcColors.BOLD + gameState.getRoundNumber() + IrcColors.BOLD + "; Executive Action");

        if (executiveAction instanceof ExecutiveActions.PolicyPeek) {
            executiveAction.accept(null); // This one doesn't require any arguments, so immediately use it then proceed to next round
            gameState.commitExecutiveActionRecord(executiveAction, null);
            gameState.setNextPresidentialCandidateSequentially();
            gameState.nextRound(); // Begin new round
            announceElectionPhase(true);
        } else {
            channel.send().message("President, please check your private messages for instructions.");

            showExecutiveActionUsageInstructions(executiveAction);

            gameState.setAvailableExecutiveAction(executiveAction);
        }
    }

    void generateAndSendLegislativeSessionInfoForPresident(List<Policy> policies) {
        StringBuilder sb = new StringBuilder("President, the following policies are up selection this round: ");

        for (int i = 0; i < policies.size(); i++) {
            sb.append(IrcColors.BOLD)
                .append('[')
                .append(i + 1)
                .append(']')
                .append(IrcColors.BOLD)
                .append(": ")
                .append(policies.get(i))
                .append("; ");
        }

        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), sb.substring(0, sb.length() - 2));
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "Please PM me the command \"" +
            CMD_DISCARD + " number\", where \"number\" is the policy number you wish to " + IrcColors.BOLD + "discard" +
            IrcColors.BOLD + ".  The remaining two policies will be sent to the Chancellor, who will pick one from the two to enact.");
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "Remember - you MAY NOT communicate " +
            "with the Chancellor in ANY WAY during the Legislative Session, INCLUDING private messages!");
    }

    void generateAndSendLegislativeSessionInfoForChancellor(List<Policy> legislativeSessionPolicies, GenericMessageEvent<T> event) {
        StringBuilder sb = new StringBuilder("Chancellor, the President has discarded a policy and presented " +
            "you with the following policy choices: ");

        for (int i = 0; i < legislativeSessionPolicies.size(); i++) {
            sb.append(IrcColors.BOLD)
                .append('[')
                .append(i + 1)
                .append(']')
                .append(IrcColors.BOLD)
                .append(": ")
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

    void sendVetoInformation(GenericMessageEvent<T> event) {
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

    void showExecutiveActionUsageInstructions(ExecutiveActions.ExecutiveAction executiveAction) {
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), executiveAction.getInstructions());
        channel.getBot().sendIRC().message(gameState.getPresidentialCandidate().getNick(), "You must use this executive " +
            "action here and now - passing is not allowed!");
    }

    void signalGameStopped() {
        gameHandler.signalGameStopped(this);
    }
}
