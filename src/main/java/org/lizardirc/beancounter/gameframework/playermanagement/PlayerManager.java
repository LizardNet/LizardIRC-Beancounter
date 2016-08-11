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

public class PlayerManager {
    private final BiMap<User, Player> playerStore;
    private final List<Player> ingamePlayerList;

    private boolean gameStarted = false;

    public PlayerManager() {
        playerStore = Maps.synchronizedBiMap(HashBiMap.create());
        ingamePlayerList = Collections.synchronizedList(new ArrayList<>());
    }

    public Player addPlayer(User user) {
        if (isPlayingIgnoreNick(user)) {
            throw new IllegalArgumentException("Specified User is already in this game.");
        }

        Player newPlayer = new Player(this);
        playerStore.put(user, newPlayer);
        ingamePlayerList.add(newPlayer);
        return newPlayer;
    }

    public boolean isPlaying(User user) {
        Player player = getPlayerOfUser(user);
        return player != null && player.isPlayerIngame();
    }

    public boolean isPlayingIgnoreNick(User user) {
        Player player = getPlayerOfUserIgnoringNick(user);
        return player != null && player.isPlayerIngame();
    }

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

    public void removePlayer(Player player) {
        requireValidPlayer(player);

        if (gameStarted) {
            player.setPlayerRemoved(true);
        } else {
            playerStore.inverse().remove(player);
        }

        ingamePlayerList.remove(player);
    }

    public User substituteUserForPlayer(User user, Player player) throws IlegelSubstitutionException {
        requireValidPlayer(player);
        User oldUser = playerStore.inverse().get(player);

        if (oldUser.equals(user)) {
            throw new IlegelSubstitutionException("Attempt to substitute a User in for themselves.  Do you think this is a motherfucking game?  ಠ_ಠ");
        }

        if (player.isPlayerRemoved()) {
            throw new IlegelSubstitutionException("Attempt to substitute in a User for a Player who has been removed from play.");
        }

        playerStore.inverse().forcePut(player, user);
        return oldUser;
    }

    public List<Player> getIngamePlayerList() {
        return new ArrayList<>(ingamePlayerList); // Ensure that our internal player list is not modified externally
    }

    public Player getPlayerOfUser(User user) {
        return playerStore.get(user);
    }

    public Player getPlayerOfUserIgnoringNick(User user) {
        // This is a rather expensive operation.

        return playerStore.entrySet().stream()
            .filter(e -> e.getKey().getHostmask().equalsIgnoreCase(user.getHostmask())
                && e.getKey().getLogin().equalsIgnoreCase(user.getLogin()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public String getNicknameOf(Player player) {
        return playerStore.inverse().get(requireValidPlayer(player)).getNick();
    }

    public String getUsernameOf(Player player) {
        return playerStore.inverse().get(requireValidPlayer(player)).getLogin();
    }

    public String getHostOf(Player player) {
        return playerStore.inverse().get(requireValidPlayer(player)).getHostmask();
    }

    public void signalGameStarted() {
        gameStarted = true;
    }

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

    public int getIngamePlayerCount() {
        return ingamePlayerList.size();
    }

    public void nukePlayers() {
        // Ensure all player references are destroyed here, to hopefully prevent memory leaks
        playerStore.clear();
        ingamePlayerList.clear();
    }
}
