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

package org.lizardirc.beancounter.commands.remind;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.pircbotx.PircBotX;

class TimedReminderProcessor<T extends PircBotX> implements Runnable {
    private final ReminderListener<T> reminderListener;

    public TimedReminderProcessor(ReminderListener<T> reminderListener) {
        this.reminderListener = reminderListener;
    }

    @Override
    public synchronized void run() {
        // This is simple enough - when called, go through the list of timed reminders, find any that are at or
        // past their delivery times, and check their targets.  If target is online, deliver immediately - else,
        // put in the standard reminders last to schedule delivery next time the target is online.

        // Find ready-to-be-delivered timed reminders and handle them

        Iterator<TimedReminder> iter = reminderListener.getTimedReminders().iterator();
        while (iter.hasNext()) {
            TimedReminder tr = iter.next();
            if (tr.isDeliveryTime()) {
                if (!reminderListener.deliverReminder(tr)) {
                    reminderListener.getReminders().add(tr);
                }

                iter.remove();
            }
        }

        reminderListener.sync();

        // Check if there are any remaining timed reminders, and if so, schedule another run of the processor
        if (!reminderListener.getTimedReminders().isEmpty()) {
            if (reminderListener.getScheduledFuture() != null) {
                reminderListener.getScheduledFuture().cancel(false);
            }
            reminderListener.setScheduledFuture(reminderListener.getSes().schedule(this, ZonedDateTime.now().until(reminderListener.getTimedReminders().get(0).getDeliveryTime(), ChronoUnit.SECONDS) + 1, TimeUnit.SECONDS));
        }
    }
}
