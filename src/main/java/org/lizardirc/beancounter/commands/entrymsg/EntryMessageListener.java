/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016-2020 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.entrymsg;

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;

public class EntryMessageListener extends ListenerAdapter {
    /** This must match the type of the {@link #entryMessages} field */
    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Map<String, EntryMessage>>(){}.getType();

    private static final String MESSAGE_OUTPUT_FORMAT = "%s (Set by %s (%s) at %s)";

    private final PersistenceManager pm;
    private final AccessControl acl;
    private final Map<String, EntryMessage> entryMessages;
    private final EntryMessageCommandHandler commandHandler;

    public EntryMessageListener(PersistenceManager pm, AccessControl acl) {
        this.pm = pm;
        this.acl = acl;

        // Depersist state
        Gson gson = new Gson();
        Optional<String> serializedEntryMessages = pm.get("entryMessages");
        if (serializedEntryMessages.isPresent()) {
            Map<String, EntryMessage> entryMessages = gson.fromJson(serializedEntryMessages.get(), PERSISTENCE_TYPE_TOKEN);

            // Force all key strings to be lowercase, and arbitrarily discard the "old" item in case of collision
            this.entryMessages = new HashMap<>(entryMessages.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue, (o, n) -> n)));
        } else {
            entryMessages = new HashMap<>();
        }

        this.commandHandler = new EntryMessageCommandHandler(this);
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        pm.set("entryMessages", gson.toJson(entryMessages, PERSISTENCE_TYPE_TOKEN));
        pm.sync();
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    AccessControl getAccessControl() {
        return acl;
    }

    void setEntryMessage(String channel, EntryMessage entryMessage) {
        entryMessages.put(channel.toLowerCase(), entryMessage);
        sync();
    }

    EntryMessage getEntryMessage(String channel) {
        return entryMessages.get(channel.toLowerCase());
    }

    void clearEntryMessage(String channel) {
        entryMessages.remove(channel.toLowerCase());
        sync();
    }

    String generateMessageString(GenericChannelEvent event) {
        EntryMessage message = getEntryMessage(event.getChannel().getName());
        if (message != null) {
            String[] settingUser = message.getSettingUser().split("!");
            return String.format(MESSAGE_OUTPUT_FORMAT, message.getMessage(), settingUser[0], settingUser[1],
                message.getTimestamp().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        } else {
            return null;
        }
    }

    @Override
    public void onJoin(JoinEvent event) {
        if (event.getBot().getUserBot().equals(event.getUser())) {
            return;
        }

        String response = generateMessageString(event);

        if (response != null) {
            event.respond(generateMessageString(event));
        }
    }
}
