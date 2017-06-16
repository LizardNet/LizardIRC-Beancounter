/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.admin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.gameframework.GameHandler;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class AdminHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_QUIT = "quit";
    private static final String CMD_NICK = "nick";
    private static final String CMD_JOIN = "JoinChannel";
    private static final String CMD_PART = "part";
    private static final String CMD_SAY = "say";
    private static final String CMD_ACT = "act";
    private static final String CMD_RAW = "raw";
    private static final String CMD_LIST = "listchans";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_QUIT, CMD_NICK, CMD_JOIN, CMD_PART, CMD_SAY, CMD_ACT, CMD_RAW, CMD_LIST);

    private static final String PERM_QUIT = CMD_QUIT;
    private static final String PERM_NICK = CMD_NICK;
    private static final String PERM_JOIN = CMD_JOIN;
    private static final String PERM_PART = CMD_PART;
    private static final String PERM_SAY = CMD_SAY;
    private static final String PERM_ACT = CMD_SAY; // Important! Take note!
    private static final String PERM_RAW = CMD_RAW;
    private static final String PERM_LIST = CMD_LIST;

    private static final String QUIT_OPT_FORCE = "--force";
    private static final String QUIT_OPT_LATER = "--later";
    private static final String QUIT_OPT_CANCEL = "--cancel";
    private static final Set<String> QUIT_OPTS = ImmutableSet.of(QUIT_OPT_FORCE, QUIT_OPT_CANCEL, QUIT_OPT_LATER);

    private static final String E_PERMFAIL = "No u! (You don't have the necessary permissions to do this.)";

    private final AccessControl<T> acl;
    private final GameHandler<T> gameHandler;
    private final AdminListener<T> adminListener = new AdminListener<>();

    private String murderer = null;

    public AdminHandler(AccessControl<T> acl, GameHandler<T> gameHandler) {
        this.acl = acl;
        this.gameHandler = gameHandler;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        } else if (commands.size() == 1 && commands.get(0).equals(CMD_QUIT)) {
            return QUIT_OPTS;
        }
        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 0) {
            return;
        }

        String[] args;
        OutputIRC outputIRC = event.getBot().sendIRC();
        String actor = event.getUser().getNick();

        if (remainder != null) {
            remainder = remainder.trim();
        }

        switch (commands.get(0)) {
            case CMD_QUIT:
                String quitMessage = "Tear in salami";
                if (remainder != null && !remainder.isEmpty()) {
                    quitMessage = remainder;
                }
                if (acl.hasPermission(event, PERM_QUIT)) {
                    if (commands.size() == 2) {
                        switch (commands.get(1)) {
                            case QUIT_OPT_FORCE:
                                dedify(quitMessage);
                                break;
                            case QUIT_OPT_LATER:
                                event.respond("Okay, I'll quit when all currently active games have finished, " +
                                    "and prevent new games from starting until then.  If you change your mind and don't want me to quit after all, please use the \"" +
                                    CMD_QUIT + ' ' + QUIT_OPT_CANCEL + "\" command.");
                                event.respond("If you want me to quit immediately, at any time give the \"" +  CMD_QUIT + ' ' + QUIT_OPT_FORCE + "\" command.");
                                murderer = event.getUser().getNick();
                                gameHandler.quitAfterAllGamesFinish(quitMessage, this);
                                break;
                            case QUIT_OPT_CANCEL:
                                gameHandler.cancelQuitAfterAllGamesFinish();
                                murderer = null;
                                event.respond("Okay, cancelled the scheduled quit.  New games may be started once again.");
                                break;
                        }
                    } else {
                        int activeGameChannels = gameHandler.getAllInProgressGamesChannels().size();

                        if (activeGameChannels > 0) {
                            event.respond("Refusing to quit - there are active games in " + activeGameChannels + " channels");
                            event.respond("If you want the bot to quit right now anyway, please use the \"" +
                                QUIT_OPT_FORCE + "\" option.  If you want the bot to quit after all games finish (and prevent new games from starting), use the \"" + QUIT_OPT_LATER + "\" option.");
                        } else {
                            dedify(quitMessage);
                        }
                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_NICK:
                if (acl.hasPermission(event, PERM_NICK)) {
                    if (remainder == null || remainder.isEmpty()) {
                        event.respond("Error: You have to tell me what I should change my nickname to!");
                    } else {
                        outputIRC.changeNick(remainder.split(" ")[0]);
                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_JOIN:
                if (acl.hasPermission(event, PERM_JOIN)) {
                    if (remainder == null || remainder.isEmpty()) {
                        event.respond("Error: You have to tell me what channel you want me to join!");
                    } else {
                        args = remainder.split(" ");

                        if (args.length > 1) {
                            event.respond("Error: Too many arguments for join.");
                        } else {
                            if (!Miscellaneous.isChannelLike(event, args[0])) {
                                event.respond("Error: \"" + args[0] + "\" doesn't seem to be a valid channel name.");
                            } else {
                                outputIRC.joinChannel(args[0]);
                                outputIRC.message(args[0], actor + " asked me to join the channel.");
                            }
                        }
                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_PART:
                if (acl.hasPermission(event, PERM_PART)) {
                    String partMessage = "Requested by " + actor;

                    if (remainder == null || remainder.isEmpty()) {
                        if (event instanceof GenericChannelEvent) {
                            ((GenericChannelEvent) event).getChannel().send().part(partMessage);
                        } else {
                            event.respond("Error: Syntax: part [#channel] {message}");
                        }
                    } else {
                        args = remainder.split(" ");
                        remainder = remainder.substring(args[0].length()).trim();

                        if (!Miscellaneous.isChannelLike(event, args[0])) {
                            event.respond("Error: \"" + args[0] + "\" doesn't seem to be a valid channel name.");
                        } else {
                            if (args.length > 1) {
                                partMessage += " (" + remainder + ")";
                            }

                            // I don't even
                            event.getBot().getUserChannelDao().getChannel(args[0]).send().part(partMessage);
                        }
                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_SAY:
                if (acl.hasPermission(event, PERM_SAY)) {
                    sendToChannel(event, "say", remainder);
                } else {
                    // take that, smartass!
                    if (E_PERMFAIL.equals(remainder) || (actor + ": " + E_PERMFAIL).equals(remainder)) {
                        sendToChannel(event, "act", "throws sand in " + actor + "'s face");
                    }
                    else {
                        event.respond(E_PERMFAIL);
                    }
                }
                break;
            case CMD_ACT:
                if (acl.hasPermission(event, PERM_ACT)) {
                    sendToChannel(event, "act", remainder);
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_RAW:
                if (acl.hasPermission(event, PERM_RAW)) {
                    if (remainder == null || remainder.isEmpty()) {
                        event.respond("Error: Syntax: raw [raw command]");
                    } else {
                        event.getBot().sendRaw().rawLine(remainder);
                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_LIST:
                if (acl.hasPermission(event, PERM_LIST)) {
                    Set<Channel> channels = event.getBot().getUserChannelDao().getAllChannels();
                    event.respond("I am in the following channels: " + Miscellaneous.getStringRepresentation(Miscellaneous.asSortedList(
                        channels.stream()
                            .map(Channel::getName)
                            .collect(Collectors.toSet())
                    )));
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
        }
    }

    private void sendToChannel(GenericMessageEvent<?> event, String what, String remainder) {
        String[] args;
        OutputIRC outputIRC = event.getBot().sendIRC();

        if (!what.equals("say") && !what.equals("act")) {
            throw new IllegalArgumentException("Parameter \"what\" to sendToChannel() must be \"say\" or \"act\"");
        }

        if (remainder == null || remainder.isEmpty()) {
            event.respond("Error: Syntax: " + what + " [message] | " + what + " [#channel] [message]");
        } else {
            args = remainder.split(" ");

            if (event instanceof GenericChannelEvent) {
                if (!Miscellaneous.isChannelLike(event, args[0])) {
                    // say/act command was given in channel without a channel argument (message only)
                    // consider the channel the command was given in to be the target

                    if (what.equals("act")) {
                        ((GenericChannelEvent) event).getChannel().send().action(remainder);
                    } else {
                        ((GenericChannelEvent) event).getChannel().send().message(remainder);
                    }

                    return;
                }
            }

            if (args.length < 2) {
                event.respond("Error: Too few arguments. Syntax: " + what + " [#channel] [message]");
            } else {
                remainder = remainder.substring(args[0].length()).trim();
                if (what.equals("act")) {
                    outputIRC.action(args[0], remainder);
                } else {
                    outputIRC.message(args[0], remainder);
                }
            }
        }
    }

    public void dedify(String quitMessage) {
        if (murderer != null) {
            adminListener.getBot().sendIRC().message(murderer, "Bot is now quitting per your request.");
        }

        adminListener.getBot().stopBotReconnect();
        adminListener.getBot().sendIRC().quitServer(quitMessage);
    }

    public AdminListener<T> getListener() {
        return adminListener;
    }
}
