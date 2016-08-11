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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.pircbotx.Channel;
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
    private static final String CMD_JOIN = "JoinGame";
    private static final String CMD_LEAVE = "leave";
    private static final String CMD_PLAYERS = "players";
    private static final String CMD_START = "start";
    private static final String CMD_HALT = "halt";
    private static final String CMD_GAMES = "games";
    private static final String CMD_CFGGAMES = "cfggames";
    private static final String CFGGAMES_ENABLE = "enable";
    private static final String CFGGAMES_DISABLE = "disable";

    private static final Set<String> CMDS_ALWAYS_AVAILABLE = ImmutableSet.of(CMD_CFGGAMES, CMD_HALT);
    private static final Set<String> CMDS_PHASE_INACTIVE = ImmutableSet.of(CMD_PLAY, CMD_GAMES);
    private static final Set<String> CMDS_PHASE_SETUP = ImmutableSet.of(CMD_JOIN, CMD_LEAVE, CMD_PLAYERS, CMD_START,
        CMD_PLAY);
    private static final Set<String> CMDS_PHASE_ACTIVE = ImmutableSet.of(CMD_PLAYERS, CMD_LEAVE);
    private static final Set<String> CFGGAMES_OPTS = ImmutableSet.of(CFGGAMES_ENABLE, CFGGAMES_DISABLE);
    
    private static final String PERM_GAMEMASTER = "gamemaster";

    private static final Type PERSISTENCE_TYPE_TOKEN = new TypeToken<Set<String>>(){}.getType();

    private final AccessControl<T> acl;
    private final PersistenceManager pm;
    private final ScheduledExecutorService ses;
    private final GameListener<T> listener;

    private final Set<String> gameEnabledChannels;
    private final Map<Channel, ChannelState<T>> perChannelState = new HashMap<>();
    private Set<Class<?>> availableGames;

    public GameHandler(AccessControl<T> acl, PersistenceManager pm, ScheduledExecutorService ses) {
        this.acl = Objects.requireNonNull(acl);
        this.pm = Objects.requireNonNull(pm);
        this.ses = Objects.requireNonNull(ses);

        Reflections reflections = new Reflections("org.lizardirc.beancounter.games");
        Set<Class<?>> availableGames = reflections.getTypesAnnotatedWith(GameEntrypoint.class);

        Iterator<Class<?>> iter = availableGames.iterator();
        while (iter.hasNext()) {
            Class<?> clazz = iter.next();

            if (!Game.class.isAssignableFrom(clazz)) {
                System.err.println("WARNING: Game class " + clazz.getName() + " does not implement Game and will be unavailable.");
                iter.remove();
                continue;
            }

            try {
                clazz.getConstructor(this.getClass());
            } catch (NoSuchMethodException e) {
                System.err.println("WARNING: Game class " + clazz.getName() + " does not have an appropriate constructor and will be unavailable.");
                iter.remove();
                break;
            }

            GameEntrypoint annotation = clazz.getAnnotation(GameEntrypoint.class);

            if (annotation.minimumPlayers() < 1 || annotation.maximumPlayers() < 1) {
                System.err.println("WARNING: Game class " + clazz.getName() + " specifies a min or max player count less than 1 and will be unavailable.");
                iter.remove();
                break;
            }

            if (annotation.minimumPlayers() > annotation.maximumPlayers()) {
                System.err.println("WARNING: Game class " + clazz.getName() + " specifies a minimum player count greater than the maximum and will be unavailable.");
                iter.remove();
                break;
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

        listener = new GameListener<>(this);
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 1 && CMD_CFGGAMES.equals(commands.get(0))) {
            return CFGGAMES_OPTS;
        }

        if (event instanceof GenericChannelEvent) {
            GenericChannelEvent gce = (GenericChannelEvent) event;

            if (!perChannelState.containsKey(gce.getChannel())) {
                perChannelState.put(gce.getChannel(), new ChannelState<>());
            }

            switch (perChannelState.get(gce.getChannel()).getGamePhase()) {
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
                case SETUP:
                    if (commands.size() == 0) {
                        Set<String> retval = new HashSet<>();
                        retval.addAll(CMDS_ALWAYS_AVAILABLE);
                        retval.addAll(CMDS_PHASE_SETUP);
                        retval.addAll(perChannelState.get(gce.getChannel()).getActiveGame().getSetupPhaseCommands(event, commands));
                        return retval;
                    } else {
                        return Collections.emptySet();
                    }
                case ACTIVE:
                    if (commands.size() == 0) {
                        Set<String> retval = new HashSet<>();
                        retval.addAll(CMDS_ALWAYS_AVAILABLE);
                        retval.addAll(CMDS_PHASE_ACTIVE);
                        retval.addAll(perChannelState.get(gce.getChannel()).getActiveGame().getActivePhaseCommands(event, commands));
                        return retval;
                    } else {
                        return Collections.emptySet();
                    }
                default:
                    return CMDS_ALWAYS_AVAILABLE;
            }
        }

        return CMDS_ALWAYS_AVAILABLE;
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() < 1) { // We ain't makin' no assumptions!
            return;
        }

        if (CMD_CFGGAMES.equals(commands.get(0))) {
            if (event instanceof GenericChannelEvent) {
                GenericChannelEvent gce = (GenericChannelEvent) event;

                if (commands.size() == 1) {
                    boolean isEnabled = gameEnabledChannels.contains(gce.getChannel().getName().toLowerCase());
                    event.respond("Games are " + (isEnabled ? "" : "not ") + "enabled in this channel.");
                } else if (commands.size() == 2) {
                    if (acl.hasPermission(event, PERM_GAMEMASTER)) {
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

        if (CMD_HALT.equals(commands.get(0))) {
            if (event instanceof GenericChannelEvent) {
                GenericChannelEvent gce = (GenericChannelEvent) event;

                if (acl.hasPermission(event, PERM_GAMEMASTER) || gce.getChannel().isOp(event.getUser())) {
                    switch (perChannelState.get(gce.getChannel()).getGamePhase()) {
                        case INACTIVE:
                            event.respond("There's no game running to be halted.");
                            break;
                        case ACTIVE:
                            perChannelState.get(gce.getChannel()).getActiveGame().gameHaltRequest(event);
                        case SETUP:
                            sendChannelMessage(gce, "Game was forcibly stopped by an admin.");
                            signalGameStopped(perChannelState.get(gce.getChannel()).getActiveGame());
                    }
                } else {
                    event.respond("You must have the \"" + PERM_GAMEMASTER + "\" global bot permission or be a channel operator to use this command.");
                }
            } else {
                event.respond("This command must be run in a channel.");
            }

            return;
        }

        if (event instanceof GenericChannelEvent) {
            GenericChannelEvent gce = (GenericChannelEvent) event;

            if (!perChannelState.containsKey(gce.getChannel())) {
                perChannelState.put(gce.getChannel(), new ChannelState<>());
            }

            ChannelState<T> channelState = perChannelState.get(gce.getChannel());

            switch (channelState.getGamePhase()) {
                case INACTIVE:
                    if (CMD_PLAY.equals(commands.get(0))) {
                        if (!gameEnabledChannels.contains(gce.getChannel().getName().toLowerCase())) {
                            event.respond("Games are disabled in this channel.");
                            return;
                        }

                        if (commands.size() == 1) {
                            event.respond("The following games are available for play: " +
                                Miscellaneous.getStringRepresentation(Miscellaneous.asSortedList(getAvailableGameCommandNames())));
                            event.respond("To play one of these, use the command: " + CMD_PLAY + " [gameName]");
                        } else if (commands.size() == 2) {
                            Class<?> clazz = availableGames.stream()
                                .filter(clzz -> clzz.getAnnotation(GameEntrypoint.class).commandName().equals(commands.get(1)))
                                .findFirst()
                                .orElse(null);

                            if (clazz == null) {
                                event.respond("Failed to find game class");
                                return;
                            } else {
                                GameEntrypoint clazzAnnotation = clazz.getAnnotation(GameEntrypoint.class);
                                String gameName = clazzAnnotation.properName();
                                Integer minPlayers = clazzAnnotation.minimumPlayers();
                                Integer maxPlayers = clazzAnnotation.maximumPlayers();

                                if (clazzAnnotation.requiresChanops() && !gce.getChannel().isOp(event.getBot().getUserBot())) {
                                    event.respond(gameName + " cannot be played in this channel: Bot must be opped.");
                                    return;
                                }
                                
                                Game<T> game;

                                try {
                                    game = (Game<T>) clazz.getConstructor(this.getClass()).newInstance(this);
                                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                                    event.respond("Failed to instantiate game class: " + e.getMessage());
                                    return;
                                }
                                
                                                               
                                for (Field f : clazz.getDeclaredFields()) {
                                    if (f.isAnnotationPresent(InjectSes.class)) {
                                        try {
                                            f.set(game, ses);
                                        } catch (IllegalAccessException e) {
                                            event.respond("Failed to inject requested ScheduledExecutorService; cannot load game class");
                                            return;
                                        }
                                    }
                                }
                                
                                channelState.setGamePhaseSetup(game);

                                sendChannelMessage(gce, event.getUser().getNick() + " has instantiated a game of \002" +
                                    gameName + "\002!");

                                sendChannelMessage(gce, "To join, use the \"" + CMD_JOIN + "\" command.  Once players have joined, use the \"" +
                                    CMD_START + "\" command to start.  If the game isn't started in 20 minutes, or if all players leave, the " +
                                    "game will be stopped.");

                                sendChannelMessage(gce, "Other commands: \"" + CMD_LEAVE + "\" to leave the game, \"" + CMD_PLAYERS + "\" to see the current players list.");

                                sendChannelMessage(gce, "Minimum required players: " + minPlayers + "; maximum allowed players: " + maxPlayers);
                                
                                channelState.getScheduledFutureMap().put(ScheduledFutureType.GAME_SETUP_TIMER, ses.schedule(() -> {
                                    sendChannelMessage(gce, "Attention: " + Miscellaneous.getStringRepresentation(channelState.getAllPlayerNicks()));
                                    sendChannelMessage(gce, "The game was not started in time and has been stopped.  If you'd like to start again, use the command: " + CMD_PLAY + " [gameName]");
                                    channelState.setGamePhaseInactive();
                                }, 20L, TimeUnit.MINUTES));

                                handleJoin(channelState, event);
                            }
                        } else {
                            event.respond("Too many arguments.  Syntax: " + CMD_PLAY + " {gameName}");
                        }
                    } else if (CMD_GAMES.equals(commands.get(0))) {
                        sendChannelMessage(gce, "The following games are available for play:");
                        getGamesList().forEach(game -> sendChannelMessage(gce, game));
                        return;
                    }
                    break;
                case SETUP:
                    GameEntrypoint annotation = channelState.getActiveGame().getClass().getAnnotation(GameEntrypoint.class);

                    switch (commands.get(0)) {
                        case CMD_JOIN:
                            handleJoin(channelState, event);
                            break;
                        case CMD_LEAVE:
                            if (channelState.getPlayers().contains(new Player(event.getUser()))) {
                                handleLeaveDuringSetup(gce.getChannel(), event.getUser());
                            } else {
                                event.respond("You can't leave a game you aren't in!");
                            }
                            break;
                        case CMD_PLAYERS:
                            event.respond("The name of the game is \002" + annotation.properName() + "\002.  Players (in join order): " +
                                Miscellaneous.getStringRepresentation(channelState.getAllPlayerNicks()));
                            event.respond("Minimum players required: " + annotation.minimumPlayers() + "; maximum players allowed: " + annotation.maximumPlayers());
                            long secondsUntilHalt = channelState.getScheduledFutureMap().get(ScheduledFutureType.GAME_SETUP_TIMER).getDelay(TimeUnit.SECONDS);
                            long minutesUntilHalt = secondsUntilHalt / 60L;
                            secondsUntilHalt %= 60L;
                            event.respond("The game must be started within \002" + minutesUntilHalt + 'm' + secondsUntilHalt + "s\002 or setup will be terminated.");
                            break;
                        case CMD_START:
                            if (channelState.getPlayers().contains(new Player(event.getUser()))) {
                                if (channelState.getPlayers().size() < annotation.minimumPlayers()) {
                                    event.respond("Not enough players to start game; " + (annotation.minimumPlayers() - channelState.getPlayers().size()) + " more required.");
                                } else {
                                    sendChannelMessage(gce, "Launching game!");
                                    channelState.getScheduledFutureMap().get(ScheduledFutureType.GAME_SETUP_TIMER).cancel(true);
                                    channelState.setGamePhaseActive();
                                    channelState.getActiveGame().gameStartRequest(event, channelState);
                                }
                            } else {
                                event.respond("Only players in a game may start the game.");
                            }
                            break;
                        case CMD_PLAY:
                            event.respond("A game has already been instantiated.  All players must leave the current game if you wish to change the game.");
                            break;
                        default:
                            channelState.getActiveGame().handleSetupPhaseCommand(event, commands, remainder);
                            break;
                    }
                    break;
                case ACTIVE:
                    switch (commands.get(0)) {
                        case CMD_LEAVE:
                            // We don't say anything here; we just delegate to the Game
                            channelState.getActiveGame().playerQuit(gce.getChannel(), event.getUser());
                            break;
                        case CMD_PLAYERS:
                            List<String> players = channelState.getAllPlayerNicks();
                            event.respond("There are \002" + players.size() + "\002 players.  In join order: " +
                                Miscellaneous.getStringRepresentation(players));
                            channelState.getActiveGame().playersCommandHook(event);
                            break;
                        default:
                            channelState.getActiveGame().handleActivePhaseCommand(event, commands, remainder);
                            break;
                    }
                    break;
            }
        }
    }

    public static void sendChannelMessage(GenericChannelEvent<?> gce, String message) {
        gce.getBot().sendIRC().message(gce.getChannel().getName(), message);
    }

    public static void sendChannelMessage(GenericMessageEvent<?> event, String message) {
        sendChannelMessage((GenericChannelEvent) event, message);
    }

    public static void sendChannelMessage(Channel channel, String message) {
        channel.getBot().sendIRC().message(channel.getName(), message);
    }

    private Set<String> getAvailableGameCommandNames() {
        return availableGames.stream()
            .map(clazz -> clazz.getAnnotation(GameEntrypoint.class))
            .map(GameEntrypoint::commandName)
            .collect(Collectors.toSet());
    }

    private Set<String> getGamesList() {
        return availableGames.stream()
            .map(clazz -> clazz.getAnnotation(GameEntrypoint.class))
            .map(a -> {
                StringBuilder sb = new StringBuilder("\002")
                    .append(a.properName())
                    .append("\002");
                if (!a.longDescription().isEmpty()) {
                    sb.append(": ")
                        .append(a.longDescription());
                }
                sb.append(" (Min players: \002")
                    .append(a.minimumPlayers())
                    .append("\002; max players: \002")
                    .append(a.maximumPlayers())
                    .append("\002) To play, use command: \002")
                    .append(CMD_PLAY)
                    .append(" ")
                    .append(a.commandName())
                    .append('\002');
                return sb.toString();
            })
            .collect(Collectors.toSet());
    }

    private synchronized void sync() {
        Gson gson = new Gson();
        String gameEnabledChannelsSerialized = gson.toJson(gameEnabledChannels, PERSISTENCE_TYPE_TOKEN);
        pm.set("gameEnabledChannels", gameEnabledChannelsSerialized);
        pm.sync();
    }

    public void signalGameStopped(Game<T> game) {
        // Find the ChannelStates that have a reference to the given game and set them to the Inactive phase
        perChannelState.entrySet().stream()
            .filter(entry -> !entry.getValue().getGamePhase().equals(GamePhase.INACTIVE))
            .filter(entry -> entry.getValue().getActiveGame() == game) // Yes, deep equality
            .forEach(entry -> {
                entry.getValue().setGamePhaseInactive();
                // oh god
                entry.getKey().getBot().sendIRC().message(entry.getKey().getName(), "Game has been stopped - use the \"" + CMD_PLAY + "\" command to start a new game!");
                // I don't like this
            });
    }

    private void handleJoin(ChannelState<T> state, GenericMessageEvent<T> event) {
        Integer maxPlayers = state.getActiveGame().getClass().getAnnotation(GameEntrypoint.class).maximumPlayers();

        if (state.getPlayers().contains(new Player(event.getUser()))) {
            event.respond("You're already in the game, silly human!");
        } else {
            if (state.getPlayers().size() >= maxPlayers) {
                event.respond("Sorry, the maximum number of players for this game has been reached.  :(");
            } else {
                state.getPlayers().add(new Player(event.getUser()));
                sendChannelMessage((GenericChannelEvent) event, '\002' + event.getUser().getNick() + "\002 has joined the game; there are now " + state.getPlayers().size() + " players.");
            }
        }
    }

    void handleLeaveDuringSetup(Channel channel, User user) {
        ChannelState<T> state = perChannelState.get(channel);

        if (GamePhase.SETUP.equals(state.getGamePhase()) && state.getPlayers().contains(new Player(user))) {
            state.getPlayers().remove(new Player(user));
            if (state.getPlayers().size() > 0) {
                sendChannelMessage(channel, '\002' + user.getNick() + "\002 has chickened out.  " +
                    state.getPlayers().size() + " players remaining.");
            } else {
                sendChannelMessage(channel, '\002' + user.getNick() +
                    "\002 chickend out and no players are remaining.  The game has been stopped and a new game may now be selected.");
                state.setGamePhaseInactive();
            }
        }
    }

    Map<Channel, ChannelState<T>> getPerChannelState() {
        return perChannelState;
    }

    public GameListener<T> getListener() {
        return listener;
    }
}
