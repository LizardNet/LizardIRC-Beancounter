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

package org.lizardirc.beancounter.events;

import java.util.Objects;

import org.pircbotx.hooks.events.MessageEvent;

public class MessageEventView extends MessageEvent {

    private final MessageEvent childEvent;
    private final String newMessage;

    public MessageEventView(MessageEvent childEvent, String newMessage) {
        super(childEvent.getBot(), childEvent.getChannel(), childEvent.getChannelSource(), childEvent.getUserHostmask(),
                childEvent.getUser(), newMessage, childEvent.getTags());
        this.childEvent = childEvent;
        this.newMessage = newMessage;
    }

    @Override
    public boolean canEqual(Object other) {
        return other instanceof MessageEventView;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MessageEventView) {
            MessageEventView that = (MessageEventView) o;
            return that.canEqual(this)
                    && newMessage.equals(that.newMessage)
                    && super.equals(that);
        }
        return false;
    }

    @Override
    public long getId() {
        return childEvent.getId();
    }

    @Override
    public String getMessage() {
        return newMessage;
    }

    @Override
    public long getTimestamp() {
        return childEvent.getTimestamp();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), newMessage);
    }

    @Override
    public String toString() {
        return super.toString() + " -> '" + newMessage + "'";
    }
}
