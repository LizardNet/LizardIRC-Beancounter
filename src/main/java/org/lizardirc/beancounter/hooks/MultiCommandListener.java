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

package org.lizardirc.beancounter.hooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class MultiCommandListener<T extends PircBotX> extends CommandListener<T> {
    private Set<CommandListener<T>> subCommandListeners;

    // Mmm, generics
    private Map<GenericMessageEvent<T>, Map<List<String>, Set<CommandListener<T>>>> listenerMap = new WeakHashMap<>();

    public MultiCommandListener() {
        subCommandListeners = new HashSet<>();
    }

    public MultiCommandListener(Collection<CommandListener<T>> c) {
        subCommandListeners = new HashSet<>(c);
    }

    public void add(CommandListener<T> listener) {
        subCommandListeners.add(listener);
    }

    public void remove(CommandListener<T> listener) {
        subCommandListeners.remove(listener);
    }

    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        Set<CommandListener<T>> listeners = subCommandListeners;
        if (listenerMap.containsKey(event)) {
            listeners = listenerMap.get(event).get(clone(commands));
        } else {
            listenerMap.put(event, new HashMap<>());
        }

        Map<List<String>, Set<CommandListener<T>>> map = listenerMap.get(event);
        for (CommandListener<T> listener : listeners) {
            for (String str : listener.getSubCommands(event, commands)) {
                List<String> newCommands = append(commands, str);
                if (!map.containsKey(newCommands)) {
                    map.put(newCommands, new HashSet<>());
                }
                map.get(newCommands).add(listener);
            }
        }
        return map.keySet().stream()
                .filter(l -> l.size() == commands.size() + 1)
                .map(l -> l.get(l.size() - 1)) // last element of each
                .collect(Collectors.toSet());
    }

    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (!listenerMap.containsKey(event)) {
            throw new IllegalStateException("Listener map not populated");
        }

        Set<CommandListener<T>> listeners = listenerMap.get(event).get(commands);
        if (listeners == null) {
            throw new IllegalStateException("No possible handlers for command");
        } else if (listeners.size() != 1) {
            throw new IllegalStateException(listeners.size() + " possible handlers for command");
        }
        for (CommandListener<T> listener : listeners) {
            listener.handleCommand(event, commands, remainder);
        }
    }

    private List<String> clone(List<String> commands) {
        List<String> ret = new ArrayList<>(commands);
        return Collections.unmodifiableList(ret);
    }

    private List<String> append(List<String> commands, String tail) {
        List<String> ret = new ArrayList<>(commands);
        ret.add(tail);
        return Collections.unmodifiableList(ret);
    }
}
