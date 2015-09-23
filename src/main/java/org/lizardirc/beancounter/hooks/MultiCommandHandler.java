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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class MultiCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private final Set<CommandHandler<T>> subCommandHandlers;

    // Mmm, generics
    private Map<GenericMessageEvent<T>, Map<List<String>, Set<CommandHandler<T>>>> listenerMap = new WeakHashMap<>();

    public MultiCommandHandler() {
        subCommandHandlers = new HashSet<>();
    }

    public MultiCommandHandler(Collection<CommandHandler<T>> c) {
        subCommandHandlers = new HashSet<>(c);
    }

    public synchronized void add(CommandHandler<T> listener) {
        subCommandHandlers.add(listener);
    }

    public synchronized void remove(CommandHandler<T> listener) {
        subCommandHandlers.remove(listener);
    }

    public synchronized Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        Set<CommandHandler<T>> listeners = subCommandHandlers;
        if (listenerMap.containsKey(event)) {
            listeners = listenerMap.get(event).get(ImmutableList.copyOf(commands));
        } else {
            listenerMap.put(event, new HashMap<>());
        }

        Map<List<String>, Set<CommandHandler<T>>> map = listenerMap.get(event);
        for (CommandHandler<T> listener : listeners) {
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

    public synchronized void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (!listenerMap.containsKey(event)) {
            throw new IllegalStateException("Listener map not populated");
        }

        Set<CommandHandler<T>> listeners = listenerMap.get(event).get(commands);
        if (listeners == null) {
            throw new IllegalStateException("No possible handlers for command");
        } else if (listeners.size() != 1) {
            throw new IllegalStateException(listeners.size() + " possible handlers for command");
        }

        listenerMap.remove(event);

        for (CommandHandler<T> listener : listeners) {
            listener.handleCommand(event, commands, remainder);
        }
    }

    private List<String> append(List<String> commands, String tail) {
        List<String> ret = new ArrayList<>(commands);
        ret.add(tail);
        return Collections.unmodifiableList(ret);
    }
}
