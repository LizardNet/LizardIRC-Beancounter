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

package org.lizardirc.beancounter.commands.remind;

import com.google.common.collect.ImmutableSet;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ReminderCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String COMMAND_REMIND = "remind";
    private static final String COMMAND_REMIND_ME = "remindme";
    private static final String COMMAND_CLEAR_REMINDERS = "clearreminders";
    private static final String COMMAND_LIST_REMINDERS = "ListReminders";

    private static final String CMD_LISTREM_INBOUND = "inbound";

    private static final Set<String> COMMANDS = ImmutableSet.of(COMMAND_REMIND, COMMAND_REMIND_ME, COMMAND_CLEAR_REMINDERS, COMMAND_LIST_REMINDERS);
    private static final Set<String> CMD_LISTREM_SUBCOMMANDS = ImmutableSet.of(CMD_LISTREM_INBOUND, "");

    private static final String CONFIRM_STRING = "Yes, I want to delete all reminders";

    private static final String PERM_CLEAR_REMINDERS = "clearreminders";

    private final ReminderListener<T> reminderListener;

    public ReminderCommandHandler(ReminderListener<T> reminderListener) {
        this.reminderListener = reminderListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (COMMAND_LIST_REMINDERS.equals(commands.get(0)) && commands.size() == 1) {
            return CMD_LISTREM_SUBCOMMANDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        if (commands.get(0).equals(COMMAND_CLEAR_REMINDERS)) {
            if (reminderListener.getAcl().hasPermission(event, PERM_CLEAR_REMINDERS)) {
                if (remainder.trim().equalsIgnoreCase(CONFIRM_STRING)) {
                    reminderListener.clearAllReminders();
                    event.respond("Done.  *All* reminders have been purged.");
                } else {
                    event.respond("Error: Clearing all reminders requires confirmation.  To confirm, type \"" +
                        CONFIRM_STRING + "\" (without the quotes) as the argument to the " + COMMAND_CLEAR_REMINDERS +
                        " command.");
                }
                return;
            } else {
                event.respond("No u! (You don't have the necessary permissions to do this.)");
                return;
            }
        }

        if (commands.get(0).equals(COMMAND_LIST_REMINDERS)) {
            handleListReminders(event, commands);
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
                try {
                    TimedReminder tr = new TimedReminder(from, target, message, channel, enteredTime,
                            timedReminderDeliveryTime);
                    reminderListener.getTimedReminders().add(tr);
                    reminderListener.sortTimedReminders();
                    reminderListener.sync();
                    if (reminderListener.getScheduledFuture() != null) {
                        reminderListener.getScheduledFuture().cancel(false);
                    }
                    reminderListener.setScheduledFuture(reminderListener.getSes()
                            .schedule(new TimedReminderProcessor<>(reminderListener), ZonedDateTime.now()
                                    .until(reminderListener.getTimedReminders().get(0).getDeliveryTime(),
                                            ChronoUnit.SECONDS) + 1, TimeUnit.SECONDS));
                    event.respond("Reminder successfully scheduled; will be delivered at or after "
                            + timedReminderDeliveryTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)
                            + " when the target user is online (note: reminders may be delivered by private message).");
                } catch (Exception e) {
                    event.respond("Failed to set reminder: " + e.toString());
                }
            } else {
                if (remainder.isEmpty()) {
                    showNoMessageError(event, commands);
                    return;
                }
                try {
                    Reminder r = new Reminder(from, target, remainder, channel, enteredTime);
                    reminderListener.getReminders().add(r);
                    reminderListener.sync();
                    event.respond(
                            "Reminder successfully recorded; will be delivered as soon as I see the target user talk in or join a channel I'm in (note: reminders may be delivered by private message).");
                } catch (Exception e) {
                    event.respond("Failed to set reminder: " + e.toString());
                }
            }
        }
    }

    private void handleListReminders(GenericMessageEvent<T> event, List<String> commands) {
        String reminderType;
        Predicate<TimedReminder> filterFunc;

        switch (commands.get(1)) {
            case CMD_LISTREM_INBOUND:
                reminderType = "inbound";
                filterFunc = x ->
                    x.target.equalsIgnoreCase(event.getUser().getNick())
                        || x.target.equalsIgnoreCase(event.getUser().getLogin() + "@" + event.getUser().getHostmask());
                break;
            default:
                event.respond(
                    String.format("Please choose one of these list types: %s",
                        CMD_LISTREM_SUBCOMMANDS.stream().collect(Collectors.joining(", "))));
                return;
        }

        // fetch this user's timed reminders
        List<String> timedReminders = reminderListener.getTimedReminders()
            .stream()
            .filter(filterFunc)
            .map(x -> x.getResponseMessagePrefix(false, true)
                .append(": ")
                .append(x.message)
                .toString())
            .collect(Collectors.toList());

        if (timedReminders.size() == 0) {
            event.respond(String.format("I have no %s reminders queued up for you.", reminderType));
        } else {
            event.respond(String.format("I have the following %d %s reminders queued up for you:", timedReminders.size(), reminderType));
            timedReminders.forEach(event::respond);
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

    private void showNoMessageError(GenericMessageEvent<T> event, List<String> commands) {
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
