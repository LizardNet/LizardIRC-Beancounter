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

import org.lizardirc.beancounter.games.handler.Game;
import org.lizardirc.beancounter.games.handler.GameEntrypoint;
import org.lizardirc.beancounter.games.handler.GameHandler;
import org.lizardirc.beancounter.games.handler.GameHelpers;
import org.lizardirc.beancounter.games.handler.Player;
import org.lizardirc.beancounter.utils.Miscellaneous;
import org.lizardirc.beancounter.utils.Pair;

@GameEntrypoint(properName = "Secret Hitler", commandName = "secrethitler", minimumPlayers = 5, maximumPlayers = 10,
    requiresChanops = true, usesPrivateMessageCommands =  true,
    summary = "A secret identity game inspired by Werewolf and Mafia", gameUrl = "http://secrethitler.com/")
@SuppressWarnings("unused")
public class SecretHitler<T extends PircBotX> implements Game<T> {
    private static final String CMD_MYROLE = "myrole";
    private static final String CMD_NOMINATE  = "nominate";
    private static final String CMD_VOTE = "vote";
    private static final String VOTE_YES = "yes";
    private static final String VOTE_NO = "no";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_MYROLE, CMD_NOMINATE, CMD_VOTE);
    private static final Set<String> VOTE_SUBCMDS = ImmutableSet.of(VOTE_YES, VOTE_NO);

    private final GameHandler<T> gameHandler;
    private final List<Player> players;
    private final Channel channel;

    private final Map<Player, PlayerState> playerStates = new HashMap<>();
    private final GameState gameState;

    public SecretHitler(GameHandler<T> gameHandler, List<Player> players, Channel channel) {
        this.gameHandler = gameHandler;
        this.players = players;
        this.channel = channel;
        gameState = new GameState(this.players);
    }

    @Override
    public Set<String> getActivePhaseCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1) {
            switch (commands.get(0)) {
                case CMD_NOMINATE:
                    return ImmutableSet.copyOf(players.stream()
                        .map(Player::getNick)
                        .collect(Collectors.toSet())
                    );
            }
        }

        return Collections.emptySet();
    }

    @Override
    public void handleActivePhaseCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        Player player = new Player(event.getUser());

        switch (commands.get(0)) {
            case CMD_MYROLE:
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

                Player chancellorNominee = new Player(event.getBot().getUserChannelDao().getUser(commands.get(1)));

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
                channel.send().message("Time to vote to elect this President/Chancellor pair! All players - including the " +
                    "candidates - please PM the bot \"" + CMD_VOTE + ' ' + VOTE_YES + "\" or \"" + CMD_VOTE + ' ' + VOTE_NO +
                    "\" to cast your vote.  There must be more \"" + VOTE_YES + "\" votes than \"" + VOTE_NO + "\" votes " +
                    "for this pair to be elected."
                );
                gameState.nextPhase();
                break;
        }
    }

    @Override
    public void gameStart(GenericMessageEvent<T> event, ScheduledExecutorService scheduledExecutorService) {
        String everyone = Miscellaneous.getStringRepresentation(players.stream()
            .map(Player::getNickname)
            .collect(Collectors.toList()), " ");

        gameState.gameStartHook();
        int playerCount = players.size();

        GameHelpers.voicePlayers(players, channel);
        channel.send().setModerated(channel); // pircbotx wat

        channel.send().message(everyone + ": Welcome to Secret Hitler, the hidden identity game inspired by Werewolf and Mafia.  Can you find and kill the Secret Hitler before it's too late?");

        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);

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

        gameState.setNextPresidentialCandidateRandomly();

        beginElectionPhase(true);
    }

    @Override
    public void gameHaltRequest(GenericMessageEvent<T> event) {
        channel.send().message("The game is being forcibly stopped.  Nobody wins.");
        gameEndCleanupAndStatsOutput();
    }

    @Override
    public void playerQuit(Player player) {
        channel.send().message('\002' + player.getNick() + "\002 has left the game.  Unfortunately, the game cannot continue and it must now end.  Nobody wins.");
        gameEndCleanupAndStatsOutput();
        gameHandler.signalGameStopped(this);
    }

    @Override
    public void playersCommandHook(GenericMessageEvent<T> event) {
        int playerCount = gameState.getPlayerCountAtGameStart();
        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);

        channel.send().message("At game start, there were \002" + playerCount + "\002 players: \002" + roleCount.get(Role.LIBERAL) +
            "\002 Liberals, \002" + roleCount.get(Role.FASCIST) + "\002 Fascists, and \002" + roleCount.get(Role.HITLER) +
            "\002 Hitler.");
    }

    @Override
    public void playerNicknameChanged(String newNickname, String oldNickname) {
        channel.send().setMode("+v-v ", newNickname, oldNickname);
    }

    private void gameEndCleanupAndStatsOutput() {
        channel.send().message("The Liberals were: " + Miscellaneous.getStringRepresentation(getLiberals().stream()
            .map(Player::getNick)
            .collect(Collectors.toList())
        ));
        channel.send().message("The Fascists were: " + Miscellaneous.getStringRepresentation(getFascists().stream()
            .map(Player::getNick)
            .collect(Collectors.toList())
        ));
        channel.send().message("The Hitler was: " + getHitler().getNick());
        channel.send().removeModerated(channel);
        GameHelpers.devoicePlayers(players, channel);
    }

    private String getFascistPartyMembersAsInformativeString() {
        return "The Fascists are: " +
            Miscellaneous.getStringRepresentation(getFascists().stream()
                .map(player -> new Pair<>(player, players.contains(player)))
                .map(pair -> pair.getLeft().getNick() + (pair.getRight() ? " (executed)" : ""))
                .collect(Collectors.toList())
            ) + "; the Hitler is: " + getHitler().getNickname();
    }

    private void sendHitlerExtraInformationIfAppropriate(GenericEvent<T> event) {
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

        for (Player player : players) {
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
            "were either the previously elected President or Chancellor: ");

        boolean playersAreIneligible = false;

        if (gameState.getPreviousElectedChancellor() != null) {
            sb.append(gameState.getPreviousElectedChancellor().getNick());
            sb.append(' ');
            playersAreIneligible = true;
        }

        if (gameState.getPreviousElectedPresident() != null) {
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
    }
}
