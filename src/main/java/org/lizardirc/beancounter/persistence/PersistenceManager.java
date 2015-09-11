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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

import org.lizardirc.beancounter.utils.Bases;

public interface PersistenceManager {
    PersistenceManager getNamespace(String name);

    Optional<String> get(String name);

    default Optional<Integer> getInt(String name) {
        try {
            return get(name).map(Integer::parseInt);
        } catch (NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    default Stream<String> getStream(String name) {
        return get(name)
            .map(s -> Arrays.stream(s.split(","))
                    .map(Bases::base64decode)
            )
            .orElse(Stream.of());
    }

    default List<String> getList(String name) {
        return getStream(name).collect(Collectors.toList());
    }

    default Set<String> getSet(String name) {
        return getStream(name).collect(Collectors.toSet());
    }

    default Map<String, String> getMap(String name) {
        return get(name)
            .map(s -> Arrays.stream(s.split(","))
                .map(entry -> entry.split(":"))
                .collect(Collectors.toMap(
                        arr -> Bases.base64decode(arr[0]),
                        arr -> Bases.base64decode(arr[1]))
                ))
            .orElse(ImmutableMap.of());
    }

    void set(String name, String value);

    default void setInt(String name, int value) {
        set(name, String.valueOf(value));
    }

    default void setStream(String name, Stream<String> value) {
        String joined = value
            .map(Bases::base64encode)
            .collect(Collectors.joining(","));
        set(name, joined);
    }

    default void setList(String name, List<String> value) {
        setStream(name, value.stream());
    }

    default void setSet(String name, Set<String> value) {
        setStream(name, value.stream());
    }

    default void setMap(String name, Map<String, String> value) {
        String joined = value.entrySet().stream()
            .map(e -> Bases.base64encode(e.getKey()) + ":" + Bases.base64encode(e.getValue()))
            .collect(Collectors.joining(","));
        set(name, joined);
    }

    void sync();
}
