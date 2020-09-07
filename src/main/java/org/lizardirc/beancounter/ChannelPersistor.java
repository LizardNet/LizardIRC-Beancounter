/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2020 by the LizardIRC Development Team. Some rights reserved.
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

import java.util.Set;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.PartEvent;

import org.lizardirc.beancounter.persistence.PersistenceManager;

public class ChannelPersistor extends ListenerAdapter {
    private static final String PERSIST_CHANNELS = "channels";
    private final Set<String> channels;
    private final PersistenceManager pm;

    public ChannelPersistor(PersistenceManager pm) {
        this.pm = pm;

        channels = pm.getSet(PERSIST_CHANNELS);
    }

    public synchronized void onConnect(ConnectEvent event) {
        channels.forEach(event.getBot().sendIRC()::joinChannel);
    }

    public synchronized void onJoin(JoinEvent event) {
        if (event.getBot().getUserBot().equals(event.getUser())) {
            channels.add(event.getChannel().getName());
            sync();
        }
    }

    public synchronized void onKick(KickEvent event) {
        if (event.getBot().getUserBot().equals(event.getRecipient())) {
            channels.remove(event.getChannel().getName());
            sync();
        }
    }

    public synchronized void onPart(PartEvent event) {
        if (event.getBot().getUserBot().getNick().equals(event.getUser().getNick())) {
            channels.remove(event.getChannel().getName());
            sync();
        }
    }

    private void sync() {
        pm.setSet(PERSIST_CHANNELS, channels);
        pm.sync();
    }
}
