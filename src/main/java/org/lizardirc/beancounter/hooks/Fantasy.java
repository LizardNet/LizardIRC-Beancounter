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

package org.lizardirc.beancounter.hooks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import org.lizardirc.beancounter.events.MessageEventView;
import org.lizardirc.beancounter.events.PrivateMessageEventView;

public class Fantasy extends Decorator {
    private static final Pattern ADDRESSED_PATTERN = Pattern.compile("^ *[,:] *(.*)$");
    private final String fantasyPrefix;
    private final int fantasyLength;

    public Fantasy(Listener childListener, String fantasyPrefix) {
        super(childListener);
        this.fantasyPrefix = fantasyPrefix;
        this.fantasyLength = fantasyPrefix.length();
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (event instanceof MessageEvent) {
            MessageEvent me = (MessageEvent) event;
            String newMessage = processMessage(me.getMessage().trim(), event.getBot().getNick());
            if (newMessage != null) {
                super.onEvent(new MessageEventView(me, newMessage));
            }
        } else if (event instanceof PrivateMessageEvent) {
            PrivateMessageEvent me = (PrivateMessageEvent) event;
            String newMessage = processMessage(me.getMessage().trim(), event.getBot().getNick());
            if (newMessage != null) {
                super.onEvent(new PrivateMessageEventView(me, newMessage));
            } else {
                super.onEvent(me);
            }
        }
    }

    private String processMessage(String message, String botNick) {
        if (message.startsWith(fantasyPrefix)) {
            return message.substring(fantasyLength);
        } else if (message.startsWith(botNick)) {
            Matcher m = ADDRESSED_PATTERN.matcher(message.substring(botNick.length()));
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }
}
