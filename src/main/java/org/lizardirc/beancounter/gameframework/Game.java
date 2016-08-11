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

package org.lizardirc.beancounter.gameframework;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

/**
 * LIZARDIRC BEANCOUNTER GAMES PLATFORM ENTERPRISE EDITION 1<p>
 *
 * This interface represents the standard methods any BGP-EE game must (abstract methods) or may (default methods)
 * implement.  Please refer to the Javadoc of each respective method for more information.<p>
 *
 * Note that a class implementing this interface does not mean it will be detected as a game by Beancounter - the game's
 * main class must implement this interface and be annotated with {@code @}{@link GameEntrypoint}, which provides
 * access to some metadata without having to instantiate the game's class.  See the documentation for
 * {@code @GameEntrypoint} for more information about the contract game classes must follow, including the constructor
 * game classes must provide.<p>
 *
 * A general warning: Ensure that in any of the four methods that provide commands,
 * (get|handle)(Active|Setup)PhaseCommands?, you don't provide a command whose name clashes with a command specified
 * elsewhere in the main section of the bot (primarily org.lizardirc.beancounter.commands.*).  Until the bot has proper
 * handling of contextual commands, this will cause an exception to be thrown whenever the command is given, and all
 * instances of the command will be unusable.
 *
 * @param <T> I still have no idea what this is actually for.  Thanks PircBotX!
 */
public interface Game<T extends PircBotX> {
    /**
     * This method returns the commands (and subcommanes) that are recognized by the game during the ACTIVE phase.  This
     * is the most important part of the game class, as this is where the actual gameplay commands are provided.  Only
     * a couple commands are provided standard by GameHandler and intercepted (specifically, ?leave, ?players, and
     * ?substitute).  Everything else is passed directly to the game class using {@link
     * #handleActivePhaseCommand(GenericMessageEvent, List, String)}.<p>
     *
     * Works similarly to {@link
     * org.lizardirc.beancounter.hooks.CommandHandler#getSubCommands(GenericMessageEvent, List)}<p>
     *
     * The GameHandler will ensure that only players are allowed to run these commands.
     *
     * @param event The event that triggered the command request - if the game is defined as {@linkplain
     *              GameEntrypoint#usesPrivateMessageCommands() using private message commands}, may be a channel event
     *              or a private message event.  Otherwise, guaranteed to be a channel event.
     * @param commands The command(s) to get subcommands of, or an empty list to get the base commands.
     * @return The available commands/subcommands
     */
    Set<String> getActivePhaseCommands(GenericMessageEvent<T> event, List<String> commands);

    /**
     * This method handles the commands recognized by the game class during the ACTIVE phase.  This is the most
     * important part of the game class, as this is where the actual gameplay commands are handled.  The GameHandler
     * provides a few standard commands that will never be seen by this method (specifically, ?leave, ?players, and
     * ?substitute), even though during the active phase these will lead to other methods provided by this interface
     * being called.  Aside from those three, everything is forwarded to this method for handling.<p>
     *
     * Works similarly to {@link
     * org.lizardirc.beancounter.hooks.CommandHandler#handleCommand(GenericMessageEvent, List, String)}<p>
     *
     * The GameHandler will ensure that only players are allowed to run these commands.
     *
     * @param event The event that triggered the command request - if the game is defined as {@linkplain
     *              GameEntrypoint#usesPrivateMessageCommands() using private message commands}, may be a channel event
     *              or a private message event.  Otherwise, guaranteed to be a channel event.
     * @param commands The command (with subcommands, if any) to be processed
     * @param remainder The remainder - i.e., freeform arguments not "predicted" by a subcommand
     */
    void handleActivePhaseCommand(GenericMessageEvent<T> event, List<String> commands, String remainder);

    /**
     * This method is called as soon as a game is successfully started (i.e., right after {@link
     * #isGameStartable(GenericMessageEvent)} returns {@code true}.  The GameHandler's ScheduledExecutorService is also
     * passed as a parameter, allowing the game class the ability to schedule tasks; however, using it is entirely
     * optional, and game classes are free to simply leave the parameter unused if they don't need it.<p>
     *
     * This is the class where one should do the game start setup, such as printing rules or assigning roles.<p>
     *
     * By the time this method is called, the game is now in the ACTIVE phase.<p>
     *
     * If a game class uses the provided ScheduledExecutorService, it is the developer's responsibility to ensure that
     * all ScheduledFutures using the ScheduledExecutorService are cancelled and cleaned up before the game terminates!
     *
     * @param event The event that triggered the ?start command (this will be the same as the event parameter for
     *              {@link #isGameStartable(GenericMessageEvent)}).  Guaranteed to be a channel event.
     * @param scheduledExecutorService The GameHandler's ScheduledExecutorService
     */
    void gameStart(GenericMessageEvent<T> event, ScheduledExecutorService scheduledExecutorService);

    /**
     * This method is called whenever a player quits the game, regardless of how that happens.  It could have been
     * because they used the ?leave command, or because they actually parted the channel or quit IRC.  The game class
     * should take this opportunity to handle the quit, whether by ending the game or by - at a minimum - removing the
     * player from the player list (note that, in this case, the GameHandler does <b>not</b> do this for you - the game
     * class must at a minimum remove the player from the player list or Terrible and Undefined Things™ could happen!).
     * <p>
     *
     * A reference to the player list object created by the GameHandler's ChannelState map is injected by the
     * GameHandler into the constructor required by the {@code @}{@link GameEntrypoint} annotation's contract.<p>
     *
     * If you want to quit the game here, call {@link GameHandler#signalGameStopped(Game)} with {@code this} as the
     * parameter; a reference to the GameHandler is also injected by GameHandler into the constructor required by the
     * {@code @GameEntrypoint} annotation.
     *
     * @param player The player who left
     */
    void playerQuit(Player player);

    /**
     * This method returns the commands (and subcommands) that are recognized by the game during the SETUP phase; i.e.,
     * after a game has been selected but before it starts.  These will be recognized in addition to the standard SETUP
     * phase commands (?joingame, ?leave, ?players, ?substitute, and ?start) and are passed by the GameHandler directly
     * to the game class (using {@link #handleSetupPhaseCommand(GenericMessageEvent, List, String)}).<p>
     *
     * Works similarly to {@link
     * org.lizardirc.beancounter.hooks.CommandHandler#getSubCommands(GenericMessageEvent, List)}<p>
     *
     * This method is optional; if you don't implement it, the default action is to indicate no setup phase commands are
     * available by returning an empty set.  Warning: If you do implement any SETUP phase commands, you must implement
     * both this method and {@link #handleSetupPhaseCommand(GenericMessageEvent, List, String)}!<p>
     *
     * The GameHandler will ensure that only players are allowed to run these commands.
     *
     * @param event The event that triggered the command request.  Guaranteed to be a channel event.
     * @param commands The command(s) to get subcommands of, or an empty list to get the base commands.
     * @return The available commands/subcommands
     */
    default Set<String> getSetupPhaseCommands(GenericMessageEvent<T> event, List<String> commands) {
        return Collections.emptySet();
    }

    /**
     * This method handles commands recognized by the game class during the SETUP phase.  Note that the game class
     * should only attempt to handle the commands it specifies in {@link
     * #getSetupPhaseCommands(GenericMessageEvent, List)}; the commands that are provided standard by GameHandler during
     * the SETUP phase (e.g., ?joingame and ?start) are handled by GameHandler itself and will never be passed to the
     * game class, though they may lead to other game class methods being called (e.g., ?start will eventually lead to
     * GameHandler calling {@link #isGameStartable(GenericMessageEvent)} if the minimum player count is met).<p>
     *
     * Works similarly to {@link
     * org.lizardirc.beancounter.hooks.CommandHandler#handleCommand(GenericMessageEvent, List, String)}<p>
     *
     * This method is optional; if you don't implement it, the default action is to do nothing (i.e., not handle any
     * SETUP phase commands).  Warning: If you do implement any SETUP phase commands, you must implement both this
     * method and {@link #getSetupPhaseCommands(GenericMessageEvent, List)}!<p>
     *
     * The GameHandler will ensure that only players are allowed to run these commands.
     *
     * @param event The event that triggered the command.  Guaranteed to be a channel event.
     * @param commands The command (with subcommands, if any) to be processed
     * @param remainder The remainder - i.e., freeform arguments not "predicted" by a subcommand
     */
    default void handleSetupPhaseCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        // Goes nowhere, does nothing
    }

    /**
     * This method is called when the minimum player count defined by the game class's {@code @}{@link GameEntrypoint}
     * annotation is met and a player calls the ?start command.  This method should <b>only</b> do checks that the game
     * is startable (beyond the minimum player count criteria, which is checked and enforced by the GameHandler) and not
     * do any game setup; game setup should be done after the game state has advanced to ACTIVE, i.e., in {@link
     * #gameStart(GenericMessageEvent, ScheduledExecutorService)}.  This method may, however, output messages (via the
     * event parameter) that are pertinent to verifying that the game is startable; usually, these are messages to
     * inform players that some game-specific start criteria have not yet been met.<p>
     *
     * This method can only be called when the game is in the SETUP phase; if it returns true, upon returning the
     * GameHandler will advance the game phase to ACTIVE and game commands become enabled.  If it returns false, the
     * GameHandler will notify the channel that the game has not started and the game phase remains in SETUP.<p>
     *
     * This method is optional; if you don't implement it, the default action is to return {@code true} - effectively,
     * this means the only check that the game is startable is the built-in minimum player count check in GameHandler.
     *
     * @param event The event that triggered the ?start command.  Guaranteed to be a channel event.
     * @return Whether or not the game is startable
     */
    default boolean isGameStartable(GenericMessageEvent<T> event) {
        return true;
    }

    /**
     * This method is called when a channel operator or a user with the "gamemaster" global bot permission calls the
     * ?halt command.  Here, do any cleanup you want to do (such as informing users of the game state at the time ?halt
     * was called) and return.  The GameHandler will handle resetting the game phase to INACTIVE and notifying the
     * channel that the game was stopped by an admin.  Note that a halt cannot be intercepted; the game will be halted
     * by the GameHandler (game state set to INACTIVE, references to the game class instance erased) as soon as this
     * method returns.<p>
     *
     * This method is optional; if you don't implement it, the default action is to do nothing.
     *
     * @param event The event that triggered the ?halt command.  No guarantees are made about the type or origin of the
     *              event.
     */
    default void gameHaltRequest(GenericMessageEvent<T> event) {
        // Go nowhere, do nothing
    }

    /**
     * This method is called after the GameHandler processes any call to the ?players command while the game is in the
     * ACTIVE phase.  This method is not called during the SETUP phase.  In the ACTIVE phase, the ?players command
     * spits out the player list and nothing more; if you want to add something, like some game statistics, this is
     * the place to do it.<p>
     *
     * This method is optional; if you don't implement it, the default action is to do nothing.
     *
     * @param event The event that triggered the ?players command.  If the game is defined as {@linkplain
     *              GameEntrypoint#usesPrivateMessageCommands() using private message commands}, may be a channel event
     *              or a private message event.  Otherwise, guaranteed to be a channel event.
     */
    default void playersCommandHook(GenericMessageEvent<T> event) {
        // Go nowhere, do nothing
    }

    /**
     * This method is called after the GameHandler processes an active player changing their nickname during the ACTIVE
     * phase.  It is not called during the SETUP phase.  The nickname change can be due to an actual /nick command on
     * IRC, or due to another IRC user from the same user@host using the ?substitute command, which allows one to
     * substitute a new connection in for another one (perhaps because the other connection is about to time out).  An
     * example usage of this method is to ensure that in games that use a moderated channel, that the old nickname is
     * devoiced and the new nickname is voiced.<p>
     *
     * This method is optional; if you don't implement it, the default action is to do nothing.
     *
     * @param newNickname The player's old nickname
     * @param oldNickname The player's new nickname
     */
    default void playerNicknameChanged(String newNickname, String oldNickname) {
        // Go nowhere, do nothing
    }
}
