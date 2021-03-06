/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.seen;

import com.google.common.collect.ImmutableSet;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.utils.Miscellaneous;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

// ˢᵉʳᶦᵒᵘˢ ᵗʳᵒᵘᵇᶫᵉ
class UserLastSeenCommandHandler<SeriouslyIHaveToDoThisT extends PircBotX> implements CommandHandler<SeriouslyIHaveToDoThisT> {
    private static final String COMMAND_SEEN = "seen";
    private static final String COMMAND_SEEN_CONFIG = "cfgseen";
    private static final Set<String> COMMANDS = ImmutableSet.of(COMMAND_SEEN, COMMAND_SEEN_CONFIG);

    private static final String SEEN_CONFIG_OPERATION_TRACK = "track";
    private static final String SEEN_CONFIG_OPERATION_DO_NOT_TRACK = "notrack";
    private static final String SEEN_CONFIG_OPERATION_LIST_DO_NOT_TRACKS = "list";
    private static final String SEEN_CONFIG_OPERATION_ISTRACKED = "istracked";
    private static final Set<String> SEEN_CONFIG_OPERATIONS = ImmutableSet.of(SEEN_CONFIG_OPERATION_TRACK,
        SEEN_CONFIG_OPERATION_DO_NOT_TRACK, SEEN_CONFIG_OPERATION_LIST_DO_NOT_TRACKS,
        SEEN_CONFIG_OPERATION_ISTRACKED);

    private static final String PERM_SEEN_CONFIG = "cfgseen";

    private final Random random = new Random();
    private final UserLastSeenListener<SeriouslyIHaveToDoThisT> userLastSeenListener;

    public UserLastSeenCommandHandler(UserLastSeenListener<SeriouslyIHaveToDoThisT> userLastSeenListener) {
        this.userLastSeenListener = userLastSeenListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<SeriouslyIHaveToDoThisT> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (commands.size() == 1 && COMMAND_SEEN_CONFIG.equals(commands.get(0))) {
            return SEEN_CONFIG_OPERATIONS;
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GenericMessageEvent<SeriouslyIHaveToDoThisT> event, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        remainder = remainder.trim();
        String[] args = remainder.split(" ");

        switch (commands.get(0)) {
            case COMMAND_SEEN:
                if (remainder.isEmpty()) {
                    event.respond("Error: Too few arguments.  Syntax: " + COMMAND_SEEN + " [nickname]");
                    return;
                }

                if (args[0].equalsIgnoreCase(event.getUser().getNick())) {
                    final String channel;

                    if (event instanceof GenericChannelEvent) {
                        channel = ((GenericChannelEvent) event).getChannel().getName();
                    } else {
                        channel = event.getUser().getNick();
                    }
                    Consumer<String> output = s -> event.getBot().sendIRC().message(channel, s);

                    switch (random.nextInt(10)) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            output.accept("Am seer, " + args[0] + " seen villager");
                            break;
                        case 5:
                        case 6:
                            output.accept("Seer here, " + args[0] + " seen WOLF!");
                            break;
                        case 7:
                            output.accept("Seer here, " + args[0] + " SEEN WOFL!");
                            break;
                        case 8:
                            output.accept("Am seer, " + args[0] + " seen fool");
                            break;
                        case 9:
                            output.accept("Oracle here, " + args[0] + " seen EULA-violating reverse-engineering sinner!");
                            break;
                    }
                    return;
                }

                if (args[0].equalsIgnoreCase(event.getBot().getNick())) {
                    event.respond("Yes, I see myself.  Why do you ask?");
                    return;
                }

                String userHost;
                StringBuilder response;
                ChannelAndTime cat; // cat just stands for ChannelAndTime

                // First, we assume that the argument in the remainder (args[0]) is the nickname to look up
                // If the user is online, get their user@host and look it up in the lastSeen map
                // Otherwise, look for the nick as a key in the lastUsedUserHosts map to get the last user user@host,
                // and use that for the lastSeen map lookup.
                if (event.getBot().getUserChannelDao().userExists(args[0])) {
                    User user = event.getBot().getUserChannelDao().getUser(args[0]);
                    userHost = (user.getLogin() + "@" + user.getHostmask()).toLowerCase();

                    response = new StringBuilder("User ").append(args[0]).append(" is currently \0033online\003; ");
                } else {
                    if (userLastSeenListener.getLastUsedUserHosts().containsKey(args[0].toLowerCase())) {
                        response = new StringBuilder("User ").append(args[0]).append(" is currently \0034offline\003; ");
                        userHost = userLastSeenListener.getLastUsedUserHosts().get(args[0].toLowerCase()).toLowerCase();
                    } else {
                        event.respond("Sorry, I haven't seen the nickname " + args[0] + " talk in a tracked channel.");
                        return;
                    }
                }

                cat = userLastSeenListener.getLastSeen().get(userHost);

                if (cat == null) {
                    response.append("I haven't seen them talk in a channel I'm tracking, though.");
                    event.respond(response.toString());
                    return;
                }

                response.append("I last saw them talk in ").append(cat.getChannel()).append(" at ")
                    .append(cat.getDateTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)).append(" (");

                long seconds = cat.getDateTime().until(ZonedDateTime.now(), ChronoUnit.SECONDS);
                long days = seconds / (24 * 60 * 60);
                seconds %= (24 * 60 * 60);
                long hours = seconds / (60 * 60);
                seconds %= (60 * 60);
                long minutes = seconds / 60;
                seconds %= 60;

                response.append(days).append(" days, ").append(hours).append(" hours, ").append(minutes)
                    .append(" minutes, and ").append(seconds).append(" seconds ago).");

                event.respond(response.toString());
                break;
            case COMMAND_SEEN_CONFIG:
                if (commands.size() != 2) {
                    event.respond("Error: Invalid arguments for " + COMMAND_SEEN_CONFIG + "; usage: " +
                        COMMAND_SEEN_CONFIG + " <" + SEEN_CONFIG_OPERATION_TRACK + "|" +
                        SEEN_CONFIG_OPERATION_DO_NOT_TRACK + "|" + SEEN_CONFIG_OPERATION_LIST_DO_NOT_TRACKS +
                        "|" + SEEN_CONFIG_OPERATION_ISTRACKED + "> [#channel]");
                    return;
                }

                switch (commands.get(1)) {
                    case SEEN_CONFIG_OPERATION_TRACK:
                        if (userLastSeenListener.getAcl().hasPermission(event, PERM_SEEN_CONFIG)) {
                            if (remainder.isEmpty()) {
                                event.respond("Error: Channel argument required for this command.");
                            } else {
                                if (Miscellaneous.isChannelLike(event, args[0])) {
                                    userLastSeenListener.getDoNotTrackChannels().remove(args[0].toLowerCase());
                                    userLastSeenListener.sync();
                                    event.respond("Now tracking channel " + args[0] + " for the " + COMMAND_SEEN + " command.");
                                } else {
                                    event.respond("Error: \"" + args[0] + "\" doesn't seem to be a valid channel name.");
                                }
                            }
                        } else {
                            event.respond("No u!  (You don't have the necessary permissions to do this.)");
                        }
                        break;
                    case SEEN_CONFIG_OPERATION_DO_NOT_TRACK:
                        if (userLastSeenListener.getAcl().hasPermission(event, PERM_SEEN_CONFIG)) {
                            if (remainder.isEmpty()) {
                                event.respond("Error: Channel argument required for this command.");
                            } else {
                                if (Miscellaneous.isChannelLike(event, args[0])) {
                                    userLastSeenListener.getDoNotTrackChannels().add(args[0].toLowerCase());
                                    userLastSeenListener.sync();
                                    event.respond("Adding channel " + args[0] + " to the \"Do Not Track\" list for the " + COMMAND_SEEN + " command.");
                                } else {
                                    event.respond("Error: \"" + args[0] + "\" doesn't seem to be a valid channel name.");
                                }
                            }
                        } else {
                            event.respond("No u!  (You don't have the necessary permissions to do this.)");
                        }
                        break;
                    case SEEN_CONFIG_OPERATION_LIST_DO_NOT_TRACKS:
                        if (userLastSeenListener.getAcl().hasPermission(event, PERM_SEEN_CONFIG)) {
                            event.respond("The following channels are on the \"Do Not Track\" list for the " + COMMAND_SEEN + " command: " +
                                Miscellaneous.getStringRepresentation(userLastSeenListener.getDoNotTrackChannels()));
                        } else {
                            event.respond("No u!  (You don't have the necessary permissions to do this.)");
                        }
                        break;
                    case SEEN_CONFIG_OPERATION_ISTRACKED:
                        if (event instanceof GenericChannelEvent) {
                            Channel thisChannel = ((GenericChannelEvent) event).getChannel();

                            if (userLastSeenListener.getDoNotTrackChannels().contains(thisChannel.getName().toLowerCase())) {
                                event.respond("This channel, " + thisChannel.getName() + ", is *NOT* tracked by the " + COMMAND_SEEN + " command.");
                            } else {
                                if (thisChannel.isSecret()) {
                                    event.respond("This channel, " + thisChannel.getName() + ", *would* be tracked by the " + COMMAND_SEEN + " command; however, it is marked as secret (channel mode +s), so it is *not*.");
                                } else {
                                    event.respond("This channel, " + thisChannel.getName() + ", is tracked by the " + COMMAND_SEEN + " command.");
                                }
                            }
                        } else {
                            event.respond("Sorry, you can only use " + COMMAND_SEEN_CONFIG + " " + SEEN_CONFIG_OPERATION_ISTRACKED +
                                " from within a channel.");
                        }
                }
                break;
        }
    }
}
