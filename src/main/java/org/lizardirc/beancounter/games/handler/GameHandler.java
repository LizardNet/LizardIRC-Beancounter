/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.games.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.reflections.Reflections;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class GameHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_PLAY = "play";
    private static final Set<String> CMDS_PHASE_INACTIVE = ImmutableSet.of(CMD_PLAY);

    private static final String CMD_JOIN = "join";
    private static final String CMD_LEAVE = "leave";
    private static final String CMD_STATS = "stats";
    private static final String CMD_START = "start";
    private static final Set<String> CMDS_PHASE_SETUP = ImmutableSet.of(CMD_JOIN, CMD_LEAVE, CMD_STATS, CMD_START,
        CMD_PLAY);

    private static final String CMD_CFGGAMES = "cfggames";
    private static final Set<String> CMDS_ALWAYS_AVAILABLE = ImmutableSet.of(CMD_CFGGAMES);

    private static final String CFGGAMES_ENABLE = "enable";
    private static final String CFGGAMES_DISABLE = "disable";
    private static final Set<String> CFGGAMES_OPTS = ImmutableSet.of(CFGGAMES_ENABLE, CFGGAMES_DISABLE);

    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Set<String>>(){}.getType();

    private final AccessControl<T> acl;
    private final PersistenceManager pm;
    private final ScheduledExecutorService ses;

    private final Set<String> gameEnabledChannels;

    private GamePhase gamePhase = GamePhase.INACTIVE;
    private ScheduledFuture scheduledFuture = null;
    private Set<Class<?>> availableGames;

    private Game<T> activeGame = null;
    private Set<User> players = null;

    @SuppressWarnings("unchecked")
    public GameHandler(AccessControl<T> acl, PersistenceManager pm, ScheduledExecutorService ses) {
        this.acl = Objects.requireNonNull(acl);
        this.pm = Objects.requireNonNull(pm);
        this.ses = Objects.requireNonNull(ses);

        Reflections reflections = new Reflections("org.lizardirc.beancounter.games");
        Set<Class<?>> availableGames = reflections.getTypesAnnotatedWith(GameEntrypoint.class);

        Type implType = new TypeToken<Game<T>>(){}.getType();

        Iterator<Class<?>> iter = availableGames.iterator();
        while (iter.hasNext()) {
            Class<?> clazz = iter.next();

            if (!Game.class.isAssignableFrom(clazz)) {
                System.err.println("WARNING: Game class " + clazz.getName() + " does not implement Game<T extends PircBotX> and will be unavailable.");
                iter.remove();
                continue;
            }

            try {
                clazz.getConstructor(this.getClass());
            } catch (NoSuchMethodException e) {
                System.err.println("WARNING: Game class " + clazz.getName() + " does not have an appropriate constructor and will be unavailable.");
                iter.remove();
            }
        }

        this.availableGames = availableGames;

        Optional<String> gameEnabledChannelsSerialized = pm.get("gameEnabledChannels");
        if (gameEnabledChannelsSerialized.isPresent()) {
            Gson gson = new Gson();
            Set<String> gameEnabledChannels = gson.fromJson(gameEnabledChannelsSerialized.get(), PERSISTENCE_TYPE_TOKEN);
            this.gameEnabledChannels = new HashSet<>(
                gameEnabledChannels.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet())
            );
        } else {
            gameEnabledChannels = new HashSet<>();
        }

    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 1 && CMD_CFGGAMES.equals(commands.get(0))) {
            return CFGGAMES_OPTS;
        }

        switch (gamePhase) {
            case INACTIVE:
                if (commands.size() == 0) {
                    Set<String> retval = new HashSet<>();
                    retval.addAll(CMDS_ALWAYS_AVAILABLE);
                    retval.addAll(CMDS_PHASE_INACTIVE);
                    return retval;
                } else if (commands.size() == 1 && CMD_PLAY.equals(commands.get(0))) {
                    return getAvailableGameCommandNames();
                } else {
                    return Collections.emptySet();
                }
            default:
                return CMDS_ALWAYS_AVAILABLE;
        }
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() <= 0) { // We ain't makin' no assumptions!
            return;
        }

        if (CMD_CFGGAMES.equals(commands.get(0))) {
            if (event instanceof GenericChannelEvent) {
                GenericChannelEvent gce = (GenericChannelEvent) event;

                if (commands.size() == 1) {
                    boolean isEnabled = gameEnabledChannels.contains(gce.getChannel().getName().toLowerCase());
                    event.respond("Games are " + (isEnabled ? "" : "not ") + "enabled in this channel.");
                } else if (commands.size() == 2) {
                    if (acl.hasPermission(event, "gamemaster")) {
                        switch (commands.get(1)) {
                            case CFGGAMES_ENABLE:
                                gameEnabledChannels.add(gce.getChannel().getName().toLowerCase());
                                sync();
                                event.respond("Games are now enabled in this channel");
                                break;
                            case CFGGAMES_DISABLE:
                                gameEnabledChannels.remove(gce.getChannel().getName().toLowerCase());
                                sync();
                                event.respond("Games are now disabled in this channel");
                                break;
                        }
                    } else {
                        event.respond("You lack the necessary permissions to enable or disable game play in this channel.");
                    }
                } else {
                    event.respond("Invalid number of arguments.");
                }
            } else {
                event.respond("This command must be run in a channel.");
            }
            return;
        }

        switch (gamePhase) {
            case INACTIVE:
                if (CMD_PLAY.equals(commands.get(0))) {
                    if (event instanceof GenericChannelEvent) {
                        GenericChannelEvent gce = (GenericChannelEvent) event;

                        if (!gameEnabledChannels.contains(gce.getChannel().getName().toLowerCase())) {
                            event.respond("Games are disabled in this channel.");
                            return;
                        }

                        if (commands.size() == 1) {
                            event.respond("The following games are available for play: " +
                                Miscellaneous.getStringRepresentation(Miscellaneous.asSortedList(getAvailableGameCommandNames())));
                        } else if (commands.size() == 2) {
                            Class<?> clazz = availableGames.stream()
                                .filter(clzz -> clzz.getAnnotation(GameEntrypoint.class).getCommandName().equals(commands.get(1)))
                                .findFirst()
                                .orElse(null);

                            if (clazz == null) {
                                event.respond("Failed to find game class");
                            } else {
                                if (clazz.getAnnotation(GameEntrypoint.class).requiresChanops() && !gce.getChannel().isOp(event.getBot().getUserBot())) {
                                    event.respond("This game cannot be played in this channel: Bot must be opped.");
                                    return;
                                }

                                try {
                                    activeGame = (Game<T>) clazz.getConstructor(this.getClass()).newInstance(this);
                                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                                    event.respond("Failed to instantiate game class: " + e.getMessage());
                                    return;
                                }

                                String gameName = activeGame.getProperName();
                                gamePhase = GamePhase.SETUP;

                                sendChannelMessage(gce, event.getUser().getNick() + " has started a game of \002" +
                                    gameName + "\002!");

                                sendChannelMessage(gce, "To join, use the \"" + CMD_JOIN + "\" command.  Once players have joined, use the \"" +
                                    CMD_START + "\" command to start.  If the game isn't started in 20 minutes, or if all players leave, the " +
                                    "game will be stopped.");
                            }
                        } else {
                            event.respond("Too many arguments.  Syntax: " + CMD_PLAY + " {gameName}");
                        }
                    } else {
                        event.respond("This command must be run in a channel.");
                    }
                }
                break;
        }
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void sendChannelMessage(GenericChannelEvent<?> gce, String message) {
        gce.getBot().sendIRC().message(gce.getChannel().getName(), message);
    }

    private Set<String> getAvailableGameCommandNames() {
        return availableGames.stream()
            .map(clazz -> clazz.getAnnotation(GameEntrypoint.class))
            .map(GameEntrypoint::getCommandName)
            .collect(Collectors.toSet());
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        String gameEnabledChannelsSerialized = gson.toJson(gameEnabledChannels, PERSISTENCE_TYPE_TOKEN);
        pm.set("gameEnabledChannels", gameEnabledChannelsSerialized);
        pm.sync();
    }
}
