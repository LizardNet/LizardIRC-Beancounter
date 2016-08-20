/*
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

package org.lizardirc.beancounter.commands.help;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.hooks.MultiCommandHandler;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class HelpHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_COMMANDS = "commands";
    private static final Set<String> COMMANDS = ImmutableSet.of(COMMAND_HELP, COMMAND_COMMANDS); // ARE YOU CONFUSED YET?!

    private static final String MESSAGE_HELP = "My documentation, including a list of commands I support, can be found on LizardIRC's website: <https://www.lizardirc.org/index.php?page=beancounter>; " +
        "my source code can be found on GitHub: <https://github.com/LizardNet/LizardIRC-Beancounter>. You can also use the \"commands\" command to get a list of commands.";

    private final MultiCommandHandler<T> multiCommandListener;

    public HelpHandler(MultiCommandHandler<T> multiCommandListener) {
        this.multiCommandListener = multiCommandListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 1) {
            switch (commands.get(0)) {
                case COMMAND_HELP:
                    event.respond(MESSAGE_HELP);
                    break;
                case COMMAND_COMMANDS:
                    String knownCommands;

                    knownCommands = Miscellaneous.getStringRepresentation(Miscellaneous.asSortedList(multiCommandListener.getSubCommands(event, ImmutableList.of())));
                    event.respond("I am aware of the following commands: " + knownCommands);
                    break;
            }
        }
    }
}
