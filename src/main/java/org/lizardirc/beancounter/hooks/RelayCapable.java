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

package org.lizardirc.beancounter.hooks;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Bases;
import org.lizardirc.beancounter.views.MessageEventView;
import org.lizardirc.beancounter.views.UserView;

public class RelayCapable<T extends PircBotX> extends Decorator<T> {
    private static final String CMD_RELAYS = "relays";
    private static final String CMD_ADD = "add";
    private static final String CMD_DEL = "delete";
    private static final String CMD_LIST = "list";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_RELAYS);
    private static final Set<String> COMMANDS_RELAYS = ImmutableSet.of(CMD_ADD, CMD_DEL, CMD_LIST);
    private static final Set<String> EMPTY_SET = ImmutableSet.of();

    private static final String PRIV_RELAYS = "relays";

    private final PersistenceManager persistenceManager;
    private final AccessControl<T> acl;
    private final Map<String, Relay<T>> relays;

    public RelayCapable(Listener<T> childListener, PersistenceManager persistenceManager, AccessControl<T> acl) {
        super(childListener);
        this.persistenceManager = persistenceManager;
        this.acl = acl;
        relays = persistenceManager.getMap("relays").entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Relay.fromString(e.getValue())));
    }

    @Override
    public void onEvent(Event<T> event) throws Exception {
        if (event instanceof MessageEvent) {
            MessageEvent<T> me = (MessageEvent<T>) event;
            for (Relay<T> relay : relays.values()) {
                MessageEvent<T> transformed = relay.transform(me);
                if (transformed != null) {
                    super.onEvent(transformed);
                    return;
                }
            }
        }
        super.onEvent(event);
    }

    public CommandHandler<T> getCommandHandler() {
        return commandHandler;
    }

    private void sync() {
        persistenceManager.setMap("relays", relays.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().toString()
            )));
        persistenceManager.sync();
    }

    private final CommandHandler<T> commandHandler = new CommandHandler<T>() {
        @Override
        public Set<String> getSubCommands(final GenericMessageEvent<T> event, final List<String> commands) {
            switch (commands.size()) {
                case 0:
                    return COMMANDS;
                case 1:
                    return COMMANDS_RELAYS;
                default:
                    return EMPTY_SET;
            }
        }

        @Override
        public void handleCommand(final GenericMessageEvent<T> event, final List<String> commands, final String remainder) {
            if (commands.isEmpty()) {
                return;
            }

            if (!acl.hasPermission(event, PRIV_RELAYS)) {
                event.respond("I'm afraid I can't do that, Dave");
                return;
            }

            if (commands.size() == 1) {
                event.respond("Usage: relays [add | delete | list]");
                return;
            }

            String relayName;
            switch (commands.get(1)) {
                case CMD_ADD:
                    String[] split = remainder.trim().split(" ");
                    if (split.length != 4) {
                        event.respond("Usage: relays add NAME HOSTMASK NAMEREGEX MESSAGEREGEX");
                        break;
                    }

                    relayName = split[0];
                    if (relays.containsKey(relayName)) {
                        event.respond("Relay " + relayName + " already exists");
                        break;
                    }
                    String hostmask = split[1];
                    String nameRegex = split[2];
                    String messageRegex = split[3];
                    relays.put(relayName, new Relay<T>(hostmask, nameRegex, messageRegex));
                    sync();
                    event.respond("Added relay " + relayName);
                    break;
                case CMD_DEL:
                    relayName = remainder.trim();
                    if (relayName.isEmpty()) {
                        event.respond("Usage: relays delete NAME");
                        break;
                    }

                    if (relays.containsKey(relayName)) {
                        relays.remove(relayName);
                        sync();
                        event.respond("Removed relay " + relayName);
                    } else {
                        event.respond("Relay " + relayName + " not found");
                    }
                    break;
                case CMD_LIST:
                    event.respond("I know the following relays: " + relays.keySet().stream().collect(Collectors.joining(", ")));
            }
        }
    };

    private static class Relay<T extends PircBotX> {
        private final String relayRegex;
        private final String nameRegex;
        private final String messageRegex;
        private final Pattern relay;
        private final Pattern name;
        private final Pattern message;

        public Relay(String relayRegex, String nameRegex, String messageRegex) {
            this.relayRegex = relayRegex;
            this.nameRegex = nameRegex;
            this.messageRegex = messageRegex;
            this.relay = Pattern.compile(relayRegex);
            this.name = Pattern.compile(nameRegex);
            this.message = Pattern.compile(messageRegex);
        }

        public String toString() {
            return Bases.base64encode(relayRegex) + ',' + Bases.base64encode(nameRegex) + ',' + Bases.base64encode(messageRegex);
        }

        public static <T extends PircBotX> Relay<T> fromString(String encoded) {
            String[] split = encoded.split(",");
            return new Relay<>(Bases.base64decode(split[0]), Bases.base64decode(split[1]), Bases.base64decode(split[2]));
        }

        public MessageEvent<T> transform(MessageEvent<T> me) {
            User relayUser = me.getUser();
            String hostmask = relayUser.getNick() + "!" + relayUser.getLogin() + "@" + relayUser.getHostmask();
            if (relay.matcher(hostmask).matches()) {
                String str = me.getMessage();
                Matcher nameMatcher = name.matcher(str);
                Matcher messageMatcher = message.matcher(str);
                if (!nameMatcher.matches()) {
                    System.err.println("Warning: message from relay, but unable to match name");
                    return null;
                }
                if (!messageMatcher.matches()) {
                    System.err.println("Warning: message from relay, but unable to match content");
                    return null;
                }
                User fakeUser = transformUser(relayUser, nameMatcher.group(1));
                return new MessageEventView<>(me, messageMatcher.group(1), fakeUser);
            }
            return null;
        }

        private User transformUser(User user, String newName) {
            return new UserView(user, user.getNick() + ":" + newName, user.getLogin() + ":" + newName, user.getHostmask());
        }
    }
}