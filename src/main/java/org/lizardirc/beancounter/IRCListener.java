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

package org.lizardirc.beancounter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandListener;

public class IRCListener<T extends PircBotX> extends CommandListener<T> {
    private static final Set<String> COMMANDS = ImmutableSet.of("quit", "slap", "test");

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }
        switch (commands.get(0)) {
            case "slap":
                if (!(event instanceof GenericChannelEvent)) {
                    break;
                }
                GenericChannelEvent gce = (GenericChannelEvent) event;
                return gce.getChannel().getUsers().stream()
                        .map(User::getNick)
                        .collect(Collectors.toSet());
        }
        return Collections.<String>emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 0) {
            return;
        }
        switch (commands.get(0)) {
            case "quit":
                event.getBot().sendIRC().quitServer("Tear in salami");
                break;
            case "slap":
                String target = event.getUser().getNick();
                if (commands.size() >= 2) {
                    target = commands.get(1);
                }
                String channel = event.getUser().getNick();
                if (event instanceof GenericChannelEvent) {
                    channel = ((GenericChannelEvent) event).getChannel().getName();
                }
                event.getBot().sendIRC().action(channel, "slaps " + target + " around a bit with a large trout");
                break;
            case "test":
                event.respond("Hello world!");
                break;
        }
    }
}
