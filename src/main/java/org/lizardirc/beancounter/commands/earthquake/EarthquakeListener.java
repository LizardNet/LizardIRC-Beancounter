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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class EarthquakeListener<T extends PircBotX> extends ListenerAdapter<T> {
    private static final String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

    private final PersistenceManager pm;
    private final AccessControl<T> acl;
    private final ScheduledExecutorService ses;
    private final CommandHandler<T> commandHandler = new EarthquakeCommandHandler<>(this);

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

    synchronized void sync() {
        pm.setMap("feedMap", feedMap.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> Integer.toString(e.getValue().toInt())))
        );
        pm.sync();
    }

    ScheduledFuture scheduleFeedChecker() {
        FeedChecker<T> fc = new FeedChecker<>(this);
        fc.run(); // Force a run of the feed checker immediately
        return ses.scheduleWithFixedDelay(fc, 5L, 5L, TimeUnit.MINUTES);
    }

    GeoJson getAPIData(String s) throws IOException {
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

    static String convertDecToRoman(float f) {
        int i = Math.round(f);
        if (i < 1) {
            return "N/A";
        } else if (i > 12) {
            return "XII+";
        }
        return romans[i - 1];
    }

    Map<String, Feed> getFeedMap() {
        return feedMap;
    }

    PircBotX getBot() {
        return bot;
    }

    AccessControl<T> getAcl() {
        return acl;
    }

    ScheduledFuture getFuture() {
        return future;
    }

    void setFuture(ScheduledFuture future) {
        this.future = future;
    }

    /**
     * Extracts all IDs from the given event (second parameter) and adds or updates all possible id to event mappings
     * in the given map (first parameter).
     *
     * @param eventMap The map of event IDs to events
     * @param event The event to be added or updated
     */
    static void addOrUpdateEvent(Map<String, GeoJsonFeature> eventMap, GeoJsonFeature event) {
        event.getAllIds()
            .forEach(id -> eventMap.put(id.toLowerCase(), event));
    }
}
