/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.elite;
;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.commands.elite.eddn.EddnStreamer;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;

public class DangerousCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_CMDR = "cmdr";
    private static final String CMD_CFGEDDN = "cfgeddn";
    private static final String CFGEDDN_ENABLE = "enable"; // Enables EDDN tracking output in the current channel
    private static final String CFGEDDN_DISABLE = "disable"; // Disables EDDN tracking output in the current channel, without deleting the list of tracked CMDRs for that channel
    private static final String CFGEDDN_TRACK = "track"; // Adds the given Commander to the current channel's tracking
    private static final String CFGEDDN_REMOVE = "remove"; // Removes the given Commander from the current channel's tracking
    private static final String CFGEDDN_GETALL = "getall"; // Privileged command - list all channels with EDDN tracking enabled
    private static final Set<String> CMDS_CFGEDDN = ImmutableSet.of(CFGEDDN_ENABLE, CFGEDDN_DISABLE, CFGEDDN_TRACK, CFGEDDN_REMOVE,
        CFGEDDN_GETALL);
    private static final String PERM_CFGEDDN = "cfgeddn";

    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final EddnStreamer eddnStreamer; // WARNING - this will be null to indicate that EDDN access is disabled!

    public DangerousCommandHandler(PersistenceManager pm, AccessControl<T> acl, EddnStreamer eddnStreamer) {
        this.pm = pm;
        this.acl = acl;
        this.eddnStreamer = eddnStreamer;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            Set<String> retval = Sets.newHashSet(CMD_CMDR);
            if (eddnStreamer != null) {
                retval.add(CMD_CFGEDDN);
            }

            return ImmutableSet.copyOf(retval);
        } else if (commands.size() == 1 && eddnStreamer != null) {
            return CMDS_CFGEDDN;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {

    }
}
