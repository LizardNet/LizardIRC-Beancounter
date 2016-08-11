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
package org.lizardirc.beancounter.commands.fishbot;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

class FishbotCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_FISHBOT = "fishbot";

    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_FISHBOT);

    private static final String SUBCMD_ON = "on";
    private static final String SUBCMD_OFF = "off";
    private static final Set<String> SUBCOMMANDS = ImmutableSet.of(SUBCMD_ON, SUBCMD_OFF);


    private final FishbotListener<T> parentListener;

    FishbotCommandHandler(FishbotListener<T> parentListener) {
        this.parentListener = parentListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        } else {
            return SUBCOMMANDS;
        }
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.isEmpty() || !CMD_FISHBOT.equals(commands.get(0))) {
            return;
        }

        if (!(event instanceof GenericChannelEvent)) {
            event.respond("This command may only be run in a channel.");
            return;
        }

        if (commands.size() == 1) {
            // ?fishbot  - report current status
            boolean enabled = parentListener.getStatus(((GenericChannelEvent) event).getChannel().getName());
            String status = enabled ? "enabled" : "disabled";
            event.respond("fishb0t is currently " + status + " in this channel.");
        }

        if (commands.size() == 2) {
            // ?fishbot on|off  - change status
            if (!((GenericChannelEvent) event).getChannel().isOp(event.getUser())) {
                event.respond("You must be a channel operator to enable or disable fishb0t.");
                return;
            }

            switch (commands.get(1)) {
                case SUBCMD_OFF:
                    parentListener.disable(((GenericChannelEvent) event).getChannel().getName());
                    event.respond("Done");
                    break;
                case SUBCMD_ON:
                    parentListener.enable(((GenericChannelEvent) event).getChannel().getName());
                    event.respond("Done");
                    break;
            }
        }
    }
}
