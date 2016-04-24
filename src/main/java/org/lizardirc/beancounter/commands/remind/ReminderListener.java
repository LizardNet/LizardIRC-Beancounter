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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Bases;

public class ReminderListener<T extends PircBotX> extends ListenerAdapter<T> {
    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final ScheduledExecutorService ses;
    private final Set<Reminder> reminders;
    private final List<TimedReminder> timedReminders;
    private final CommandHandler<T> commandHandler = new ReminderListenerHandler<>();

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
            scheduledFuture = ses.schedule(new TimedReminderProcessor(), 20, TimeUnit.SECONDS);
        }
    }

    private synchronized void sync() {
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

    private boolean deliverReminder(Reminder r) {
        // IMPORTANT: THIS METHOD DOES NOT REMOVE THE REMINDER FROM THE LIST OF REMINDERS!
        // You must do this in whatever methods call this one!

        // First, is our target a user@host (target user was online at time of reminder request), or just a nickname
        // (target user was offline at time of reminder request)?
        String target = r.getTarget();
        if (target.contains("@")) {
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
            if (!r.getChannel().equals("")) {
                String targetChannel = r.getChannel();

                if (bot.getUserBot().getChannels().stream()
                    .map(Channel::getName)
                    .anyMatch(targetChannel::equals)
                    ) {
                    Stream<User> users2 = users.stream()
                        .filter(u -> u.getChannels().stream()
                            .map(Channel::getName)
                            .anyMatch(targetChannel::equals));

                    // users2 is now a list of users that match the target hostmask and are in the target channel
                    bot.sendIRC().message(targetChannel,
                        users2.map(User::getNick)
                            .collect(Collectors.joining(", ")
                            ) + ": " + r.toString());
                    return true;
                }
            }

            // Fall back to sending the reminder in a private message, a PM to each matching user.
            users.stream()
                .map(User::getNick)
                .forEach(n -> bot.sendIRC().message(n, r.toString()));
            return true;
        } else {
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

    private synchronized void clearAllReminders() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        reminders.clear();
        timedReminders.clear();
        sync();
    }

    private static class Reminder {
        private final String from; // Full nick@user@host mask of the sending user
        private final String target; // user@host of the target user, or just a nickname (usually because target user was offline when the reminder was requested)
        private final String message;
        private final String channel; // Channel name, including prefix, if reminder was requested in a channel, otherwise an empty string (means reminder was probably requested in PM)
        private final ZonedDateTime enteredTime;

        public Reminder(String from, String target, String message, String channel, ZonedDateTime enteredTime) {
            this.from = from;
            this.target = target.toLowerCase();
            this.message = message;
            this.channel = channel.toLowerCase();
            this.enteredTime = enteredTime;
        }

        @Override
        public String toString() {
            long seconds = enteredTime.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
            long days = seconds / (24 * 60 * 60);
            seconds %= (24 * 60 * 60);
            long hours = seconds / (60 * 60);
            seconds %= (60 * 60);
            long minutes = seconds / 60;
            seconds %= 60;

            String[] sender = from.split("!");

            StringBuilder response = new StringBuilder();
            if (sender[1].equalsIgnoreCase(target) || sender[0].equalsIgnoreCase(target)) {
                response.append("YOU");
            } else {
                response.append(sender[0])
                    .append(" (")
                    .append(sender[1])
                    .append(")");
            }

            response.append(" asked me at ")
                .append(enteredTime.format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .append(" (")
                .append(days)
                .append(" days, ")
                .append(hours)
                .append(" hours, ")
                .append(minutes)
                .append(" minutes, and ")
                .append(seconds)
                .append(" seconds ago) to remind you: ")
                .append(message);

            return response.toString();
        }

        public String toSerializedString() {
            return Bases.base64encode(from) + ':' + Bases.base64encode(target) + ':' + Bases.base64encode(message) + ':' +
                Bases.base64encode(channel) + ':' + enteredTime.toEpochSecond();
        }

        public static Reminder fromSerializedString(String stringRepresentation) {
            String[] s = stringRepresentation.split(":");
            return new Reminder(Bases.base64decode(s[0]), Bases.base64decode(s[1]), Bases.base64decode(s[2]),
                Bases.base64decode(s[3]), ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s[4])), ZoneId.systemDefault()));
        }

        public String getTarget() {
            return target;
        }

        public String getChannel() {
            return channel;
        }

        // Okay, now for the hard part
        @Override
        public int hashCode() {
            return Objects.hash(from.toLowerCase(), target, message, channel, enteredTime);
        }

        public boolean canEqual(Object other) {
            return other instanceof Reminder;
        }

        @Override
        public boolean equals(Object other) {
            boolean result = false;
            if (other instanceof Reminder) {
                Reminder that = (Reminder) other;
                result = that.canEqual(this)
                    && this.from.equalsIgnoreCase(that.from)
                    && this.target.equalsIgnoreCase(that.target)
                    && this.message.equals(that.message)
                    && this.channel.equalsIgnoreCase(that.channel)
                    && this.enteredTime.equals(that.enteredTime);
            }

            return result;
        }
    }

    // Extend and/or implement all the things!
    private static class TimedReminder extends Reminder implements Comparable<TimedReminder> {
        private final ZonedDateTime deliverAt;

        public TimedReminder(String from, String target, String message, String channel, ZonedDateTime enteredTime, ZonedDateTime deliverAt) {
            super(from, target, message, channel, enteredTime);
            this.deliverAt = deliverAt; // Deliver reminder at or after this time
        }

        public TimedReminder(Reminder reminder, ZonedDateTime deliverAt) {
            super(reminder.from, reminder.target, reminder.message, reminder.channel, reminder.enteredTime);
            this.deliverAt = deliverAt;
        }

        @Override
        public int compareTo(TimedReminder o) {
            return Long.compare(this.getDeliveryTimeEpoch(), o.getDeliveryTimeEpoch());
        }

        @Override
        public String toSerializedString() {
            String s = super.toSerializedString();
            return s + ':' + deliverAt.toEpochSecond();
        }

        public static TimedReminder fromSerializedString(String stringRepresentation) {
            String[] s = stringRepresentation.split(":");
            return new TimedReminder(Reminder.fromSerializedString(stringRepresentation),
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(s[5])), ZoneId.systemDefault()));
        }

        public ZonedDateTime getDeliveryTime() {
            return deliverAt;
        }

        public long getDeliveryTimeEpoch() {
            return deliverAt.toEpochSecond();
        }

        public boolean isDeliveryTime() {
            return deliverAt.until(ZonedDateTime.now(), ChronoUnit.SECONDS) >= 0;
        }
    }

    private class TimedReminderProcessor implements Runnable {
        @Override
        public synchronized void run() {
            // This is simple enough - when called, go through the list of timed reminders, find any that are at or
            // past their delivery times, and check their targets.  If target is online, deliver immediately - else,
            // put in the standard reminders last to schedule delivery next time the target is online.

            // Find ready-to-be-delivered timed reminders and handle them

            Iterator<TimedReminder> iter = timedReminders.iterator();
            while (iter.hasNext()) {
                TimedReminder tr = iter.next();
                if (tr.isDeliveryTime()) {
                    if (!deliverReminder(tr)) {
                        reminders.add(tr);
                    }

                    iter.remove();
                }
            }

            sync();

            // Check if there are any remaining timed reminders, and if so, schedule another run of the processor
            if (!timedReminders.isEmpty()) {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(false);
                }
                scheduledFuture = ses.schedule(this, ZonedDateTime.now().until(timedReminders.get(0).getDeliveryTime(), ChronoUnit.SECONDS) + 1, TimeUnit.SECONDS);
            }
        }
    }

    private class ReminderListenerHandler<U extends PircBotX> implements CommandHandler<U> {
        private static final String COMMAND_REMIND = "remind";
        private static final String COMMAND_REMIND_ME = "remindme";
        private static final String COMMAND_CLEAR_REMINDERS = "clearreminders";
        private final Set<String> COMMANDS = ImmutableSet.of(COMMAND_REMIND, COMMAND_REMIND_ME, COMMAND_CLEAR_REMINDERS);

        private static final String PERM_CLEAR_REMINDERS = "clearreminders";

        @Override
        public Set<String> getSubCommands(GenericMessageEvent<U> event, List<String> commands) {
            if (commands.isEmpty()) {
                return COMMANDS;
            }

            return Collections.emptySet();
        }

        @Override
        public void handleCommand(GenericMessageEvent<U> event, List<String> commands, String remainder) {
            if (commands.size() < 1) {
                return;
            }

            if (commands.get(0).equals(COMMAND_CLEAR_REMINDERS)) {
                if (acl.hasPermission(event, PERM_CLEAR_REMINDERS)) {
                    clearAllReminders();
                    event.respond("Done.  *All* reminders have been purged.");
                    return;
                } else {
                    event.respond("No u! (You don't have the necessary permissions to do this.)");
                    return;
                }
            }

            String target;
            String from;
            String message;
            String channel;
            ZonedDateTime enteredTime = ZonedDateTime.now();
            ZonedDateTime timedReminderDeliveryTime;

            remainder = remainder.trim();
            String[] args = remainder.split(" ");

            if (commands.get(0).equals(COMMAND_REMIND) || commands.get(0).equals(COMMAND_REMIND_ME)) {
                if (remainder.isEmpty()) {
                    showNoMessageError(event, commands);
                    return;
                }

                if (commands.get(0).equals(COMMAND_REMIND)) {
                    if (event.getBot().getUserChannelDao().userExists(args[0])) {
                        User targetUser = event.getBot().getUserChannelDao().getUser(args[0]);
                        target = (targetUser.getLogin() + '@' + targetUser.getHostmask()).toLowerCase();
                    } else {
                        target = args[0];
                    }

                    remainder = remainder.substring(args[0].length()).trim();
                    args = remainder.split(" ");
                } else {
                    target = (event.getUser().getLogin() + '@' + event.getUser().getHostmask()).toLowerCase();
                }

                if (event instanceof GenericChannelEvent) {
                    channel = ((GenericChannelEvent) event).getChannel().getName();
                } else {
                    channel = "";
                }
                from = event.getUser().getNick() + '!' + event.getUser().getLogin() + '@' + event.getUser().getHostmask();

                if (args[0].equalsIgnoreCase("in:")) {
                    try {
                        timedReminderDeliveryTime = processTimeSpec(args[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        event.respond("Error: Missing time specification.");
                        return;
                    } catch (Exception e) {
                        event.respond("Error: \"" + args[1] + "\" is not a valid time specification: " + e.getMessage() + ".  Time specifications look like this: 1y2w3d4h5m6s, meaning \"one year, two weeks, three days, four hours, five minutes, and six seconds\".");
                        event.respond("All \"fields\" of a time specification are optional (e.g., 10d is equivalent to 0y0w10d0h0m0s).");
                        return;
                    }

                    message = remainder.substring((args[0] + " " + args[1]).length()).trim();
                    if (message.isEmpty()) {
                        showNoMessageError(event, commands);
                        return;
                    }
                    TimedReminder tr = new TimedReminder(from, target, message, channel, enteredTime, timedReminderDeliveryTime);
                    timedReminders.add(tr);
                    Collections.sort(timedReminders);
                    sync();
                    if (scheduledFuture != null) {
                        scheduledFuture.cancel(false);
                    }
                    scheduledFuture = ses.schedule(new TimedReminderProcessor(), ZonedDateTime.now().until(timedReminders.get(0).getDeliveryTime(), ChronoUnit.SECONDS) + 1, TimeUnit.SECONDS);
                    event.respond("Reminder successfully scheduled; will be delivered at or after " + timedReminderDeliveryTime.format(DateTimeFormatter.RFC_1123_DATE_TIME) + " when the target user is online (note: reminders may be delivered by private message).");
                } else {
                    if (remainder.isEmpty()) {
                        showNoMessageError(event, commands);
                        return;
                    }
                    Reminder r = new Reminder(from, target, remainder, channel, enteredTime);
                    reminders.add(r);
                    sync();
                    event.respond("Reminder successfully recorded; will be delivered as soon as I see the target user talk in or join a channel I'm in (note: reminders may be delivered by private message).");
                }
            }
        }

        private ZonedDateTime processTimeSpec(String timespec) throws IllegalArgumentException {
            PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendYears().appendSuffix("y")
                .appendWeeks().appendSuffix("w")
                .appendDays().appendSuffix("d")
                .appendHours().appendSuffix("h")
                .appendMinutes().appendSuffix("m")
                .appendSeconds().appendSuffix("s")
                .toFormatter();

            ZonedDateTime retval = ZonedDateTime.now().plusSeconds(formatter.parsePeriod(timespec).toDurationFrom(org.joda.time.Instant.now()).getStandardSeconds());
            if (retval.getYear() > 9999) {
                throw new IllegalArgumentException("Specified time is too far in the future (past year 9999). Go away.");
            }
            return retval;
        }

        private void showNoMessageError(GenericMessageEvent<U> event, List<String> commands) {
            switch (commands.get(0)) {
                case COMMAND_REMIND:
                    event.respond("Error: You have to provide a nickname and a message for the reminder!");
                    event.respond("Syntax: " + COMMAND_REMIND + " [nickname] {in: [time]} [message]");
                    break;
                case COMMAND_REMIND_ME:
                    event.respond("Error: You have to provide a message for the reminder!");
                    event.respond("Syntax: " + COMMAND_REMIND_ME + " {in: [time]} [message]");
                    break;
            }
        }
    }
}
