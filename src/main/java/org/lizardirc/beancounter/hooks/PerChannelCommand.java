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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class PerChannelCommand<T extends PircBotX> implements CommandHandler<T> {
    private final LoadingCache<Channel, ? extends CommandHandler<T>> childListeners;

    public PerChannelCommand(Function<Channel, ? extends CommandHandler<T>> childFunction) {
        childListeners = CacheBuilder.newBuilder()
            .build(CacheLoader.from(childFunction));
    }

    public PerChannelCommand(Supplier<? extends CommandHandler<T>> childSupplier) {
        childListeners = CacheBuilder.newBuilder()
            .build(CacheLoader.from(childSupplier));
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        Channel channel = getChannel(event);
        if (channel != null) {
            try {
                return childListeners.get(channel).getSubCommands(event, commands);
            } catch (ExecutionException e) {
                e.printStackTrace(); // TODO log this properly
            }
        }
        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        Channel channel = getChannel(event);
        if (channel != null) {
            try {
                childListeners.get(channel).handleCommand(event, commands, remainder);
            } catch (ExecutionException e) {
                e.printStackTrace(); // TODO log this properly
            }
        }
    }

    private Channel getChannel(GenericEvent<T> event) {
        if (event instanceof GenericChannelEvent) {
            GenericChannelEvent gce = (GenericChannelEvent) event;
            return gce.getChannel();
        }
        return null;
    }
}
