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

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandListener;
import org.lizardirc.beancounter.security.AccessControl;

public class AdminListener<T extends PircBotX> extends CommandListener<T> {
    private static final String CMD_QUIT = "quit";
    private static final String CMD_NICK = "nick";
    private static final String CMD_JOIN = "join";
    private static final String CMD_PART = "part";
    private static final String CMD_SAY = "say";
    private static final String CMD_ACT = "act";
    private static final String CMD_RAW = "raw";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_QUIT, CMD_NICK, CMD_JOIN, CMD_PART, CMD_SAY, CMD_ACT, CMD_RAW);

    private static final String PERM_QUIT = CMD_QUIT;
    private static final String PERM_NICK = CMD_NICK;
    private static final String PERM_JOIN = CMD_JOIN;
    private static final String PERM_PART = CMD_PART;
    private static final String PERM_SAY = CMD_SAY;
    private static final String PERM_ACT = CMD_ACT;
    private static final String PERM_RAW = CMD_RAW;
    private static final Set<String> PERMISSIONS = ImmutableSet.of(PERM_QUIT, PERM_NICK, PERM_JOIN, PERM_PART, PERM_SAY, PERM_ACT, PERM_RAW);

    private static final String E_PERMFAIL = "No u! (You don't have the necessary permissions to do this.)";

    private final AccessControl<T> acl;

    public AdminListener(AccessControl<T> acl) {
        this.acl = acl;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }
        return Collections.<String>emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() == 0) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_QUIT:
                String quitMessage = "Tear in salami";
                if (remainder != null && !remainder.trim().isEmpty()) {
                    quitMessage = remainder.trim();
                }
                if (acl.hasPermission(event, PERM_QUIT)) {
                    event.getBot().sendIRC().quitServer(quitMessage);
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_NICK:
                if (acl.hasPermission(event, PERM_NICK)) {
                    if (remainder == null || remainder.trim().isEmpty()) {
                        event.respond("Error: You have to tell me what I should change my nickname to!");
                    } else {
                        event.getBot().sendIRC().changeNick(remainder.split(" ")[0]);
                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
            case CMD_JOIN:
                if (acl.hasPermission(event, PERM_JOIN)) {
                    if (remainder == null || remainder.trim().isEmpty()) {

                    }
                } else {
                    event.respond(E_PERMFAIL);
                }
                break;
        }
    }
}
