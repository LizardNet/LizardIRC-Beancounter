/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.elite.eddn;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;

import org.lizardirc.beancounter.commands.elite.Configuration;
import org.lizardirc.beancounter.commands.elite.eddn.schema.journal.EventType;
import org.lizardirc.beancounter.commands.elite.eddn.schema.journal.JournalEntry;

public class EddnHandler<T extends PircBotX> extends ListenerAdapter<T> {
    public static final String ZEROMQ_ENDPOINT = "tcp://eddn.edcd.io:9500";

    private final ScheduledExecutorService ses;
    private final Parser parser = new Parser();
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();

    private ScheduledFuture cooldownMapCleanupTask = null;
    private T bot;
    private Configuration config = null;
    private Thread eddnMonitorThread = null;
    private EddnMonitor eddnMonitor = null;

    public EddnHandler(ScheduledExecutorService ses) {
        this.ses = ses;
    }

    @Override
    public void onConnect(ConnectEvent<T> event) {
        bot = event.getBot();

        startEddnMonitorThread();

        if (cooldownMapCleanupTask == null) {
            cooldownMapCleanupTask = ses.scheduleWithFixedDelay(() -> {
                final long now = Instant.now().getEpochSecond();

                // For thread safety, don't directly iterate over the cooldownMap - instead, collect all the expired
                // entries into a set and loop over those
                cooldownMap.entrySet().stream()
                    .filter(e -> (now - e.getValue()) >= 7200L) // All entries two or more hours old
                    .map(Map.Entry::getKey)
                    .forEach(cooldownMap::remove);
            }, 1L, 1L, TimeUnit.HOURS);
        }
    }

    private void startEddnMonitorThread() {
        eddnMonitor = new EddnMonitor(this);
        eddnMonitorThread = new Thread(eddnMonitor);
        eddnMonitorThread.setDaemon(true);
        eddnMonitorThread.start();

        System.err.println("elite.eddn.EddnHandler: Started EDDN monitor thread");
    }

    void signalError(Exception e) {
        // Print to stderr and stop the thread if it isn't already; restart the monitor in one minute
        System.err.println("elite.eddn.EddnHandler: ERROR reading EDDN message: " + e.toString());
        eddnMonitor.interrupt();
        eddnMonitorThread.interrupt();
        eddnMonitor = null;
        eddnMonitorThread = null;

        ses.schedule(this::startEddnMonitorThread, 1L, TimeUnit.MINUTES);
        System.err.println("elite.eddn.EddnHandler: EDDN monitor thread stopped and restart tasked scheduled for one minute from now");
    }

    void handleMessage(String message) {
        if (config == null) {
            // We haven't initialized yet; don't do anything
            return;
        }

        // We don't want to parse to JSON if this isn't a message type we're interested in, so check the raw JSON for the
        // schema URL we expect first
        if (message.toLowerCase().contains(JournalEntry.SCHEMA_ID)) {
            try {
                JournalEntry journalEntry = parser.parseJson(message, JournalEntry.class);
                String cmdr = journalEntry.getHeader().getUploaderId();

                if (cooldownCheck(cmdr)) {
                    return;
                }

                if ((EventType.DOCKED.equals(journalEntry.getMessage(parser).getEventType())
                    || EventType.FSD_JUMP.equals(journalEntry.getMessage(parser).getEventType()))) {
                    config.getTrackedCommandersMap().entrySet().stream()
                        .filter(e -> config.isChannelEnabled(e.getKey()))
                        .filter(e -> e.getValue().contains(cmdr.toLowerCase()))
                        .forEach(e -> bot.sendIRC().message(e.getKey(), JournalEntry.asString(parser, journalEntry)));
                }

                if (EventType.DOCKED.equals(journalEntry.getMessage(parser).getEventType())
                    && journalEntry.getMessageAsDockedEvent(parser).getStationName().equalsIgnoreCase("Hutton Orbital")) {
                    config.getChannelsTrackingHutton().stream()
                        .filter(c -> config.isChannelEnabled(c))
                        .filter(c -> !config.getTrackedCommandersMap().get(c).contains(cmdr.toLowerCase()))
                        .forEach(c -> bot.sendIRC().message(c, JournalEntry.asString(parser, journalEntry)));
                }
            } catch (SchemaMismatchException e) {
                // ?idklol
                System.err.println("elite.eddn.EddnHandler: ERROR parsing EDDN message: " + e.toString());
            }
        }
    }

    /**
     * Checks if we've seen an event from this Commander within the last 15 seconds; if so, ignore it - it's probably
     * either erroneous or a duplicate.
     *
     * @param cmdr The player's name, without the CMDR prefix
     * @return {@code true} if the name is in cooldown and the message should not be relayed, {@code false} otherwise
     */
    private boolean cooldownCheck(String cmdr) {
        cmdr = cmdr.toLowerCase();
        final long now = Instant.now().getEpochSecond();

        if (cooldownMap.containsKey(cmdr)) {
            if (now - cooldownMap.get(cmdr) < 15L) {
                return true;
            }
        }

        cooldownMap.put(cmdr, now);
        return false;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }
}
