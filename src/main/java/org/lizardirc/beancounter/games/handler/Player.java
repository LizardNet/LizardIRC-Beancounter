package org.lizardirc.beancounter.games.handler;

import java.util.Objects;

import org.pircbotx.User;

/**
 * Warning: This class has a natural ordering inconsistent with its equals()!
 */
public class Player implements Comparable<Player> {
    private final String nickname;
    private final String username;
    private final String host;

    public Player(User user) {
        nickname = user.getNick();
        username = user.getLogin();
        host = user.getHostmask();
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase(), host.toLowerCase());
    }

    public boolean canEquals(Object o) {
        return o instanceof Player;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;

        if (o instanceof Player) {
            Player that = (Player) o;
            result = that.canEquals(this) &&
                username.equalsIgnoreCase(that.username) &&
                host.equalsIgnoreCase(that.host);
        }

        return result;
    }

    public String getNick() {
        return getNickname();
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public int compareTo(Player o) {
        return nickname.compareToIgnoreCase(o.nickname);
    }
}
