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

package org.lizardirc.beancounter.commands.fishbot;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;

public class FishbotListener extends ListenerAdapter {

    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Set<String>>(){}.getType();
    private static final String PERSISTENCE_KEY = "fishbotEnabled";

    private final FishbotResponseRepository responseRepository;
    private final PersistenceManager pm;
    private final AccessControl acl;

    private final FishbotCommandHandler commandHandler;

    private final Set<String> enabledChannels;

    private String currentNickname = null;

    public FishbotListener(FishbotResponseRepository responseRepository, PersistenceManager pm, AccessControl acl) {
        this.responseRepository = responseRepository;
        this.pm = pm;
        this.acl = acl;
        this.commandHandler = new FishbotCommandHandler(this);

        Gson gson = new Gson();
        Optional<String> serialisedFishbotChannels = pm.get(PERSISTENCE_KEY);
        if (serialisedFishbotChannels.isPresent()) {
            Set<String> persistedEnabledChannels = gson.fromJson(serialisedFishbotChannels.get(), PERSISTENCE_TYPE_TOKEN);

            //noinspection FuseStreamOperations
            enabledChannels = new HashSet<>(persistedEnabledChannels.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList()));
        } else {
            enabledChannels = new HashSet<>();
        }
    }

    @Override
    public void onConnect(ConnectEvent event) {
        recompile(event.getBot().getNick());
    }

    @Override
    public void onNickChange(NickChangeEvent event) {
        recompile(event.getBot().getNick());
    }

    @Override
    public void onGenericChannel(GenericChannelEvent event) {
        if (!(event instanceof GenericMessageEvent)) {
            return;
        }

        if (currentNickname == null) {
            // we came too early ( sorry! :P )
            return;
        }

        if (event.getChannel() == null || !enabledChannels.contains(event.getChannel().getName().toLowerCase())) {
            // not enabled in this channel.
            return;
        }

        GenericMessageEvent messageEvent = (GenericMessageEvent) event;

        String message = messageEvent.getMessage().trim();
        User user = messageEvent.getUser();
        Channel channel = event.getChannel();

        if (event instanceof ActionEvent) {
            handleResponse(event, message, responseRepository.getActions(), user, channel);
        }

        if (event instanceof MessageEvent) {
            handleResponse(event, message, responseRepository.getMessages(), user, channel);
        }
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        pm.set(PERSISTENCE_KEY, gson.toJson(enabledChannels, PERSISTENCE_TYPE_TOKEN));
        pm.sync();
    }

    private void recompile(String nick) {
        if (nick.equals(currentNickname)) {
            // nothing to do
            return;
        }

        responseRepository.recompile(nick);
        currentNickname = nick;
    }

    private void handleResponse(GenericChannelEvent event, String message, List<FishbotResponse> responses, User user, Channel channel) {
        for (FishbotResponse e : responses) {
            Matcher matcher = e.getMatcher(message);

            if (matcher != null && matcher.find()) {
                String response = e.getResponse();

                response = response.replace("%n", user.getNick());
                response = response.replace("%c", channel.getName());

                if (matcher.groupCount() >= 1) {
                    response = response.replace("%1", matcher.group(1));
                }

                // send to channel
                event.getChannel().send().message(response);
                return;
            }
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

    AccessControl getAccessControl() {
        return acl;
    }
}
