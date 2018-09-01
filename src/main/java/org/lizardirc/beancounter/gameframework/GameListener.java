/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2016-2020 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.gameframework;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;

class GameListener extends ListenerAdapter {
    private final GameHandler parent;

    public GameListener(GameHandler parent) {
        this.parent = parent;
    }

    @Override
    public void onPart(PartEvent event) throws Exception {
        onPartOrKick(event.getChannel().getGeneratedFrom(), event.getUser().getGeneratedFrom());
    }

    @Override
    public void onKick(KickEvent event) throws Exception {
        onPartOrKick(event.getChannel(), event.getRecipient());
    }

    @Override
    public void onQuit(QuitEvent event) throws Exception {
        User user = event.getUser().getGeneratedFrom();

        // Dammit PircBotX.  This would be so much more efficient if you let me get a list of channels a user was in
        // before they quit....
        parent.getPerChannelState().entrySet().stream()
            .filter(e -> e.getValue().getPlayerManager() != null && e.getValue().getPlayerManager().isPlaying(user))
            .forEach(e -> {
                switch (e.getValue().getGamePhase()) {
                    case SETUP:
                        parent.handleLeaveDuringSetup(e.getKey(), user);
                        break;
                    case ACTIVE:
                        parent.handleLeaveDuringActive(e.getValue(), user);
                        break;
                }
            });
    }

    private void onPartOrKick(Channel channel, User user) {
        ChannelState state = parent.getPerChannelState().get(channel);

        if (state != null) {
            if (GamePhase.SETUP.equals(state.getGamePhase())){
                parent.handleLeaveDuringSetup(channel, user);
            } else if (GamePhase.ACTIVE.equals(state.getGamePhase())) {
                if (state.getPlayerManager().isPlaying(user)) {
                    parent.handleLeaveDuringActive(state, user);
                }
            }
        }
    }
}
