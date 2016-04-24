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

package org.lizardirc.beancounter.commands.earthquake;

import com.google.common.collect.ImmutableSet;
import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.utils.Miscellaneous;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

class EarthquakeCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private final EarthquakeListener<T> earthquakeListener;
    private final String COMMAND_CFGQUAKES = "cfgquakes";
    private final String COMMAND_LASTQUAKE = "lastquake";
    private final Set<String> COMMANDS = ImmutableSet.of(COMMAND_CFGQUAKES, COMMAND_LASTQUAKE);

    private final String CFG_OP_GET = "get";
    private final String CFG_OP_GETALL = "getall";
    private final String CFG_OP_SETCHAN = "setchan";
    private final String CFG_OP_DELCHAN = "delchan";
    private final Set<String> CFG_OPERATIONS = ImmutableSet.of(CFG_OP_GET, CFG_OP_GETALL, CFG_OP_SETCHAN, CFG_OP_DELCHAN);

    private final String FEED_ALL = "all";
    private final String FEED_M1_0 = "M1.0";
    private final String FEED_M2_5 = "M2.5";
    private final String FEED_M4_5 = "M4.5";
    private final String FEED_SIGNIFICANT = "significant";
    private final Set<String> AVAILABLE_FEEDS = ImmutableSet.of(FEED_ALL, FEED_M1_0, FEED_M2_5, FEED_M4_5, FEED_SIGNIFICANT);

    private final String PERM_CFGQUAKES = "cfgquakes";

    public EarthquakeCommandHandler(EarthquakeListener<T> earthquakeListener) {
        this.earthquakeListener = earthquakeListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1) {
            if (commands.get(0).equals(COMMAND_CFGQUAKES)) {
                return CFG_OPERATIONS;
            } else if (commands.get(0).equals(COMMAND_LASTQUAKE)) {
                return AVAILABLE_FEEDS;
            }
        }

        if (commands.size() == 2) {
            if (commands.get(0).equals(COMMAND_CFGQUAKES) && commands.get(1).equals(CFG_OP_SETCHAN)) {
                return AVAILABLE_FEEDS;
            }
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() >= 1) {
            switch (commands.get(0)) {
                case COMMAND_CFGQUAKES:
                    if (commands.size() >= 2) {
                        if (commands.get(1).equals(CFG_OP_GETALL)) {
                            if (earthquakeListener.getAcl().hasPermission(event, PERM_CFGQUAKES)) {
                                String target = event.getUser().getNick();
                                OutputIRC outIRC = event.getBot().sendIRC();
                                Consumer<String> message = s -> outIRC.message(target, s);

                                if (!(event instanceof PrivateMessageEvent)) {
                                    event.respond("See private message.");
                                }

                                message.accept("The following is a list of channels which have earthquake reporting enabled:");
                                message.accept("Format: channel => feed");
                                message.accept("----- BEGIN -----");
                                outputMap(earthquakeListener.getFeedMap(), message);
                                message.accept("------ END ------");
                                message.accept("The FeedChecker job is: " + ((earthquakeListener.getFuture() == null) ? "NOT scheduled" : "scheduled"));
                                return;
                            } else {
                                event.respond("No u! (You don't have the necessary permissions to do this.)");
                                return;
                            }
                        }

                        remainder = remainder.trim();
                        String[] args = remainder.split(" ");
                        String channel;

                        if (remainder.isEmpty()) {
                            if (event instanceof GenericChannelEvent) {
                                channel = ((GenericChannelEvent) event).getChannel().getName();
                            } else {
                                event.respond("Error: This command requires the channel argument if it is not given in a channel.");
                                if (commands.get(1).equals(CFG_OP_SETCHAN)) {
                                    event.respond("Syntax: " + COMMAND_CFGQUAKES + " " + CFG_OP_SETCHAN +
                                        " <" + Miscellaneous.getStringRepresentation(AVAILABLE_FEEDS, "|") + "> {[#channel]}");
                                } else {
                                    event.respond("Syntax: " + COMMAND_CFGQUAKES + " " + commands.get(1) + " {[#channel]}");
                                }
                                return;
                            }
                        } else {
                            channel = args[0];
                            if (!Miscellaneous.isChannelLike(event, channel)) {
                                event.respond("Error: \"" + channel + "\" is not a valid channel name on this network.");
                                return;
                            }
                        }

                        switch (commands.get(1)) {
                            case CFG_OP_GET:
                                Feed channelActiveFeed = earthquakeListener.getFeedMap().get(channel.toLowerCase());
                                if (channelActiveFeed == null) {
                                    event.respond("No earthquake feeds are enabled for channel " + channel + ".");
                                } else {
                                    event.respond("Earthquake feed \"" + channelActiveFeed + "\" enabled for channel " + channel + ".");
                                }
                                break;
                            case CFG_OP_SETCHAN:
                                if (earthquakeListener.getAcl().hasPermission(event, PERM_CFGQUAKES)) {
                                    if (commands.size() == 3) {
                                        switch (commands.get(2)) {
                                            case FEED_ALL:
                                                earthquakeListener.getFeedMap().put(channel.toLowerCase(), Feed.FEED_ALL_EARTHQUAKES);
                                                break;
                                            case FEED_M1_0:
                                                earthquakeListener.getFeedMap().put(channel.toLowerCase(), Feed.FEED_MAGNITUDE_1_0);
                                                break;
                                            case FEED_M2_5:
                                                earthquakeListener.getFeedMap().put(channel.toLowerCase(), Feed.FEED_MAGNITUDE_2_5);
                                                break;
                                            case FEED_M4_5:
                                                earthquakeListener.getFeedMap().put(channel.toLowerCase(), Feed.FEED_MAGNITUDE_4_5);
                                                break;
                                            case FEED_SIGNIFICANT:
                                                earthquakeListener.getFeedMap().put(channel.toLowerCase(), Feed.FEED_SIGNIFICANT_EARTHQUAKES);
                                                break;
                                        }

                                        if (earthquakeListener.getFuture() == null) {
                                            earthquakeListener.setFuture(earthquakeListener.scheduleFeedChecker());
                                        }

                                        earthquakeListener.sync();

                                        event.respond("Earthquake feed \"" + earthquakeListener.getFeedMap().get(channel.toLowerCase()) + "\" will now be reported to channel " + channel + ".");
                                    } else {
                                        event.respond("Error: Too few arguments.  Syntax: " + COMMAND_CFGQUAKES + " " + CFG_OP_SETCHAN +
                                            " <" + Miscellaneous.getStringRepresentation(AVAILABLE_FEEDS, "|") + "> {[#channel]}");
                                    }
                                } else {
                                    event.respond("No u! (You don't have the necessary permissions to do this.)");
                                }
                                break;
                            case CFG_OP_DELCHAN:
                                if (earthquakeListener.getAcl().hasPermission(event, PERM_CFGQUAKES)) {
                                    if (earthquakeListener.getFeedMap().containsKey(channel.toLowerCase())) {
                                        earthquakeListener.getFeedMap().remove(channel.toLowerCase());

                                        if (earthquakeListener.getFeedMap().isEmpty() && earthquakeListener.getFuture() != null) {
                                            earthquakeListener.getFuture().cancel(true);
                                            earthquakeListener.setFuture(null);
                                        }

                                        earthquakeListener.sync();

                                        event.respond("Earthquake feed disabled for channel " + channel + ".");
                                    } else {
                                        event.respond("Error: Earthquake feeds were not enabled for channel " + channel + " to begin with.");
                                    }
                                } else {
                                    event.respond("No u! (You don't have the necessary permissions to do this.)");
                                }
                                break;
                        }
                    } else {
                        event.respond("Error: Too few or invalid arguments.  Syntax: " + COMMAND_CFGQUAKES +
                            " <" + Miscellaneous.getStringRepresentation(CFG_OPERATIONS, "|") + ">");
                        return;
                    }
                    break;
                case COMMAND_LASTQUAKE:
                    if (commands.size() == 2) {
                        switch (commands.get(1)) {
                            case FEED_ALL:
                                showLastQuake(event, Feed.FEED_ALL_EARTHQUAKES);
                                break;
                            case FEED_M1_0:
                                showLastQuake(event, Feed.FEED_MAGNITUDE_1_0);
                                break;
                            case FEED_M2_5:
                                showLastQuake(event, Feed.FEED_MAGNITUDE_2_5);
                                break;
                            case FEED_M4_5:
                                showLastQuake(event, Feed.FEED_MAGNITUDE_4_5);
                                break;
                            case FEED_SIGNIFICANT:
                                showLastQuake(event, Feed.FEED_SIGNIFICANT_EARTHQUAKES);
                                break;
                        }
                    } else {
                        event.respond("Error: Too few or invalid arguments.  You must specify the feed to check.");
                        event.respond("Syntax: " + COMMAND_LASTQUAKE + " <" + Miscellaneous.getStringRepresentation(AVAILABLE_FEEDS, "|") +
                            ">");
                    }
                    break;
            }
        }
    }

    private synchronized void outputMap(Map<String, Feed> map, Consumer<String> message) {
        for (Map.Entry<String, Feed> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();

            message.accept(key + " => " + value);
        }
    }

    private void showLastQuake(GenericMessageEvent<T> event, Feed f) {
        GeoJson apiData;

        try {
            apiData = earthquakeListener.getAPIData(f.getUrl());
        } catch (IOException e) {
            event.respond("Error accessing API: " + e);
            return;
        }

        if (apiData.metadata.count < 1) {
            event.respond("Feed " + apiData.metadata.title + " has no earthquakes to report.");
            return;
        }

        GeoJsonFeatureProperty p = apiData.features.get(0).properties;
        GeoJsonFeatureGeometry g = apiData.features.get(0).geometry;
        String output = "Latest event from feed %s: \002\00310%s, %s %.2f %s at %s depth %.2f km\017 - Status: %s, DYFI: %s, Intensity: %s, PAGER: %s, tsunami hazard: %s - %s";
        output = String.format(output, f.toString(), p.type, p.magType, p.magnitude, p.place, p.getEventTime().format(DateTimeFormatter.RFC_1123_DATE_TIME), g.coordinates[2],
            p.status, EarthquakeListener.convertDecToRoman(p.reportedIntensity), EarthquakeListener.convertDecToRoman(p.measuredIntensity), p.alert == null ? "N/A" : p.alert, p.tsunami == 1 ? "\002\0034YES\017" : "\0033No\017", p.url);
        event.respond(output);
    }
}
