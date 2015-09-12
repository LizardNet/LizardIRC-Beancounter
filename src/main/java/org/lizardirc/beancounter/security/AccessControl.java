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

package org.lizardirc.beancounter.security;

import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.hooks.types.GenericUserEvent;

public class AccessControl {
    private Map<String, String> accessList;

    public AccessControl(Map<String, String> accessList) {
        if (accessList.isEmpty()) {
            System.err.println("Warning: AccessControl instantiated with empty accessList.");
            System.err.println("Assuming everyone is authorized to access everything!");
            accessList.put("^.*!.*@.*$", "*");
        }
        this.accessList = accessList;
    }

    public Map<String, String> getAccessList() {
        return accessList;
    }

    public void setAccessList(Map<String, String> accessList) {
        this.accessList = accessList;
    }

    public synchronized boolean hasPriv(GenericUserEvent<?> event, String permission) {
        // Apparently, PircBotX has a different definition of "hostmask" than the rest of the IRC world....
        String userHostmask = event.getUser().getNick() + "!" + event.getUser().getLogin() + "@" + event.getUser().getHostmask();

        for (Entry<String, String> entry : accessList.entrySet()) {
            String aclHostmask = entry.getKey();
            String aclPermission = entry.getValue();

            if (userHostmask.matches(aclHostmask)) {
                /* TODO: Currently, we use a single Map of hostmasks->privileges.  Support
                   TODO: a roll-based system which would use two maps; hostmasks->roles and roles->privileges
                   TODO: Suggested by TLUL: Use roles->Set<hostmask>
                 */
                if (permission.equals(aclPermission) || aclPermission.equals("*")) {
                    return true;
                }
            }
        }

        return false;
    }
}
