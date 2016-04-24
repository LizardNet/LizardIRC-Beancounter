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

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.pircbotx.PircBotX;

class FeedChecker<T extends PircBotX> implements Runnable {
    private EarthquakeListener<T> earthquakeListener;
    private Map<Feed, Long> lastReportedEvent = new HashMap<>();
    private Map<Feed, Map<String, GeoJsonFeature>> seenEvents = new HashMap<>();

    public FeedChecker(EarthquakeListener<T> earthquakeListener) {
        this.earthquakeListener = earthquakeListener;
    }

    @Override
    public synchronized void run() {
        // First, determine what feeds need to be checked
        Set<Feed> feedsToCheck = new HashSet<>(earthquakeListener.getFeedMap().values());

        // Loop on those feeds
        for (Feed f : feedsToCheck) {
            GeoJson data;

            try {
                data = earthquakeListener.getAPIData(f.getUrl());
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

                    EarthquakeListener.addOrUpdateEvent(seenEventsThisFeed, event);

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
                        g.coordinates[2], EarthquakeListener.convertDecToRoman(p.reportedIntensity), EarthquakeListener.convertDecToRoman(p.measuredIntensity), p.tsunami == 1 ? "\002\0034YES\017" : "\0033No\017", p.status, p.url);

                    // Send the message to each channel that has this feed enabled
                    earthquakeListener.getFeedMap().entrySet().stream()
                        .filter(i -> i.getValue().equals(f))
                        .forEach(i -> earthquakeListener.getBot().sendIRC().message(i.getKey(), output));
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
                    .forEach(feature -> EarthquakeListener.addOrUpdateEvent(seenEvents.get(f), feature));

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
            if (!earthquakeListener.getFeedMap().containsValue(f)) {
                seenEvents.remove(f);
                itr.remove();
            }
        }
    }
}
