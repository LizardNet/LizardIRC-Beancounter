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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;
import redis.clients.jedis.Jedis;

import org.lizardirc.beancounter.hooks.Chainable;
import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.hooks.Fantasy;
import org.lizardirc.beancounter.hooks.MultiCommandListener;
import org.lizardirc.beancounter.hooks.PerChannel;
import org.lizardirc.beancounter.hooks.PerChannelCommand;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.persistence.PropertiesPersistenceManager;
import org.lizardirc.beancounter.persistence.RedisPersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.security.BreadBasedAccessControl;

public class Listeners<T extends PircBotX> extends CommandListener<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of("rehash");
    private final ListenerManager<T> listenerManager;
    private final Properties properties;
    private final Set<Listener<T>> ownListeners = new HashSet<>();

    private AccessControl<T> acl;
    private UserLastSeenListener<T> userLastSeenListener;

    public Listeners(ListenerManager<T> listenerManager, Properties properties) {
        this.listenerManager = listenerManager;
        this.properties = properties;
    }

    public void register() {
        String fantasyString = properties.getProperty("fantasyString", "?");
        String separator = properties.getProperty("separator", ";");
        String modesOnConnect = properties.getProperty("autoModes", "");
        String beanledgerBackend = properties.getProperty("beanledger.backend", "flatfile");
        String ownerHostmask = properties.getProperty("ownerHostmask", "");
        PersistenceManager pm;

        switch (beanledgerBackend) {
            case "flatfile":
                Path persistencePath = Paths.get(properties.getProperty("beanledger.flatfile.path", "beanledger.props"));
                pm = new PropertiesPersistenceManager(persistencePath);
                break;
            case "redis":
                String redisHost = properties.getProperty("beanledger.redis.host", "localhost");
                String redisPort = properties.getProperty("beanledger.redis.port");
                String redisNamespace = properties.getProperty("beanledger.redis.namespace");
                Jedis jedis;
                if (redisPort != null) {
                    jedis = new Jedis(redisHost, Integer.parseInt(redisPort));
                } else {
                    jedis = new Jedis(redisHost);
                }
                pm = new RedisPersistenceManager(jedis);
                if (redisNamespace != null) {
                    pm = pm.getNamespace(redisNamespace);
                }
                break;
            default:
                throw new IllegalStateException("Unknown or unsupported Beanledger backend \"" + beanledgerBackend + "\" specified in configuration.");
        }

        acl = new BreadBasedAccessControl<>(ownerHostmask, pm.getNamespace("breadBasedAccessControl"));
        userLastSeenListener = new UserLastSeenListener<>(pm.getNamespace("userLastSeenConfig"), acl);

        List<CommandListener<T>> listeners = new ArrayList<>();
        listeners.add(new AdminListener<>(acl));
        listeners.add(new DiceListener<>());
        listeners.add(new SlapListener<>(pm.getNamespace("customSlaps"), acl));
        listeners.add(new PerChannelCommand<>(RouletteListener::new));
        listeners.add(acl.getListener());
        listeners.add(userLastSeenListener.getCommandListener());
        listeners.add(new HelpListener<T>());
        listeners.add(this);
        MultiCommandListener<T> commands = new MultiCommandListener<>(listeners);
        ownListeners.add(new Chainable<>(new Fantasy<>(commands, fantasyString), separator));

        ownListeners.add(new ChannelPersistor<>(pm.getNamespace("channelPersistence")));

        if (!modesOnConnect.isEmpty()) {
            ownListeners.add(new SetModesOnConnectListener<>(modesOnConnect));
        }

        ownListeners.add(new PerChannel<>(() -> new SedListener<>(5)));
        ownListeners.add(new InviteAcceptor<>());
        ownListeners.add(userLastSeenListener);

        ownListeners.forEach(listenerManager::addListener);
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
        if (commands.size() == 0) {
            return;
        }

        if (!acl.hasPermission(event, "rehash")) {
            event.respond("No u! (You don't have permission to do this.)");
            return;
        }

        ownListeners.forEach(listenerManager::removeListener);
        ownListeners.clear();
        register();
        event.respond("Reloaded listeners");
    }
}
