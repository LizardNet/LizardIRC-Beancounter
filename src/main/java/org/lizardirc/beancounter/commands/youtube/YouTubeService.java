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
package org.lizardirc.beancounter.commands.youtube;

import org.lizardirc.beancounter.persistence.PersistenceManager;

public class YouTubeService {
    private static final String PERSISTENCE_APIKEY = "YouTubeApiKey";
    private static final String PERSISTENCE_ENABLED = "YouTubeEnabled";

    private String apiKey = "";
    private boolean enabled = false;

    private final PersistenceManager pm;

    public YouTubeService(PersistenceManager pm) {
        this.pm = pm;

        enabled = pm.getBoolean(PERSISTENCE_ENABLED).orElse(false);
        apiKey = pm.get(PERSISTENCE_APIKEY).orElse(null);
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        pm.set(PERSISTENCE_APIKEY, apiKey);

        if (apiKey == null || apiKey.equals("")) {
            // No API key = no worky 4 u.
            setEnabled(false);
        }
    }

    void setEnabled(boolean enabled) {
        if (enabled && (apiKey == null || apiKey.equals(""))) {
            throw new IllegalArgumentException("Cannot enable YouTube operations without an API key!");
        }

        this.enabled = enabled;
        pm.setBoolean(PERSISTENCE_ENABLED, enabled);
    }

    boolean isEnabled() {
        return enabled;
    }
}
