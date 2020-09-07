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

package org.lizardirc.beancounter.commands.morse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.lizardirc.beancounter.Beancounter;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MorseListener extends ListenerAdapter {

    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Set<String>>(){}.getType();
    private static final String PERSISTENCE_KEY = "morseEnabled";

    private final PersistenceManager pm;
    private final AccessControl acl;

    private final MorseCommandHandler commandHandler;

    private final Set<String> enabledChannels;
    private MorseDecoder decoder;

    public MorseListener(PersistenceManager pm, AccessControl acl) {
        this.pm = pm;
        this.acl = acl;
        this.commandHandler = new MorseCommandHandler(this);

        Gson gson = new Gson();
        Optional<String> serialisedMorseChannels = pm.get(PERSISTENCE_KEY);
        if (serialisedMorseChannels.isPresent()) {
            Set<String> persistedEnabledChannels = gson.fromJson(serialisedMorseChannels.get(), PERSISTENCE_TYPE_TOKEN);

            //noinspection FuseStreamOperations
            this.enabledChannels = new HashSet<>(persistedEnabledChannels.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList()));
        } else {
            this.enabledChannels = new HashSet<>();
        }

        try (InputStream morseDatafile = Beancounter.class.getResourceAsStream("/morse.json")) {
            InputStreamReader repositoryReader = new InputStreamReader(morseDatafile);

            Type type = new TypeToken<HashMap<String, String>>() {}.getType();

            HashMap<String, String> morseMap = gson.fromJson(repositoryReader, type);
            this.decoder = new MorseDecoder(morseMap);
        } catch (IOException | NullPointerException e) {
            System.err.println("Caught IOException or NullPointerException trying to read in morse mapping: " + e.getMessage());
        }
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    @Override
    public void onGenericChannel(GenericChannelEvent event) {
        if (!(event instanceof GenericMessageEvent)) {
            return;
        }

        if(this.decoder == null) {
            // Error loading morse mapping during initialisation, cannot continue.
            return;
        }

        if (event.getChannel() == null || !enabledChannels.contains(event.getChannel().getName().toLowerCase())) {
            // not enabled in this channel.
            return;
        }

        GenericMessageEvent messageEvent = (GenericMessageEvent) event;

        String message = messageEvent.getMessage().trim();
        User user = messageEvent.getUser();

        MorseDecoder.DecodeResult decodeResult = this.decoder.decodeMorse(message);

        if(decodeResult.detectedCharacters >= 2) {
            String sb = user.getNick()
                    + " meant to say: "
                    + decodeResult.result;
            event.getChannel().send().message(sb);
        }
    }



    boolean getStatus(String channelName) {
        return enabledChannels.contains(channelName);
    }

    synchronized void disable(String channelName) {
        String lowerChannelName = channelName.toLowerCase();

        enabledChannels.remove(lowerChannelName);

        sync();
    }

    synchronized void enable(String channelName) {
        String lowerChannelName = channelName.toLowerCase();

        enabledChannels.add(lowerChannelName);

        sync();
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        pm.set(PERSISTENCE_KEY, gson.toJson(enabledChannels, PERSISTENCE_TYPE_TOKEN));
        pm.sync();
    }

    AccessControl getAccessControl() {
        return acl;
    }
}
