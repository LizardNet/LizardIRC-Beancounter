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

package org.lizardirc.beancounter.gameframework.playermanagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.pircbotx.User;

/**
 * This class is responsible for managing players in a game.  Basically, it handles the abstraction between PircBotX
 * {@link User}s (which represent individual users on IRC) and the {@link Player} objects representing players in-game.
 * <p>
 *
 * Generally, there is a one-to-one mapping between Users and Players; however, there are some cases when we want to use
 * fuzzier matching, for example when handling player substitutions.
 */
public class PlayerManager {
    private final BiMap<User, Player> playerStore;
    private final List<Player> ingamePlayerList;

    private boolean gameStarted = false;

    /**
     * Your basic constructor.  Sets up internal storage.
     */
    public PlayerManager() {
        playerStore = Maps.synchronizedBiMap(HashBiMap.create());
        ingamePlayerList = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Adds a user to the game, and returns the Player object that will be used to represent them.  The returned Player
     * object should be the exact same object that will be returned by subsequent calls to {@link
     * #getPlayerOfUser(User)} (i.e., reference equality), but it is guaranteed that the returned Player object will
     * have value equality with the Player object returned by subsequent calls to {@code getPlayerOfUser()}.
     *
     * @param user The User to be added to the game
     * @return The Player object representing the user in the context of the game
     * @throws IllegalArgumentException If the specified User, or a User with the same ident and host, is already
     *                                  in-game.
     */
    public Player addPlayer(User user) {
        if (isPlayingIgnoreNick(user)) {
            throw new IllegalArgumentException("Specified User is already in this game.");
        }

        Player newPlayer = new Player(this);
        playerStore.put(user, newPlayer);
        ingamePlayerList.add(newPlayer);
        return newPlayer;
    }

    /**
     * Check if a given User is in the game
     *
     * @param user The user to be checked
     * @return {@code true} if the User is in the game, {@code false} otherwise.
     * @see #isPlayingIgnoreNick(User)
     */
    public boolean isPlaying(User user) {
        Player player = getPlayerOfUser(user);
        return player != null && player.isPlayerIngame();
    }

    /**
     * Check if a given User is in the game, but ignoring their nickname; i.e., this method will check if anyone with
     * the given User object's ident and host is in the game.<p>
     *
     * Note: This is a rather expensive operation.
     *
     * @param user The user to be checked
     * @return {@code true} if a user with a matching ident and host is in the game, {@code false} otherwise.
     */
    public boolean isPlayingIgnoreNick(User user) {
        Player player = getPlayerOfUserIgnoringNick(user);
        return player != null && player.isPlayerIngame();
    }

    /**
     * Removes a player from the game.  Exact semantics of usage of this method are up to the game, but this method
     * is designed to be usable both for a player leaving a game of their own volition, or for a player being "killed"
     * in a game and thus removed from the active player list.<p>
     *
     * If the game is running (indicated by the game calling {@link #signalGameStarted()}), the indicated player is not
     * actually removed from the PlayerManager's internal storage so calls to, say, {@link #getPlayerOfUser(User)} will
     * still succeed, but they are marked as removed and no longer included in the output of, for example, {@link
     * #getIngamePlayerList()}.<p>
     *
     * This method operates on PircBotX {@link User} objects.  See {@link #removePlayer(Player)} for a version of this
     * method that takes {@link Player} objects as its parameter.
     *
     * @param user The user to be removed from the game
     */
    public void removePlayer(User user) {
        if (!isPlaying(user)) {
            throw new NoSuchPlayerException("Attempt to remove a User who is not playing.");
        }

        if (gameStarted) {
            getPlayerOfUser(user).setPlayerRemoved(true);
            ingamePlayerList.remove(playerStore.get(user));
        } else {
            ingamePlayerList.remove(playerStore.remove(user));
        }
    }

    /**
     * Removes a player from the game.  Exact semantics of usage of this method are up to the game, but this method
     * is designed to be usable both for a player leaving a game of their own volition, or for a player being "killed"
     * in a game and thus removed from the active player list.<p>
     *
     * If the game is running (indicated by the game calling {@link #signalGameStarted()}), the indicated player is not
     * actually removed from the PlayerManager's internal storage so calls to, say, {@link #getPlayerOfUser(User)} will
     * still succeed, but they are marked as removed and no longer included in the output of, for example, {@link
     * #getIngamePlayerList()}.<p>
     *
     * This method operates on PircBotX {@link Player} objects.  See {@link #removePlayer(User)} for a version of this
     * method that takes {@link User} objects as its parameter.
     *
     * @param player The player to be removed from the game
     */
    public void removePlayer(Player player) {
        requireValidPlayer(player);

        if (gameStarted) {
            player.setPlayerRemoved(true);
        } else {
            playerStore.inverse().remove(player);
        }

        ingamePlayerList.remove(player);
    }

    /**
     * Substitutes a given User for the given Player, updating the mapping so that the Player will now be mapped to the
     * given User object.  Also performs checks to ensure that a user cannot be added to the game twice via this method,
     * and that a player can't be substituted for themselves.
     *
     * @param user The new user to be substituted in
     * @param player The player for which the new user is to be substitited in
     * @return The User object representing the previous mapping of the Player
     * @throws IlegelSubstitutionException If the substitution cannot be allowed
     */
    public User substituteUserForPlayer(User user, Player player) throws IlegelSubstitutionException {
        requireValidPlayer(player);
        User oldUser = playerStore.inverse().get(player);

        if (oldUser.equals(user)) {
            throw new IlegelSubstitutionException("Attempt to substitute a User in for themselves.  Do you think this is a motherfucking game?  ಠ_ಠ");
        }

        if (isPlaying(user)) {
            throw new IlegelSubstitutionException("Attempt to substitute in a User who is already playing for another User.");
        }

        if (player.isPlayerRemoved()) {
            throw new IlegelSubstitutionException("Attempt to substitute in a User for a Player who has been removed from play.");
        }

        playerStore.inverse().forcePut(player, user);
        return oldUser;
    }

    /**
     * Returns a list of players in game.  This excludes players that have been removed from play, even if valid
     * User &lt;-&gt; Player mappings still exist for them.
     *
     * @return A defensive copy of the ingame player list (modifying this returned List will not affect the
     *         PlayerManager's internal version).
     */
    public List<Player> getIngamePlayerList() {
        return new ArrayList<>(ingamePlayerList); // Ensure that our internal player list is not modified externally
    }

    /**
     * Gets the Player object that corresponds to the given PircBotX User object.
     *
     * @param user The user to get the corresponding Player for
     * @return The corresponding Player object
     * @see #getPlayerOfUserIgnoringNick(User)
     */
    public Player getPlayerOfUser(User user) {
        return playerStore.get(user);
    }

    /**
     * Gets the Player object that corresponds to the given PircboX User object, but ignoring the User's nickname.  That
     * is, gets the Player object for any User with an ident and host matching that of the given User object.<p>
     *
     * Note: This is a rather expensive operation.
     *
     * @param user The User for which we want to get the corresponding Player object
     * @return The corresponding Player object
     * @see #getPlayerOfUser(User)
     */
    public Player getPlayerOfUserIgnoringNick(User user) {
        // This is a rather expensive operation.

        return playerStore.entrySet().stream()
            .filter(e -> e.getKey().getHostmask().equalsIgnoreCase(user.getHostmask())
                && e.getKey().getLogin().equalsIgnoreCase(user.getLogin()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the Player's current nickname
     *
     * @param player The Player we want to get the nickname of
     * @return The player's nickname
     */
    public String getNicknameOf(Player player) {
        return playerStore.inverse().get(requireValidPlayer(player)).getNick();
    }

    /**
     * Returns the Player's current username/ident
     *
     * @param player The Player we want to get the username/ident of
     * @return The player's username/ident
     */
    public String getUsernameOf(Player player) {
        return playerStore.inverse().get(requireValidPlayer(player)).getLogin();
    }

    /**
     * Returns the Player's current host
     *
     * @param player The Player we want to get the host of
     * @return The player's host
     */
    public String getHostOf(Player player) {
        return playerStore.inverse().get(requireValidPlayer(player)).getHostmask();
    }

    /**
     * Used to indicate to the PlayerManager that the game has started, and that removed players should no longer have
     * their mappings removed.  Called automatically by the Games Framework; individual games do not need to worry
     * about calling this.
     */
    public void signalGameStarted() {
        gameStarted = true;
    }

    /**
     * Validates that the given Player object is valid.  This is defined as the Player object having been produced by
     * this PlayerManager, and the PlayerManager still having a valid Player &lt;-&gt; User mapping for that Player.
     *
     * @param player The player to be checked
     * @return The same Player object, if valid
     * @throws IllegalArgumentException If the given Player is invalid
     * @throws NoSuchPlayerException if the given Player is invalid
     */
    private Player requireValidPlayer(Player player) {
        Objects.requireNonNull(player);

        if (!player.getProducingFactory().equals(this)) {
            throw new IllegalArgumentException("Attempt to pass PlayerManager a Player object not produced by that PlayerManager!");
        }

        if (!playerStore.containsValue(player)) {
            throw new NoSuchPlayerException("Attempt to pass PlayerManager a Player object representing a player removed during game setup.");
        }

        return player;
    }

    /**
     * Returns the number of players in-game (but not necessarily how many players we have valid Player &lt;-&gt; User
     * mappings for).
     *
     * @return The number of players in-game
     */
    public int getIngamePlayerCount() {
        return ingamePlayerList.size();
    }

    /**
     * Cleans up the Player &lt;-&gt; User mappings.  Not meant to be called by games; instead, called automatically
     * by the Games Handler when a game ends to hopefully prevent memory leaks.
     */
    public void nukePlayers() {
        // Ensure all player references are destroyed here, to hopefully prevent memory leaks
        playerStore.clear();
        ingamePlayerList.clear();
    }
}
