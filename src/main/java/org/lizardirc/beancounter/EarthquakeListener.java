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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
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
    private static final String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

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

        /* TODO: A better solution for preventing the multiple-scheduling-on-reconnect problem will be needed when
         * the bot is refactored to be modular.
         */
        if (!feedMap.isEmpty() && future == null) {
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

        Gson gson = new Gson();
        return gson.fromJson(reader, GeoJson.class);
    }

    private String convertDecToRoman(float f) {
        int i = Math.round(f);
        if (i < 1) {
            return "N/A";
        } else if (i > 12) {
            return "XII+";
        }
        return romans[i - 1];
    }

    /**
     * Extracts all IDs from the given event (second parameter) and adds or updates all possible id to event mappings
     * in the given map (first parameter).
     *
     * @param eventMap The map of event IDs to events
     * @param event The event to be added or updated
     */
    private static void addOrUpdateEvent(Map<String, GeoJsonFeature> eventMap, GeoJsonFeature event) {
        event.getAllIds()
            .forEach(id -> eventMap.put(id.toLowerCase(), event));
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
        FEED_ALL_EARTHQUAKES(1, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson", "USGS, All Earthquakes"),
        FEED_MAGNITUDE_1_0(2, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_day.geojson", "USGS, Earthquakes Magnitude 1.0+"),
        FEED_MAGNITUDE_2_5(3, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson", "USGS, Earthquakes Magnitude 2.5+"),
        FEED_MAGNITUDE_4_5(4, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson", "USGS, Earthquakes Magnitude 4.5+"),
        FEED_SIGNIFICANT_EARTHQUAKES(5, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.geojson", "USGS, Significant Earthquakes");

        private final int intValue;
        private final String url;
        private final String humanName;
        private static final Map<Integer, Feed> intMap = new HashMap<>();

        static {
            for (Feed value : Feed.values()) {
                intMap.put(value.intValue, value);
            }
        }

        Feed(int intValue, String url, String humanName) {
            this.intValue = intValue;
            this.url = url;
            this.humanName = humanName;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return humanName;
        }

        public static Feed fromInt(int i) throws IllegalArgumentException {
            if (intMap.containsKey(i)) {
                return intMap.get(i);
            } else {
                throw new IllegalArgumentException("Invalid feed integer value");
            }
        }

        public int toInt() {
            return intValue;
        }
    }

    private class FeedChecker implements Runnable {
        private Map<Feed, Long> lastReportedEvent = new HashMap<>();
        private Map<Feed, Map<String, GeoJsonFeature>> seenEvents = new HashMap<>();

        @Override
        public synchronized void run() {
            // First, determine what feeds need to be checked
            Set<Feed> feedsToCheck = new HashSet<>(feedMap.values());

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
                    Map<String, GeoJsonFeature> seenEventsThisFeed = seenEvents.get(f);

                    // Before we begin: Since the feeds only contain events that occurred over the last day, we want
                    // to remove dropped events from the seenEvents multimap to avoid memory leaks.  So, let's do that
                    // now.
                    Set<String> eventIdsInFeed = data.features.stream()
                        .flatMap(GeoJsonFeature::getAllIds)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                    Iterator<String> itr = seenEvents.getOrDefault(f, Collections.emptyMap()).keySet().iterator();
                    while (itr.hasNext()) {
                        String s = itr.next().toLowerCase();
                        if (!eventIdsInFeed.contains(s)) {
                            itr.remove();
                        }
                    }

                    // Get a list of events that have been updated since the last check
                    long lastReportedTime = lastReportedEvent.get(f);
                    List<GeoJsonFeature> updatedEvents = new ArrayList<>(data.features.stream()
                        .filter(i -> i.properties.updated > lastReportedTime)
                        .collect(Collectors.toList())
                    );

                    long lastEventTime = -1L;
                    // Iterate over these events
                    for (GeoJsonFeature event : updatedEvents) {
                        if (event.properties.updated > lastEventTime) {
                            lastEventTime = event.properties.updated;
                        }

                        // Unfortunately, it's impossible to determine just by looking at the data whether this is a new event,
                        // or an update for an event that already occurred.  So, we need to keep track of event IDs we've seen
                        // in a multimap.  Consult that multimap now to see if this event is new or an update.  If it
                        // has been seen before, compare it to the stored event, and only report it if it has been changed.
                        // For both updated events and new events, update the multimap with the new event.

                        String eventType;

                        Optional<GeoJsonFeature> oldEvent = event.getAllIds()
                            .filter(seenEventsThisFeed::containsKey)
                            .map(seenEventsThisFeed::get)
                            .findAny();

                        if (oldEvent.isPresent()) {
                            if (event.equals(oldEvent.get())) {
                                continue; // This event, though it has been marked as updated, contains the same reported data.  Continue.
                            }
                            eventType = "\0033Updated\017";
                        } else {
                            eventType = "\0037New\017";
                        }

                        addOrUpdateEvent(seenEventsThisFeed, event);

                        // Build the message to output to IRC
                        // IMPORTANT: If you change the reported data here, remember to also change GeoJsonFeature.hashCode()
                        // and GeoJsonFeature.equals() as appropriate!  Those two methods should only consider fields that
                        // are reported to IRC here (plus the GeoJsonFeature.id field).  Note that equals() and hashCode()
                        // are written with the assumption that below we only care about the magnitude and depth to two
                        // digits of precision after the decimal, that we don't care about the milliseconds of the event
                        // time, and that we will only be using the rounded (which convertDecToRoman() handles) values
                        // of reported and measured intensity - so remember to update equals() and hashCode() if any of
                        // these assumptions change!
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

                    if (lastEventTime >= 0) {
                        lastReportedEvent.put(f, lastEventTime);
                    }
                } else {
                    // We've not previously checked this feed, so we won't report anything this time around.  However,
                    // we will go ahead and collect the IDs currently visible in the feed in the seenEvents multimap
                    // so we'll know in the future what events are new, and which ones are just updates.  Also set up
                    // the multimap here.
                    seenEvents.put(f, new HashMap<>());
                    data.features.stream()
                        .forEach(feature -> addOrUpdateEvent(seenEvents.get(f), feature));

                    // Unfortunately, the top item in the feed is not guaranteed to be the most recently updated, so
                    // we manually have to determine this
                    long lastUpdate = data.features.stream()
                        .mapToLong(e -> e.properties.updated)
                        .max()
                        .orElseGet(() -> 0L);
                    lastReportedEvent.put(f, lastUpdate);
                }
            }

            // Finally, forget data for feeds we're no longer checking.
            Iterator<Feed> itr = lastReportedEvent.keySet().iterator();
            while (itr.hasNext()) {
                Feed f = itr.next();
                if (!feedMap.containsValue(f)) {
                    seenEvents.remove(f);
                    itr.remove();
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

        /**
         * Returns all IDs that identify this GeoJsonFeature, making no distinction between primary and alternate IDs.
         *
         * @return A stream of ID strings that identify this GeoJsonFeature
         */
        public Stream<String> getAllIds() {
            return Stream.concat(properties.getAlternateIds(), Stream.of(id))
                .distinct();
        }

        // hashCode() and equals() below should only check fields that we report to IRC in FeedChecker.run(), AND the id field.

        @Override
        public int hashCode() {
            // Note that properties.magnitude and geometry.coordinates[2], despite being used for equality comparison in equals(),
            // are deliberately not used here in hashCode() because hash codes are explicitly allowed to collide, and I don't
            // want to deal with floating point arithmetic nonsense here.  In any case, since we expect id to be unique to
            // every event, that should ensure that hash codes are unique (enough) between events.
            // properties.eventTime is divided by 1000 here because we don't care about microseconds.
            return Objects.hash(id.toLowerCase(), properties.type, properties.magType, properties.place, properties.eventTime / 1000L,
                Math.round(properties.reportedIntensity), Math.round(properties.measuredIntensity), properties.tsunami, properties.status.toLowerCase(),
                properties.url.toLowerCase());
        }

        public boolean canEqual(Object other) {
            return other instanceof GeoJsonFeature;
        }

        @Override
        public boolean equals(Object other) {
            boolean result = false;
            if (other instanceof GeoJsonFeature) {
                GeoJsonFeature that = (GeoJsonFeature) other;
                result = that.canEqual(this)
                    && this.id.equalsIgnoreCase(that.id)
                    && this.properties.type.equals(that.properties.type)
                    && this.properties.magType.equals(that.properties.magType)
                    && Math.abs(this.properties.magnitude - that.properties.magnitude) < 0.01F // We only care about two decimals of precision
                    && this.properties.place.equals(that.properties.place)
                    && Math.abs(this.properties.eventTime - that.properties.eventTime) < 1000L // We don't care about the microseconds here
                    && Math.abs(this.geometry.coordinates[2] - that.geometry.coordinates[2]) < 0.01F // We only care about two decimals of precision
                    && Math.round(this.properties.reportedIntensity) == Math.round(that.properties.reportedIntensity) // We only care about the rounded value
                    && Math.round(this.properties.measuredIntensity) == Math.round(that.properties.measuredIntensity) // We only care about the rounded value
                    && this.properties.tsunami == that.properties.tsunami
                    && this.properties.status.equalsIgnoreCase(that.properties.status)
                    && this.properties.url.equalsIgnoreCase(that.properties.url);
            }

            return result;
        }
    }

    private static class GeoJsonFeatureProperty {
        @SerializedName("mag")
        public float magnitude;
        public String place;
        @SerializedName("time")
        public long eventTime;
        public long updated;
        public String url;
        @SerializedName("cdi")
        public float reportedIntensity;
        @SerializedName("mmi")
        public float measuredIntensity;
        public String alert;
        public String status;
        public int tsunami;
        private String ids;
        public String magType;
        public String type;

        public ZonedDateTime getEventTime() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTime), ZoneId.systemDefault());
        }

        public ZonedDateTime getUpdatedTime() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(updated), ZoneId.systemDefault());
        }

        public Stream<String> getAlternateIds() {
            return Stream.of(StringUtils.strip(ids, ", ").split(","))
                .distinct();
        }
    }

    private static class GeoJsonFeatureGeometry {
        public float[] coordinates;
        // coordinate 0 is longitude, [-180.0, 180.0]
        // coordinate 1 is latitude, [-90.0, 90.0]
        // coordinate 2 is depth, [0, 1000]
    }
}
