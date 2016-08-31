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

import org.lizardirc.beancounter.gameframework.playermanagement.Player;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LIZARDIRC BEANCOUNTER GAMES PLATFORM ENTERPRISE EDITION 1<p>
 *
 * This annotation is used to define the main class for a Beancounter game using BGP-EE.  The contract for a BGP-EE game
 * class is as follows:<p>
 *
 * <ul>
 *     <li>It must have public visiblity</li>
 *     <li>It must define a type parameter {@code <T extends PircBotX>}</li>
 *     <li>It must be annotated with this annotation</li>
 *     <li>It must implement {@link Game}{@code <T>}, following the contracts specified in {@code Game}'s Javadocs</li>
 *     <li>
 *         It must provide a public constructor that takes three arguments in this order: {@code GameHandler<T>},
 *         {@link org.lizardirc.beancounter.gameframework.playermanagement.PlayerManager PlayerManager}, {@code Channel}
 *         <ul>
 *             <li>Respectively, these are injected with references to the parent GameHandler, the GameHandler's
 *             player list for the channel the game is being played in, and the Channel object representing the channel
 *             the game is being played in.</li>
 *         </ul>
 *     </li>
 *     <li>Any limitations described in this annotation's methods' Javadocs must also be followed.</li>
 * </ul>
 *
 * If this contract is followed, the game class will be automatically detected by the bot's GameHandler instance and
 * made available for play.
 *
 * @see Game
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GameEntrypoint {
    /**
     * The proper name of the game.  Or, to put it another way, the human-readable name of the game.
     *
     * @return The proper (human-readable) name of the game.
     */
    String properName();

    /**
     * The command name of the game.  This is the subcommand players will need to use to call the game in GameHandler.
     * Usually, this will be the proper name, but without spaces and possibly shortened for ease of use.<p>
     *
     * Contract: This must be unique to this class.  If multiple game classes specify the same command name (normalized
     * to lower case), any clashing game classes after the first is loaded will not be loaded (the load order is
     * arbitrary and no guarantees are made about it).
     *
     * @return The command name of the game
     */
    String commandName();

    /**
     * The minimum number of players required for the game to be played.  The player count will never be below this
     * number when the game is started, strongly enforced by the GameHandler.  However, GameHandler will do nothing if
     * the player count drops below this while the game is in progress; the onus is on the game's developer to handle
     * this condition (see {@link Game#handlePlayerQuit(Player, boolean)} (after all, some games involve removal of
     * players through the course of normal gameplay).<p>
     *
     * Contract: The value specified here must be greater than or equal to 1.
     *
     * @return The minimum number of players needed to start the game
     */
    int minimumPlayers();

    /**
     * The maximum number of players required for the game to be played.  The player count will never be above this
     * number when the game is started, strongly enforced by the GameHandler.  Since players cannot join in-progress
     * games under the current design of GameHandler, this effectively means that a game's player count will never be
     * above this value.<p>
     *
     * Contract: The value specified here must be greater than or equal to 1, and greater than or equal to the
     * {@linkplain #minimumPlayers() minimum player count}.
     *
     * @return The maximum number of players allowed.
     */
    int maximumPlayers();

    /**
     * This metadata informs GameHandler if the game requires the bot to have channel operator status to be played.
     * It is guaranteed that if this is set to {@code true}, the bot will have ops when the game class is first loaded
     * during the SETUP phase.  However, since Beancounter is currently unaware of IRC services, it will not attempt
     * to regain operator status should it lose it after the game class is loaded.  Indeed, currently the bot will never
     * do anything in response to losing ops, even if this is set to {@code true}.<p>
     *
     * This is optional, and defaults to false.
     *
     * @return Whether or not the game requires the bot to have channel operator status
     */
    boolean requiresChanops() default false;

    /**
     * This metadata requests GameHandler to, during the SETUP phase, automatically voice players who join and devoice
     * those who leave.  This defaults to false.  Note that setting this to true implicitly sets {@link
     * #requiresChanops() requiresChanops} to true regardless of any explicit setting.  Note also that setting {@code
     * voicePlayersDuringSetup} to true will not cause the bot to set the channel to moderated or do anything similar.
     * Also note that this setting only has effect during the SETUP phase - if this is true, all players will be voiced
     * when the game starts, but once the game is in the ACTIVE phase, it's the game class's responsibility to handle
     * voicing and devoicing of players, including devoicing players as necessary when the game ends.
     *
     * @return Whether or not the game handler should automatically voice players during the SETUP phase.
     */
    boolean voicePlayersDuringSetup() default false;

    /**
     * This is a "long" description of the game, though perhaps "summary" is a better term.  It should fit in a single
     * IRC line, and is shown when the ?games command is used to enumerate to IRC what games are available.  It is
     * optional, and if not specified only the game name will be shown in the ?games command output.
     *
     * @return The game's long description/summary
     */
    String summary() default "";

    /**
     * This URL will be shown to players when the game is instantiated as a "more information" link.  The specific
     * message is as follows:<p>
     *
     * <blockquote>
     *     Before playing, you may wish to read the game information/rules at: &lt;gameUrl()&gt;
     * </blockquote>
     *
     * If set to the empty string (the default), this line won't be shown.
     *
     * @return The game's more information URL
     */
    String gameUrl() default "";

    /**
     * This advises the GameHandler whether or not the game needs to be able to accept commands in private message.  If
     * this is false (the default), the game will only see commands given in the channel it's being played in, but the
     * player will be allowed to join other games in other channels (provided that all of them also do not require the
     * use of private messages).  If this is set to true, that indicates that the games <i>does</i> require the ability
     * to accept commands in private message.  The GameHandler will route the commands appropriately, but as a
     * consequence, the player will only be able to able to play in a single game at a time.  If a player is in any game
     * and attempts to join a game that has this set to true, they will see an error and be prevented from joining.  If
     * a player is in any game with this set to true and attempts to join any other game, they will also be prevented.
     * They will only be allowed to join multiple games if none of the games have this set to true.<p>
     *
     * Note that private message command forwarding will only take place during the game's ACTIVE phase.
     *
     * @return Whether or not the game needs to be able to receive commands in private messages
     */
    boolean usesPrivateMessageCommands() default false;
}
