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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;

import org.lizardirc.beancounter.commands.elite.eddn.schema.journal.EventType;
import org.lizardirc.beancounter.commands.elite.eddn.schema.journal.JournalEntry;

public class EddnHandler<T extends PircBotX> extends ListenerAdapter<T> {
    public static final String ZEROMQ_ENDPOINT = "tcp://eddn-relay.elite-markets.net:9500";

    private final ScheduledExecutorService ses;
    private final Parser parser = new Parser();
    private final Map<String, Long> cooldownMap = new HashMap<>();

    private T bot;

    public EddnHandler(ScheduledExecutorService ses) {
        this.ses = ses;
    }

    @Override
    public void onConnect(ConnectEvent<T> event) {
        bot = event.getBot();

        Thread t = new Thread(new EddnStreamer(this));
        t.setDaemon(true);
        t.start();

        System.out.println("Started EDDN monitor thread");
    }

    void signalError(Exception e) {
        // ?idklol
        System.err.println("ERROR reading EDDN message: " + e.toString());
    }

    void handleMessage(String message) {
        // We don't want to parse to JSON if this isn't a message type we're interested in, so check the raw JSON for the
        // schema URL we expect first
        if (message.toLowerCase().contains("http://schemas.elite-markets.net/eddn/journal/1")) {
            try {
                JournalEntry journalEntry = parser.parseJson(message, JournalEntry.class);

                if ((EventType.DOCKED.equals(journalEntry.getMessage(parser).getEventType())
                    || EventType.FSD_JUMP.equals(journalEntry.getMessage(parser).getEventType())
                    ) && !cooldownCheck(journalEntry.getHeader().getUploaderId())) {
                    bot.sendIRC().message("#botspam", JournalEntry.asString(parser, journalEntry));
                }
            } catch (SchemaMismatchException e) {
                // ?idklol
                System.err.println("ERROR parsing EDDN message: " + e.toString());
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
        final long now = Instant.now().getEpochSecond();

        if (cooldownMap.containsKey(cmdr)) {
            if (now - cooldownMap.get(cmdr) < 15L) {
                return true;
            }
        }

        cooldownMap.put(cmdr, now);
        return false;
    }
}
