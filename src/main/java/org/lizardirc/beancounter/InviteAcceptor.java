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
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class InviteAcceptor<T extends PircBotX> extends ListenerAdapter<T> {
    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final InviteAcceptorHandler<T> commandHandler = new InviteAcceptorHandler<>();

    private boolean allowInvites;

    public InviteAcceptor(PersistenceManager pm, AccessControl<T> acl) {
        this.pm = pm;
        this.acl = acl;
        allowInvites = pm.getBoolean("allowInvites").orElse(true);
    }

    public void onInvite(InviteEvent<T> event) {
        if (allowInvites) {
            event.getBot().sendIRC().joinChannel(event.getChannel());
            event.getBot().sendIRC().message(event.getChannel(), "I was invited to join this channel by " + event.getUser() + ".");
        }
    }

    public CommandHandler<T> getCommandHandler() {
        return commandHandler;
    }

    private synchronized void sync() {
        pm.setBoolean("allowInvites", allowInvites);
        pm.sync();
    }

    private class InviteAcceptorHandler<T2 extends PircBotX> implements CommandHandler<T2> {
        private static final String CMD_CFG_INVITES = "cfginvites";
        private final Set<String> COMMANDS = ImmutableSet.of(CMD_CFG_INVITES);

        private static final String CFG_OP_ACCEPT = "accept";
        private static final String CFG_OP_REJECT = "reject";
        private final Set<String> CFG_OPERATIONS = ImmutableSet.of(CFG_OP_ACCEPT, CFG_OP_REJECT);

        private static final String PERM_CFG_INVITES = CMD_CFG_INVITES;

        @Override
        public Set<String> getSubCommands(GenericMessageEvent<T2> event, List<String> commands) {
            if (commands.isEmpty()) {
                return COMMANDS;
            }

            if (commands.size() == 1) {
                return CFG_OPERATIONS;
            }

            return Collections.emptySet();
        }

        @Override
        public void handleCommand(GenericMessageEvent<T2> event, List<String> commands, String remainder) {
            if (commands.size() == 1) {
                event.respond("I am currently configured to " + (allowInvites ? "accept" : "reject") + " invitations to join channels");
                if (acl.hasPermission(event, PERM_CFG_INVITES)) {
                    event.respond("Syntax to change this: " + CMD_CFG_INVITES + " <" + Miscellaneous.getStringRepresentation(CFG_OPERATIONS, "|") + ">");
                }
            } else if (commands.size() >= 2) {
                if (acl.hasPermission(event, PERM_CFG_INVITES)) {
                    switch (commands.get(1)) {
                        case CFG_OP_ACCEPT:
                            allowInvites = true;
                            event.respond("I will now accept invites to join channels.");
                            sync();
                            break;
                        case CFG_OP_REJECT:
                            allowInvites = false;
                            event.respond("I will now reject invites to join channels.");
                            sync();
                            break;
                    }
                } else {
                    event.respond("No u! (You don't have the necessary permissions to do this.)");
                }
            }
        }
    }
}
