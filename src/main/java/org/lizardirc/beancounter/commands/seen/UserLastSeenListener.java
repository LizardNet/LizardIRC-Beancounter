/**
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Bases;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class UserLastSeenListener<T extends PircBotX> extends ListenerAdapter<T> {
    private final PersistenceManager pm;
    private final AccessControl<T> acl;

    // This is a mapping of user@hosts to an object that contains the channel they last spoke in, and the time
    private final Map<String, ChannelAndTime> lastSeen;
    // This will be a mapping of nicknames to the user@host they were last seen using
    private final Map<String, String> lastUsedUserHosts;
    // This will be a set of channels that are forced as "do not track" by an authorized user using the COMMAND_SEEN_CONFIG
    // command.  The bot will also automatically not track channels that are set as secret (mode +s usually).
    private final Set<String> doNotTrackChannels;

    private final CommandHandler<T> commandHandler = new UserLastSeenCommandHandler<>();

    public UserLastSeenListener(PersistenceManager pm, AccessControl<T> acl) {
        this.pm = pm;
        this.acl = acl;

        doNotTrackChannels = new HashSet<>(pm.getSet("doNotTrackChannels").stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet())
        );
        lastUsedUserHosts = new HashMap<>(pm.getMap("lastUsedUserHosts").entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e->e.getValue().toLowerCase(), (o, n) -> n)) // In case of collision, arbitrarily discard old value
        );
        lastSeen = getLastSeenMap();
    }

    public synchronized void onMessage(MessageEvent<T> event) {
        if (doNotTrackChannels.contains(event.getChannel().getName().toLowerCase()) || event.getChannel().isSecret()) {
            return;
        }

        String userHost = (event.getUser().getLogin() + "@" + event.getUser().getHostmask()).toLowerCase();

        if (lastSeen.containsKey(userHost)) {
            ChannelAndTime cat = lastSeen.get(userHost); // cat just stands for ChannelAndTime
            cat.setChannel(event.getChannel().getName());
            cat.setDateTime(ZonedDateTime.now());
        } else {
            lastSeen.put(userHost, new ChannelAndTime(event.getChannel().getName(), ZonedDateTime.now()));
        }

        lastUsedUserHosts.put(event.getUser().getNick().toLowerCase(), userHost);
        sync();
    }

    private synchronized void sync() {
        pm.setSet("doNotTrackChannels", doNotTrackChannels);
        pm.setMap("lastUsedUserHosts", lastUsedUserHosts);
        putLastSeenMap();
        pm.sync();
    }

    private synchronized void putLastSeenMap() {
        pm.setMap("lastSeen", lastSeen.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().toString()))
        );
    }

    private synchronized Map<String, ChannelAndTime> getLastSeenMap() {
        return new HashMap<>(pm.getMap("lastSeen").entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e -> ChannelAndTime.fromString(e.getValue()), (o, n) -> n)) // In case of collision, arbitrarily discard old value
        );
    }

    public CommandHandler<T> getCommandHandler() {
        return commandHandler;
    }

    private static final class ChannelAndTime {
        private String channel;
        private ZonedDateTime dateTime;

        public ChannelAndTime(String channel, ZonedDateTime dateTime) {
            this.channel = channel;
            this.dateTime = dateTime;
        }

        public String getChannel() {
            return channel;
        }

        public ZonedDateTime getDateTime() {
            return dateTime;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public void setDateTime(ZonedDateTime dateTime) {
            this.dateTime = dateTime;
        }

        @Override
        public String toString() {
            return Bases.base64encode(channel) + ':' + dateTime.toEpochSecond();
        }

        public static ChannelAndTime fromString(String stringRepresentation) {
            String[] s = stringRepresentation.split(":");
            return new ChannelAndTime(Bases.base64decode(s[0]),
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s[1])), ZoneId.systemDefault()));
        }
    }

    // ˢᵉʳᶦᵒᵘˢ ᵗʳᵒᵘᵇᶫᵉ
    private class UserLastSeenCommandHandler<SeriouslyIHaveToDoThisT extends PircBotX> implements CommandHandler<SeriouslyIHaveToDoThisT> {
        private static final String COMMAND_SEEN = "seen";
        private static final String COMMAND_SEEN_CONFIG = "cfgseen";
        private final Set<String> COMMANDS = ImmutableSet.of(COMMAND_SEEN, COMMAND_SEEN_CONFIG);

        private static final String SEEN_CONFIG_OPERATION_TRACK = "track";
        private static final String SEEN_CONFIG_OPERATION_DO_NOT_TRACK = "notrack";
        private static final String SEEN_CONFIG_OPERATION_LIST_DO_NOT_TRACKS = "list";
        private static final String SEEN_CONFIG_OPERATION_ISTRACKED = "istracked";
        private final Set<String> SEEN_CONFIG_OPERATIONS = ImmutableSet.of(SEEN_CONFIG_OPERATION_TRACK,
            SEEN_CONFIG_OPERATION_DO_NOT_TRACK, SEEN_CONFIG_OPERATION_LIST_DO_NOT_TRACKS,
            SEEN_CONFIG_OPERATION_ISTRACKED);

        private static final String PERM_SEEN_CONFIG = "cfgseen";

        private final Random random = new Random();

        @Override
        public Set<String> getSubCommands(GenericMessageEvent<SeriouslyIHaveToDoThisT> event, List<String> commands) {
            if (commands.size() == 0) {
                return COMMANDS;
            }

            if (commands.size() == 1 && COMMAND_SEEN_CONFIG.equals(commands.get(0))) {
                return SEEN_CONFIG_OPERATIONS;
            }

            return Collections.<String>emptySet();
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
                        if (lastUsedUserHosts.containsKey(args[0].toLowerCase())) {
                            response = new StringBuilder("User ").append(args[0]).append(" is currently \0034offline\003; ");
                            userHost = lastUsedUserHosts.get(args[0].toLowerCase()).toLowerCase();
                        } else {
                            event.respond("Sorry, I haven't seen the nickname " + args[0] + " talk in a tracked channel.");
                            return;
                        }
                    }

                    cat = lastSeen.get(userHost);

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
                            if (acl.hasPermission(event, PERM_SEEN_CONFIG)) {
                                if (remainder.isEmpty()) {
                                    event.respond("Error: Channel argument required for this command.");
                                } else {
                                    if (Miscellaneous.isChannelLike(event, args[0])) {
                                        doNotTrackChannels.remove(args[0].toLowerCase());
                                        sync();
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
                            if (acl.hasPermission(event, PERM_SEEN_CONFIG)) {
                                if (remainder.isEmpty()) {
                                    event.respond("Error: Channel argument required for this command.");
                                } else {
                                    if (Miscellaneous.isChannelLike(event, args[0])) {
                                        doNotTrackChannels.add(args[0].toLowerCase());
                                        sync();
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
                            if (acl.hasPermission(event, PERM_SEEN_CONFIG)) {
                                event.respond("The following channels are on the \"Do Not Track\" list for the " + COMMAND_SEEN + " command: " +
                                    Miscellaneous.getStringRepresentation(doNotTrackChannels));
                            } else {
                                event.respond("No u!  (You don't have the necessary permissions to do this.)");
                            }
                            break;
                        case SEEN_CONFIG_OPERATION_ISTRACKED:
                            if (event instanceof GenericChannelEvent) {
                                Channel thisChannel = ((GenericChannelEvent) event).getChannel();

                                if (doNotTrackChannels.contains(thisChannel.getName().toLowerCase())) {
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
}
