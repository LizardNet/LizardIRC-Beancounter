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

package org.lizardirc.beancounter.commands.airport;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

public class AirportHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_AIRPORT = "airport";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_AIRPORT);

    private final DataHandler dataHandler;

    private long lastRefreshEpoch = 0L;

    public AirportHandler() {
        dataHandler = new DataHandler();
        dataHandler.acquireData();
        lastRefreshEpoch = Instant.now().getEpochSecond();
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent event, List commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent event, List commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        if (commands.get(0).equals(CMD_AIRPORT)) {
            remainder = remainder.trim().toLowerCase();

            if (remainder.isEmpty()) {
                event.respond("You have to provide an airport to lookup.  Syntax: " + CMD_AIRPORT + " [airport]; " +
                    "[airport] can be a (partial) name, IATA (3-letter) code, or ICAO (3- or 4-letter/number) code.");
            } else {
                // If the data is older than 24 hours, reacquire
                if (Instant.now().getEpochSecond() - lastRefreshEpoch > (24 * 60 * 60)) {
                    dataHandler.acquireData();
                    lastRefreshEpoch = Instant.now().getEpochSecond();
                }

                List<Airport> output;

                try {
                    output = dataHandler.getByIataCode(remainder);
                    if (output == null) {
                        output = dataHandler.getByIcaoCode(remainder);

                        if (output == null) {
                            output = dataHandler.searchByName(remainder);

                            if (output == null) {
                                event.respond(Bool.class.getCanonicalName() + '.' + Bool.AIRPORT_NOT_FOUND.name());
                                return;
                            }
                        }
                    }

                    outputAirportInformation(event, output);
                } catch (DataNotReadyException e) {
                    event.respond("The bot is busy acquiring airport data; please try again later");
                } catch (Exception e) {
                    event.respond("The bot failed to acquire airport data; please try again later (" + e.toString() + ')');
                    dataHandler.acquireData();
                    lastRefreshEpoch = Instant.now().getEpochSecond();
                }
            }
        }
    }

    public static int ftToM(int input) {
        Float lengthInM = input * 0.3048f;
        return Math.round(lengthInM);
    }

    private static void outputAirportInformation(GenericMessageEvent<?> event, List<Airport> airports) {
        if (airports.isEmpty()) {
            throw new IllegalArgumentException("airports cannot be an empty list");
        } else if (airports.size() == 1) {
            event.respond(airports.get(0).toString());
            event.respond("Runways/landing pads: " + airports.get(0).runwaysToString());
        } else if (airports.size() <= 15) {
            StringBuilder retval = new StringBuilder(Integer.toString(airports.size()))
                .append(" matches; specify one for more details: ");

            Iterator<Airport> iter = airports.iterator();
            int i = 0;
            while (iter.hasNext()) {
                // Only do five results per line
                i++;
                Airport airport = iter.next();

                retval.append(airport.toShortString());

                if (iter.hasNext()) {
                    if (i >= 5) {
                        event.respond(retval.append(" ...").toString());
                        retval = new StringBuilder();
                        i = 0;
                    } else {
                        retval.append("; ");
                    }
                }
            }

            event.respond(retval.toString());
        } else {
            event.respond(Integer.toString(airports.size()) + " airports matched your query; please be more specific.");
        }
    }
}
