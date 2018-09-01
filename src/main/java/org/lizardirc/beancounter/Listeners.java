/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2020 by the LizardIRC Development Team. Some rights reserved.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;
import redis.clients.jedis.Jedis;

import org.lizardirc.beancounter.commands.admin.AdminHandler;
import org.lizardirc.beancounter.commands.dice.DiceHandler;
import org.lizardirc.beancounter.commands.earthquake.EarthquakeListener;
import org.lizardirc.beancounter.commands.entrymsg.EntryMessageListener;
import org.lizardirc.beancounter.commands.fishbot.FishbotListener;
import org.lizardirc.beancounter.commands.fishbot.FishbotResponseRepository;
import org.lizardirc.beancounter.commands.goat.GoatHandler;
import org.lizardirc.beancounter.commands.help.HelpHandler;
import org.lizardirc.beancounter.commands.memes.MemeHandler;
import org.lizardirc.beancounter.commands.morse.MorseListener;
import org.lizardirc.beancounter.commands.reddit.RedditHandler;
import org.lizardirc.beancounter.commands.reddit.RedditService;
import org.lizardirc.beancounter.commands.remind.ReminderListener;
import org.lizardirc.beancounter.commands.roulette.RouletteHandler;
import org.lizardirc.beancounter.commands.sed.SedListener;
import org.lizardirc.beancounter.commands.seen.UserLastSeenListener;
import org.lizardirc.beancounter.commands.shakespeare.ShakespeareHandler;
import org.lizardirc.beancounter.commands.slap.SlapHandler;
import org.lizardirc.beancounter.commands.weather.WeatherHandler;
import org.lizardirc.beancounter.commands.wikipedia.WikipediaHandler;
import org.lizardirc.beancounter.commands.youtube.YouTubeHandler;
import org.lizardirc.beancounter.commands.youtube.YouTubeService;
import org.lizardirc.beancounter.gameframework.GameHandler;
import org.lizardirc.beancounter.hooks.Chainable;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.hooks.Fantasy;
import org.lizardirc.beancounter.hooks.MultiCommandHandler;
import org.lizardirc.beancounter.hooks.PerChannel;
import org.lizardirc.beancounter.hooks.PerChannelCommand;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.persistence.PropertiesPersistenceManager;
import org.lizardirc.beancounter.persistence.RedisPersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.security.BreadBasedAccessControl;

public class Listeners implements CommandHandler {
    private static final Set<String> COMMANDS = ImmutableSet.of("rehash");

    private final Set<Listener> ownListeners = new HashSet<>();

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ListenerManager listenerManager;
    private final Properties properties;

    private AccessControl acl;

    public Listeners(ExecutorService executorService, ScheduledExecutorService scheduledExecutorService, ListenerManager listenerManager, Properties properties) {
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
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

        boolean enableWeatherHandler = Boolean.parseBoolean(properties.getProperty("weather.enable", "false"));

        RedditService redditService = new RedditService();
        YouTubeService youTubeService = new YouTubeService(pm.getNamespace("youtube"));

        // Late 2020: Well, COVID-19 is out in the world, but at least we were able to vaccinate this code against a
        // type parameter infection.

        acl = new BreadBasedAccessControl(ownerHostmask, pm.getNamespace("breadBasedAccessControl"));
        UserLastSeenListener userLastSeenListener = new UserLastSeenListener(pm.getNamespace("userLastSeenConfig"), acl);
        InviteAcceptor inviteAcceptor = new InviteAcceptor(pm.getNamespace("inviteAcceptor"), acl);
        ReminderListener reminderListener = new ReminderListener(pm.getNamespace("reminderHandler"), acl, scheduledExecutorService);
        EarthquakeListener earthquakeListener = new EarthquakeListener(pm.getNamespace("earthquakeListener"), acl, scheduledExecutorService);
        EntryMessageListener entryMessageListener = new EntryMessageListener(pm.getNamespace("entryMessage"), acl);
        FishbotListener fishbotHandler = new FishbotListener(FishbotResponseRepository.initialise(), pm.getNamespace("fishbot"), acl);
        WikipediaHandler wikipediaHandler = new WikipediaHandler(pm.getNamespace("wikipediaHandler"), acl);
        MorseListener morseHandler = new MorseListener(pm.getNamespace("morse"), acl);
        GameHandler gameHandler = new GameHandler(acl, pm.getNamespace("gameHandler"), scheduledExecutorService);

        List<CommandHandler> handlers = new ArrayList<>();
        handlers.add(new AdminHandler(acl));
        handlers.add(new DiceHandler());
        handlers.add(new MemeHandler());
        handlers.add(new GoatHandler(acl));
        handlers.add(new SlapHandler(pm.getNamespace("customSlaps"), acl));
        handlers.add(new PerChannelCommand(RouletteHandler::new));
        handlers.add(wikipediaHandler);
        handlers.add(new YouTubeHandler(acl, youTubeService));
        handlers.add(new RedditHandler(redditService));
        handlers.add(acl.getHandler());
        handlers.add(new ShakespeareHandler());
        handlers.add(userLastSeenListener.getCommandHandler());
        if (enableWeatherHandler) {
            handlers.add(new WeatherHandler(pm.getNamespace("weatherHandler"), acl));
        }
        handlers.add(inviteAcceptor.getCommandHandler());
        handlers.add(reminderListener.getCommandHandler());
        handlers.add(earthquakeListener.getCommandHandler());
        handlers.add(entryMessageListener.getCommandHandler());
        handlers.add(fishbotHandler.getCommandHandler());
        handlers.add(morseHandler.getCommandHandler());
        handlers.add(gameHandler);
        handlers.add(this);
        MultiCommandHandler commands = new MultiCommandHandler(handlers);
        commands.add(new HelpHandler(commands));
        ownListeners.add(new Chainable(new Fantasy(new CommandListener(commands), fantasyString), separator));

        ownListeners.add(new ChannelPersistor(pm.getNamespace("channelPersistence")));

        if (!modesOnConnect.isEmpty()) {
            ownListeners.add(new SetModesOnConnectListener(modesOnConnect));
        }

        ownListeners.add(new PerChannel(() -> new SedListener(executorService, 5)));
        ownListeners.add(inviteAcceptor);
        ownListeners.add(userLastSeenListener);
        ownListeners.add(reminderListener);
        ownListeners.add(earthquakeListener);
        ownListeners.add(entryMessageListener);
        ownListeners.add(fishbotHandler);
        ownListeners.add(wikipediaHandler);
        ownListeners.add(morseHandler);
        ownListeners.add(gameHandler.getListener());

        ownListeners.forEach(listenerManager::addListener);
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }
        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent event, List<String> commands, String remainder) {
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
