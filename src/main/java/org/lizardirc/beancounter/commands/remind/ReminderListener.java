/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2017 by the LizardIRC Development Team. Some rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class ReminderListener<T extends PircBotX> extends ListenerAdapter<T> {
    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final ScheduledExecutorService ses;
    private final Set<Reminder> reminders;
    private final List<TimedReminder> timedReminders;
    private final CommandHandler<T> commandHandler = new ReminderCommandHandler<>(this);

    private T bot;
    private ScheduledFuture scheduledFuture;

    public ReminderListener(PersistenceManager pm, AccessControl<T> acl, ScheduledExecutorService ses) {
        this.pm = pm;
        this.acl = acl;
        this.ses = ses;

        reminders = new HashSet<>(pm.getSet("reminders").stream()
            .map(Reminder::fromSerializedString)
            .collect(Collectors.toSet())
        );

        timedReminders = new ArrayList<>(pm.getList("timedReminders").stream()
            .map(TimedReminder::fromSerializedString)
            .sorted()
            .collect(Collectors.toList())
        );
    }

    public CommandHandler<T> getCommandHandler() {
        return commandHandler;
    }

    @Override
    public void onConnect(ConnectEvent<T> event) {
        bot = event.getBot();

        if (!timedReminders.isEmpty()) {
            scheduledFuture = ses.schedule(new TimedReminderProcessor<>(this), 20, TimeUnit.SECONDS);
        }
    }

    synchronized void sync() {
        pm.setSet("reminders", reminders.stream()
            .map(Reminder::toSerializedString)
            .collect(Collectors.toSet())
        );

        pm.setList("timedReminders", timedReminders.stream()
            .map(TimedReminder::toSerializedString)
            .collect(Collectors.toList())
        );

        pm.sync();
    }

    boolean deliverReminder(Reminder r) {
        System.out.println("ReminderListener: Delivering reminder: " + r.toString() + "; target is " + r.getTarget());
        // IMPORTANT: THIS METHOD DOES NOT REMOVE THE REMINDER FROM THE LIST OF REMINDERS!
        // You must do this in whatever methods call this one!

        // First, is our target a user@host (target user was online at time of reminder request), or just a nickname
        // (target user was offline at time of reminder request)?
        String target = r.getTarget();

        if (target.isEmpty() || target.equalsIgnoreCase("@")) {
            System.err.println("ReminderListener: Reminder has empty target and cannot be delivered; returning true so it is deleted");
            return true;
        }

        if (target.contains("@")) {
            System.out.println("ReminderListener: Target is a hostmask");
            // Yes, we have a user@host
            // Check if the target user is online.  Do nothing if they aren't.
            List<User> users = bot.getUserChannelDao().getAllUsers().stream()
                .filter(s -> (s.getLogin() + '@' + s.getHostmask()).equalsIgnoreCase(target))
                .collect(Collectors.toList());

            if (users.isEmpty()) {
                return false; // No users were online to deliver the reminder to
            }

            // We now have a set of all users matching the target hostmask.  Check if any of them are in the channel the
            // reminder was given in, if it was given in a channel in the first place (i.e., not null), and only if we are
            // currently in that channel.
            if (!r.getChannel().isEmpty()) {
                String targetChannel = r.getChannel();

                if (bot.getUserBot().getChannels().stream()
                    .map(Channel::getName)
                    .anyMatch(targetChannel::equals)
                    ) {
                    Set<String> users2 = users.stream()
                        .filter(u -> u.getChannels().stream()
                            .map(Channel::getName)
                            .anyMatch(targetChannel::equals))
                        .map(User::getNick)
                        .collect(Collectors.toSet());

                    // users2 is now a list of users that match the target hostmask and are in the target channel.  Of
                    // course, this may not actually contain anyone....

                    if (!users2.isEmpty()) {
                        bot.sendIRC().message(targetChannel,
                            Miscellaneous.getStringRepresentation(users2) + ": " + r.toString());
                        return true;
                    }
                }
            }

            // Fall back to sending the reminder in a private message, a PM to each matching user.
            users.stream()
                .map(User::getNick)
                .forEach(n -> bot.sendIRC().message(n, r.toString()));
            return true;
        } else {
            System.out.println("ReminderListener: Target is a nickname");
            // We only have a nickname, which makes the matching logic a lot easier
            // Check if the target nickname is online
            if (bot.getUserChannelDao().userExists(target)) {
                if (!r.getChannel().equals("") && bot.getUserChannelDao().getChannel(r.getChannel()).getUsers().contains(bot.getUserChannelDao().getUser(target))) {
                    // Deliver in channel
                    bot.sendIRC().message(r.getChannel(), target + ": " + r.toString());
                    return true;
                } else {
                    // Deliver in PM
                    bot.sendIRC().message(target, r.toString());
                    return true;
                }
            } else {
                return false; // User is offline; reminder delivery failed
            }
        }
    }

    private synchronized void pounce(GenericUserEvent<T> event) {
        String userHostmask = event.getUser().getLogin() + '@' + event.getUser().getHostmask();
        String userNick = event.getUser().getNick();

        Iterator<Reminder> iter = reminders.iterator();
        while (iter.hasNext()) {
            Reminder r = iter.next();
            if (r.getTarget().equalsIgnoreCase(userHostmask) || r.getTarget().equalsIgnoreCase(userNick)) {
                if (deliverReminder(r)) {
                    iter.remove();
                }
            }
        }
        sync();
    }

    @Override
    public void onJoin(JoinEvent<T> event) {
        pounce(event);
    }

    @Override
    public void onMessage(MessageEvent<T> event) {
        pounce(event);
    }

    @Override
    public void onAction(ActionEvent<T> event) {
        pounce(event);
    }

    synchronized void clearAllReminders() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        reminders.clear();
        timedReminders.clear();
        sync();
    }

    List<TimedReminder> getTimedReminders() {
        return timedReminders;
    }

    Set<Reminder> getReminders() {
        return reminders;
    }

    AccessControl<T> getAcl() {
        return acl;
    }

    ScheduledFuture getScheduledFuture() {
        return scheduledFuture;
    }

    void setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    ScheduledExecutorService getSes() {
        return ses;
    }

    void sortTimedReminders() {
        Collections.sort(timedReminders);
    }
}
