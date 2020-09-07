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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;

public class UserLastSeenListener extends ListenerAdapter {
    private final PersistenceManager pm;
    private final AccessControl acl;

    // This is a mapping of user@hosts to an object that contains the channel they last spoke in, and the time
    private final Map<String, ChannelAndTime> lastSeen;
    // This will be a mapping of nicknames to the user@host they were last seen using
    private final Map<String, String> lastUsedUserHosts;
    // This will be a set of channels that are forced as "do not track" by an authorized user using the COMMAND_SEEN_CONFIG
    // command.  The bot will also automatically not track channels that are set as secret (mode +s usually).
    private final Set<String> doNotTrackChannels;

    private final CommandHandler commandHandler = new UserLastSeenCommandHandler(this);

    public UserLastSeenListener(PersistenceManager pm, AccessControl acl) {
        this.pm = pm;
        this.acl = acl;

        //noinspection FuseStreamOperations
        doNotTrackChannels = new HashSet<>(pm.getSet("doNotTrackChannels").stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet())
        );
        lastUsedUserHosts = new HashMap<>(pm.getMap("lastUsedUserHosts").entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e->e.getValue().toLowerCase(), (o, n) -> n)) // In case of collision, arbitrarily discard old value
        );
        lastSeen = getLastSeenMap();
    }

    public synchronized void onMessage(MessageEvent event) {
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

    synchronized void sync() {
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

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    AccessControl getAcl() {
        return acl;
    }

    Map<String, String> getLastUsedUserHosts() {
        return lastUsedUserHosts;
    }

    Map<String, ChannelAndTime> getLastSeen() {
        return lastSeen;
    }

    Set<String> getDoNotTrackChannels() {
        return doNotTrackChannels;
    }
}
