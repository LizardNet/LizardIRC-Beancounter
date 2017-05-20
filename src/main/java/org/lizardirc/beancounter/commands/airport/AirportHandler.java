package org.lizardirc.beancounter.commands.airport;

import java.time.Instant;
import java.util.Collections;
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

                String output;

                try {
                    output = dataHandler.getByIataCode(remainder);
                    if (output == null) {
                        output = dataHandler.getByIcaoCode(remainder);

                        if (output == null) {
                            output = dataHandler.searchByName(remainder);

                            if (output == null) {
                                output = Bool.class.getCanonicalName() + '.' + Bool.AIRPORT_NOT_FOUND.name();
                            }
                        }
                    }
                } catch (DataNotReadyException e) {
                    output = "The bot is busy acquiring airport data; please try again later";
                } catch (Exception e) {
                    output = "The bot failed to acquire airport data; please try again later (" + e.toString() + ')';
                    dataHandler.acquireData();
                    lastRefreshEpoch = Instant.now().getEpochSecond();
                }

                event.respond(output);
            }
        }
    }
}
