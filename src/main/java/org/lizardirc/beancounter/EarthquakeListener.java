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

package org.lizardirc.beancounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class EarthquakeListener<T extends PircBotX> extends ListenerAdapter<T> {
    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final ScheduledExecutorService ses;
    private final CommandHandler<T> commandHandler = new EarthquakeListenerHandler<>();

    private final String httpUserAgent;
    private final Map<String, Feed> feedMap;

    private PircBotX bot;
    private ScheduledFuture future = null;

    public EarthquakeListener(PersistenceManager pm, AccessControl<T> acl, ScheduledExecutorService ses) {
        this.pm = pm;
        this.acl = acl;
        this.ses = ses;

        feedMap = new HashMap<>(pm.getMap("feedMap").entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e -> Feed.fromInt(Integer.parseInt(e.getValue())), (o, n) -> n))
        );

        httpUserAgent = Miscellaneous.generateHttpUserAgent();
    }

    public CommandHandler<T> getCommandHandler() {
        return commandHandler;
    }

    @Override
    public void onConnect(ConnectEvent<T> event) {
        bot = event.getBot();

        if (!feedMap.isEmpty()) {
            future = scheduleFeedChecker();
        }
    }

    private synchronized void sync() {
        pm.setMap("feedMap", feedMap.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> Integer.toString(e.getValue().toInt())))
        );
        pm.sync();
    }

    private ScheduledFuture scheduleFeedChecker() {
        FeedChecker fc = new FeedChecker();
        fc.run(); // Force a run of the feed checker immediately
        return ses.scheduleWithFixedDelay(fc, 5L, 5L, TimeUnit.MINUTES);
    }

    private GeoJson getAPIData(String s) throws IOException {
        URL url = new URL(s);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("User-Agent", httpUserAgent);
        httpURLConnection.connect();

        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error interacting with API: Got return status " + httpURLConnection.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) httpURLConnection.getContent(), "UTF-8"));

        StringBuilder retval = new StringBuilder();
        int value;
        while ((value = reader.read()) != -1) {
            char c = (char)value;
            retval.append(c);
        }

        reader.close();

        Gson gson = new Gson();
        return gson.fromJson(retval.toString(), GeoJson.class);
    }

    private String convertDecToRoman(float f) {
        if (f > 0 && f <= 1) {
            return "I";
        } else if (f > 1 && f <= 2) {
            return "II";
        } else if (f > 2 && f <= 3) {
            return "III";
        } else if (f > 3 && f <= 4) {
            return "IV";
        } else if (f > 4 && f <= 5) {
            return "V";
        } else if (f > 5 && f <= 6) {
            return "VI";
        } else if (f > 6 && f <= 7) {
            return "VII";
        } else if (f > 7 && f <= 8) {
            return "VIII";
        } else if (f > 8 && f <= 9) {
            return "IX";
        } else if (f > 9 && f <= 10) {
            return "X";
        } else if (f > 10 && f <= 11) {
            return "XI";
        } else if (f > 11 && f <= 12) {
            return "XII";
        } else if (f > 12) {
            return "XII+";
        } else {
            return "N/A";
        }
    }

    private class EarthquakeListenerHandler<U extends PircBotX> implements CommandHandler<U> {
        private final String COMMAND_CFGQUAKES = "cfgquakes";
        private final String COMMAND_LASTQUAKE = "lastquake";
        private Set<String> COMMANDS = ImmutableSet.of(COMMAND_CFGQUAKES, COMMAND_LASTQUAKE);

        private final String CFG_OP_GET = "get";
        private final String CFG_OP_GETALL = "getall";
        private final String CFG_OP_SETCHAN = "setchan";
        private final String CFG_OP_DELCHAN = "delchan";
        private Set<String> CFG_OPERATIONS = ImmutableSet.of(CFG_OP_GET, CFG_OP_GETALL, CFG_OP_SETCHAN, CFG_OP_DELCHAN);

        private final String FEED_ALL = "all";
        private final String FEED_M1_0 = "M1.0";
        private final String FEED_M2_5 = "M2.5";
        private final String FEED_M4_5 = "M4.5";
        private final String FEED_SIGNIFICANT = "significant";
        private Set<String> AVAILABLE_FEEDS = ImmutableSet.of(FEED_ALL, FEED_M1_0, FEED_M2_5, FEED_M4_5, FEED_SIGNIFICANT);

        private final String PERM_CFGQUAKES = "cfgquakes";

        @Override
        public Set<String> getSubCommands(GenericMessageEvent<U> event, List<String> commands) {
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
        public synchronized void handleCommand(GenericMessageEvent<U> event, List<String> commands, String remainder) {
            if (commands.size() >= 1) {
                switch (commands.get(0)) {
                    case COMMAND_CFGQUAKES:
                        if (commands.size() >= 2) {
                            if (commands.get(1).equals(CFG_OP_GETALL)) {
                                if (acl.hasPermission(event, PERM_CFGQUAKES)) {
                                    String target = event.getUser().getNick();
                                    OutputIRC outIRC = event.getBot().sendIRC();
                                    Consumer<String> message = s -> outIRC.message(target, s);

                                    if (!(event instanceof PrivateMessageEvent)) {
                                        event.respond("See private message.");
                                    }

                                    message.accept("The following is a list of channels which have earthquake reporting enabled:");
                                    message.accept("Format: channel => feed");
                                    message.accept("----- BEGIN -----");
                                    outputMap(feedMap, message);
                                    message.accept("------ END ------");
                                    message.accept("The FeedChecker job is: " + ((future == null) ? "NOT scheduled" : "scheduled"));
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
                                    Feed channelActiveFeed = feedMap.get(channel.toLowerCase());
                                    if (channelActiveFeed == null) {
                                        event.respond("No earthquake feeds are enabled for channel " + channel + ".");
                                    } else {
                                        event.respond("Earthquake feed \"" + channelActiveFeed + "\" enabled for channel " + channel + ".");
                                    }
                                    break;
                                case CFG_OP_SETCHAN:
                                    if (acl.hasPermission(event, PERM_CFGQUAKES)) {
                                        if (commands.size() == 3) {
                                           switch (commands.get(2)) {
                                                case FEED_ALL:
                                                    feedMap.put(channel.toLowerCase(), Feed.FEED_ALL_EARTHQUAKES);
                                                    break;
                                                case FEED_M1_0:
                                                    feedMap.put(channel.toLowerCase(), Feed.FEED_MAGNITUDE_1_0);
                                                    break;
                                                case FEED_M2_5:
                                                    feedMap.put(channel.toLowerCase(), Feed.FEED_MAGNITUDE_2_5);
                                                    break;
                                                case FEED_M4_5:
                                                    feedMap.put(channel.toLowerCase(), Feed.FEED_MAGNITUDE_4_5);
                                                    break;
                                                case FEED_SIGNIFICANT:
                                                    feedMap.put(channel.toLowerCase(), Feed.FEED_SIGNIFICANT_EARTHQUAKES);
                                                    break;
                                            }

                                            if (future == null) {
                                                future = scheduleFeedChecker();
                                            }

                                            sync();

                                            event.respond("Earthquake feed \"" + feedMap.get(channel.toLowerCase()) + "\" will now be reported to channel " + channel + ".");
                                        } else {
                                            event.respond("Error: Too few arguments.  Syntax: " + COMMAND_CFGQUAKES + " " + CFG_OP_SETCHAN +
                                                " <" + Miscellaneous.getStringRepresentation(AVAILABLE_FEEDS, "|") + "> {[#channel]}");
                                        }
                                    } else {
                                        event.respond("No u! (You don't have the necessary permissions to do this.)");
                                    }
                                    break;
                                case CFG_OP_DELCHAN:
                                    if (acl.hasPermission(event, PERM_CFGQUAKES)) {
                                        if (feedMap.containsKey(channel.toLowerCase())) {
                                            feedMap.remove(channel.toLowerCase());

                                            if (feedMap.isEmpty() && future != null) {
                                                future.cancel(true);
                                                future = null;
                                            }

                                            sync();

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
            for (Entry<String, Feed> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                message.accept(key + " => " + value);
            }
        }

        private void showLastQuake(GenericMessageEvent<U> event, Feed f) {
            GeoJson apiData;

            try {
                apiData = getAPIData(f.getUrl());
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
            output = String.format(output, f.toString(), p.type, p.magType, p.magnitude, p.place, p.getEventTime().format(DateTimeFormatter.RFC_1123_DATE_TIME),g.coordinates[2],
                p.status, convertDecToRoman(p.reportedIntensity), convertDecToRoman(p.measuredIntensity), p.alert == null ? "N/A" : p.alert, p.tsunami == 1 ? "\002\0034YES\017" : "\0033No\017", p.url);
            event.respond(output);
        }
    }

    private enum Feed {
        FEED_ALL_EARTHQUAKES,
        FEED_MAGNITUDE_1_0,
        FEED_MAGNITUDE_2_5,
        FEED_MAGNITUDE_4_5,
        FEED_SIGNIFICANT_EARTHQUAKES;

        private String url;
        private String humanName;

        static {
            FEED_ALL_EARTHQUAKES.url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson";
            FEED_MAGNITUDE_1_0.url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_day.geojson";
            FEED_MAGNITUDE_2_5.url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson";
            FEED_MAGNITUDE_4_5.url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson";
            FEED_SIGNIFICANT_EARTHQUAKES.url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.geojson";

            FEED_ALL_EARTHQUAKES.humanName = "USGS, All Earthquakes";
            FEED_MAGNITUDE_1_0.humanName = "USGS, Earthquakes Magnitude 1.0+";
            FEED_MAGNITUDE_2_5.humanName = "USGS, Earthquakes Magnitude 2.5+";
            FEED_MAGNITUDE_4_5.humanName = "USGS, Earthquakes Magnitude 4.5+";
            FEED_SIGNIFICANT_EARTHQUAKES.humanName = "USGS, Significant Earthquakes";
        }

        public static String getUrl(Feed f) {
            return f.url;
        }

        public String getUrl() {
            return this.url;
        }

        public static String toString(Feed f) {
            return f.humanName;
        }

        @Override
        public String toString() {
            return this.humanName;
        }

        // I'm sure that there's a "nicer" way to implement fromInt() using reflection or some such method.
        //
        // > using reflection
        //
        // Heh.
        //
        // Anyway, let me know if there's a better way to do this.

        public static Feed fromInt(int i) throws IllegalArgumentException {
            switch (i) {
                case 1:
                    return FEED_ALL_EARTHQUAKES;
                case 2:
                    return FEED_MAGNITUDE_1_0;
                case 3:
                    return FEED_MAGNITUDE_2_5;
                case 4:
                    return FEED_MAGNITUDE_4_5;
                case 5:
                    return FEED_SIGNIFICANT_EARTHQUAKES;
                default:
                    throw new IllegalArgumentException("Unrecognized feed magic number.");
            }
        }

        public static int toInt(Feed f) throws IllegalArgumentException {
            if (f == null) {
                throw new IllegalArgumentException("Argument cannot be null.");
            }

            switch (f) {
                case FEED_ALL_EARTHQUAKES:
                    return 1;
                case FEED_MAGNITUDE_1_0:
                    return 2;
                case FEED_MAGNITUDE_2_5:
                    return 3;
                case FEED_MAGNITUDE_4_5:
                    return 4;
                case FEED_SIGNIFICANT_EARTHQUAKES:
                    return 5;
                default:
                    throw new IllegalArgumentException("Unrecognized feed type");
            }
        }

        public int toInt() {
            return toInt(this);
        }
    }

    private class FeedChecker implements Runnable {
        private Map<Feed, Long> lastReportedEvent = new HashMap<>();
        private Map<Feed, Set<String>> seenEventIds = new HashMap<>();

        @Override
        public void run() {
            // First, determine what feeds need to be checked
            Set<Feed> feedsToCheck = new HashSet<>(feedMap.entrySet().stream()
                .map(Entry::getValue)
                .collect(Collectors.toSet())
            );

            // Loop on those feeds
            for (Feed f : feedsToCheck) {
                GeoJson data;

                try {
                    data = getAPIData(f.getUrl());
                } catch (IOException e) {
                    continue; // In case of communication error, silently ignore and continue with the next feed
                }

                // IFF the feed has been previously checked, get a list of events that have update times since the last check
                if (lastReportedEvent.containsKey(f) && lastReportedEvent.get(f) != null) {
                    // Before we begin: Since the feeds only contain events that occurred over the last day, we want
                    // to remove dropped events' eventIds from the seenEventIds multimap to avoid memory leaks.  So,
                    // let's do that now.
                    Set<String> eventIdsInFeed = data.features.stream()
                        .map(i -> i.id)
                        .collect(Collectors.toSet());
                    Iterator<String> itr = seenEventIds.getOrDefault(f, Collections.emptySet()).iterator();
                    while (itr.hasNext()) {
                        String s = itr.next();
                        if (!eventIdsInFeed.contains(s)) {
                            itr.remove();
                        }
                    }

                    // Get a list of events that have been updated since the last check
                    long lastReportedTime = lastReportedEvent.get(f);
                    List<GeoJsonFeature> updatedEvents = new ArrayList<>(data.features.stream()
                        .filter(i -> i.properties.updated >= lastReportedTime)
                        .collect(Collectors.toList())
                    );

                    long lastEventTime = 0;
                    // Iterate over these events
                    for (GeoJsonFeature event : updatedEvents) {
                        if (event.properties.updated > lastEventTime) {
                            lastEventTime = event.properties.updated;
                        }

                        // Unfortunately, it's impossible to determine just by looking at the data whether this is a new event,
                        // or an update for an event that already occurred.  So, we need to keep track of event IDs we've seen
                        // in a multimap.  Consult that multimap now to see if this event is new or an update.  If it's new,
                        // add the ID to the multimap while we're here.

                        String eventType;
                        if (seenEventIds.get(f).contains(event.id)) {
                            eventType = "\0033Updated\017";
                        } else {
                            eventType = "\0037New\017";
                            seenEventIds.get(f).add(event.id);
                        }

                        // Build the message to output to IRC
                        GeoJsonFeatureProperty p = event.properties;
                        GeoJsonFeatureGeometry g = event.geometry;
                        String outputTemplate = "%s event, %s: \002\00310%s %s %.2f %s at %s depth %.2f km\017, DYFI %s, intensity %s, tsunami hazard: %s, status: %s - %s";
                        String output = String.format(outputTemplate, eventType, f, p.type, p.magType, p.magnitude, p.place, p.getEventTime().format(DateTimeFormatter.RFC_1123_DATE_TIME),
                            g.coordinates[2], convertDecToRoman(p.reportedIntensity), convertDecToRoman(p.measuredIntensity), p.tsunami == 1 ? "\002\0034YES\017" : "\0033No\017", p.status, p.url);

                        // Send the message to each channel that has this feed enabled
                        feedMap.entrySet().stream()
                            .filter(i -> i.getValue().equals(f))
                            .forEach(i -> bot.sendIRC().message(i.getKey(), output));
                    }

                    lastReportedEvent.put(f, lastEventTime);
                } else {
                    // We've not previously checked this feed, so we won't report anything this time around.  However,
                    // we will go ahead and collect the IDs currently visible in the feed in the seenEventIds multimap
                    // so we'll know in the future what events are new, and which ones are just updates.
                    seenEventIds.put(f, data.features.stream()
                        .map(i -> i.id)
                        .collect(Collectors.toSet())
                    );

                    // Unfortunately, the top item in the feed is not guaranteed to be the most recently updated, so
                    // we manually have to determine this
                    long lastUpdate = data.features.stream()
                        .mapToLong(e -> e.properties.updated)
                        .max()
                        .getAsLong();
                    lastReportedEvent.put(f, lastUpdate);
                }
            }
        }
    }

    private static class GeoJson {
        public GeoJsonMetadata metadata;
        public List<GeoJsonFeature> features;
    }

    private static class GeoJsonMetadata {
        public long generated;
        public String title;
        public int count;

        public ZonedDateTime getGeneratedTime() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(generated), ZoneId.systemDefault());
        }
    }

    private static class GeoJsonFeature {
        public GeoJsonFeatureProperty properties;
        public GeoJsonFeatureGeometry geometry;
        public String id;
    }

    private static class GeoJsonFeatureProperty {
        @SerializedName("mag")
        public float magnitude;
        public String place;
        public long time;
        public long updated;
        public String url;
        @SerializedName("cdi")
        public float reportedIntensity;
        @SerializedName("mmi")
        public float measuredIntensity;
        public String alert;
        public String status;
        public int tsunami;
        public String magType;
        public String type;

        public ZonedDateTime getEventTime() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        }

        public ZonedDateTime getUpdatedTime() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(updated), ZoneId.systemDefault());
        }
    }

    private static class GeoJsonFeatureGeometry {
        public float[] coordinates;
        // coordinate 0 is longitude, [-180.0, 180.0]
        // coordinate 1 is latitude, [-90.0, 90.0]
        // coordinate 2 is depth, [0, 1000]
    }
}
