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

package org.lizardirc.beancounter.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import redis.clients.jedis.Jedis;

import org.lizardirc.beancounter.utils.Bases;

public class RedisPersistenceManager implements PersistenceManager {
    private final Jedis jedis;
    private final String namespace;

    public RedisPersistenceManager(Jedis jedis) {
        this.jedis = jedis;
        namespace = null;
    }

    private RedisPersistenceManager(Jedis jedis, String namespace) {
        this.jedis = jedis;
        this.namespace = namespace;
    }

    @Override
    public PersistenceManager getNamespace(String name) {
        return new RedisPersistenceManager(jedis, qualify(name) + "/");
    }

    @Override
    public Optional<String> get(String name) {
        String ret = jedis.get(qualify(name));
        return ret == null ? Optional.empty() : Optional.of(ret);
    }

    @Override
    public Stream<String> getStream(String name) {
        // TODO make this lazyish
        return jedis.lrange(qualify(name), 0, -1).stream();
    }

    @Override
    public List<String> getList(String name) {
        return jedis.lrange(qualify(name), 0, -1);
    }

    @Override
    public Set<String> getSet(String name) {
        return jedis.smembers(qualify(name));
    }

    @Override
    public Map<String, String> getMap(String name) {
        return jedis.hgetAll(qualify(name));
    }

    @Override
    public Map<String, Set<String>> getMultimap(String name) {
        return getMap(name).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> DEF.NARROW_LIST_PATTERN.splitAsStream(e.getValue())
                    .map(Bases::base64decode)
                    .collect(Collectors.toSet())
            ));
    }

    @Override
    public void set(String name, String value) {
        jedis.set(qualify(name), value);
    }

    @Override
    public void setStream(String name, Stream<String> value) {
        // TODO make this lazyish
        jedis.del(qualify(name));
        jedis.rpush(qualify(name), value.toArray(String[]::new));
    }

    @Override
    public void setList(String name, List<String> value) {
        setStream(name, value.stream());
    }

    @Override
    public void setSet(String name, Set<String> value) {
        jedis.del(qualify(name));
        jedis.sadd(qualify(name), value.toArray(new String[value.size()]));
    }

    @Override
    public void setMap(String name, Map<String, String> value) {
        jedis.del(qualify(name));
        jedis.hmset(qualify(name), value);
    }

    @Override
    public void setMultimap(String name, Map<String, Set<String>> value) {
        Map<String, String> res = value.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                    .map(Bases::base64encode)
                    .collect(DEF.NARROW_LIST_COLLECTOR)
            ));
        setMap(name, res);
    }

    @Override
    public void sync() {
        // Jedis does not require syncing.
        // You can go about your business.
        // Move along.
    }

    private String qualify(String name) {
        name = name.replaceAll("\\$", "$$").replaceAll("/", "$/");
        if (namespace == null) {
            return name;
        }
        return namespace + name;
    }
}
