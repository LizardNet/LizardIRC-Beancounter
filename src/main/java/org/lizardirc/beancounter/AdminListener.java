/**
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class AdminListener<T extends PircBotX> extends CommandListener<T> {
    private static final String CMD_QUIT = "quit";
    private static final String CMD_NICK = "nick";
    private static final String CMD_JOIN = "join";
    private static final String CMD_PART = "part";
    private static final String CMD_SAY = "say";
    private static final String CMD_ACT = "act";
    private static final String CMD_RAW = "raw";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_QUIT, CMD_NICK, CMD_JOIN, CMD_PART, CMD_SAY, CMD_ACT, CMD_RAW);

    private static final String PERM_QUIT = CMD_QUIT;
    private static final String PERM_NICK = CMD_NICK;
    private static final String PERM_JOIN = CMD_JOIN;
    private static final String PERM_PART = CMD_PART;
    private static final String PERM_SAY = CMD_SAY;
    private static final String PERM_ACT = CMD_SAY; // Important! Take note!
    private static final String PERM_RAW = CMD_RAW;

    private static final String E_PERMFAIL = "No u! (You don't have the necessary permissions to do this.)";

    private final AccessControl<T> acl;

    public AdminListener(AccessControl<T> acl) {
        this.acl = acl;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }
        return Collections.<String>emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        String[] args;
        OutputIRC outputIRC = event.getBot().sendIRC();
        String actor = event.getUser().getNick();

        if (commands.size() == 0) {
            return;
        }

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
                    // Forcibly disable auto-reconnect, since we now want the bot to terminate cleanly
                    event.getBot().stopBotReconnect();
                    outputIRC.quitServer(quitMessage);
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
                    event.respond(E_PERMFAIL);
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
}
