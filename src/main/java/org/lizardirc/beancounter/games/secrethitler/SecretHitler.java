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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.games.handler.Game;
import org.lizardirc.beancounter.games.handler.GameEntrypoint;
import org.lizardirc.beancounter.games.handler.GameHandler;
import org.lizardirc.beancounter.games.handler.Player;
import org.lizardirc.beancounter.utils.Miscellaneous;

@GameEntrypoint(properName = "Secret Hitler", commandName = "secrethitler", minimumPlayers = 5, maximumPlayers = 10,
    requiresChanops = true, usesPrivateMessageCommands =  true,
    summary = "A secret identity game inspired by Werewolf and Mafia", gameUrl = "http://secrethitler.com/")
@SuppressWarnings("unused")
public class SecretHitler<T extends PircBotX> implements Game<T> {
    private static final String CMD_MYROLE = "myrole";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_MYROLE);

    private final GameHandler<T> gameHandler;
    private final List<Player> players;
    private final Channel channel;

    private Set<Player> liberals = new HashSet<>();
    private Set<Player> fascists = new HashSet<>();
    private Player hitler;

    public SecretHitler(GameHandler<T> gameHandler, List<Player> players, Channel channel) {
        this.gameHandler = gameHandler;
        this.players = players;
        this.channel = channel;
    }

    @Override
    public Set<String> getActivePhaseCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
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
                if (liberals.contains(player)) {
                    event.getUser().send().message(Role.LIBERAL.getDescription());
                } else if (fascists.contains(player)) {
                    event.getUser().send().message(Role.FASCIST.getDescription());
                    event.getUser().send().message(getFascistPartyMembers());
                } else if (player.equals(hitler)) {
                    event.getUser().send().message(Role.HITLER.getDescription());
                    sendHitlerExtraInformationIfAppropriate(event);
                }
                break;
        }
    }

    @Override
    public void gameStart(GenericMessageEvent<T> event, ScheduledExecutorService scheduledExecutorService) {
        String everyone = Miscellaneous.getStringRepresentation(players.stream()
            .map(Player::getNickname)
            .collect(Collectors.toList()), " ");

        int playerCount = players.size();

        voiceAllPlayers();
        channel.send().setModerated(channel); // pircbotx wat

        channel.send().message(everyone + ": Welcome to Secret Hitler, the hidden identity game inspired by Werewolf and Mafia.  Can you find and kill the Secret Hitler before it's too late?");

        Map<Role, Integer> roleCount = Helpers.getRoleCount(playerCount);

        channel.send().message("There are \002" + playerCount + "\002 players: \002" + roleCount.get(Role.LIBERAL) +
            "\002 Liberals, \002" + roleCount.get(Role.FASCIST) + "\002 Fascists, and \002" + roleCount.get(Role.HITLER) +
            "\002 Hitler.");

        channel.send().message("Please wait while I assign roles send out role information to all players....");

        doRoleAssignment(roleCount);

        liberals.forEach(player -> event.getBot().sendIRC().message(player.getNick(), Role.LIBERAL.getDescription()));
        fascists.forEach(player -> event.getBot().sendIRC().message(player.getNick(), Role.FASCIST.getDescription()));
        event.getBot().sendIRC().message(hitler.getNick(), Role.HITLER.getDescription());

        fascists.forEach(player -> event.getBot().sendIRC().message(player.getNick(), getFascistPartyMembers()));

        sendHitlerExtraInformationIfAppropriate(event);

        channel.send().message("All players, please now check your PMs for your role information.  If you forget, you can use the \"" + CMD_MYROLE + "\" command to be reminded.");
    }

    @Override
    public void gameHaltRequest(GenericMessageEvent<T> event) {
        doCleanup();
    }

    @Override
    public void playerQuit(Player player) {

    }

    @Override
    public void playersCommandHook(GenericMessageEvent<T> event) {

    }

    @Override
    public void playerNicknameChanged(String newNickname, String oldNickname) {

    }

    private void doCleanup() {
        channel.send().removeModerated(channel);
        devoiceAllPlayers();
    }

    private void voiceAllPlayers() {
        doVoiceAction('+');
    }

    private void devoiceAllPlayers() {
        doVoiceAction('-');
    }

    private void doVoiceAction(char operator) {
        // More efficient to determine the number of modes we can set at a time and set the modes manually rather than
        // use PircBotX's built-in mode setting.

        int playerCount = players.size();
        int maxModes = channel.getBot().getServerInfo().getMaxModes();

        for (int i = 0; i < playerCount; i += maxModes) {
            Set<String> toVoice = new HashSet<>();

            for (int j = i; j < (i + maxModes) && j < playerCount; j++) {
                toVoice.add(players.get(j).getNick());
            }

            String v = Strings.repeat("v", toVoice.size());
            String nicks = Miscellaneous.getStringRepresentation(toVoice, " ");
            channel.getBot().sendRaw().rawLine("MODE " + channel.getName() + ' ' + operator + v + ' ' + nicks);
        }
    }

    private String getFascistPartyMembers() {
        return "The Fascists are: " +
            Miscellaneous.getStringRepresentation(fascists.stream()
                .map(Player::getNick)
                .collect(Collectors.toList())
            ) + "; the Hitler is: " + hitler.getNickname();
    }

    private void sendHitlerExtraInformationIfAppropriate(GenericEvent<T> event) {
        int playerCount = players.size();

        if (playerCount >= 5 && playerCount <= 6) {
            event.getBot().sendIRC().message(hitler.getNick(), "The Fascists are: " +
                Miscellaneous.getStringRepresentation(fascists.stream()
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
                            liberals.add(player);
                            numLiberals++;
                            break outer;
                        }
                        break;
                    case 1:
                        // Fascist
                        if (numFascists < roleCount.get(Role.FASCIST)) {
                            fascists.add(player);
                            numFascists++;
                            break outer;
                        }
                        break;
                    case 2:
                        // Hitler!
                        if (numHitlers < roleCount.get(Role.HITLER)) {
                            hitler = player;
                            numHitlers++;
                            break outer;
                        }
                        break;
                }
            }
        }
    }
}
