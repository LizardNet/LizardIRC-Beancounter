/*
 * LIZARDIRC/BEANCOUNTER
 * By the LizardIRC Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2015-2020 by the LizardIRC Development Team. Some rights reserved.
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

package org.lizardirc.beancounter.commands.weather;

// The number of imports is too damn high!

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import org.pircbotx.User;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class WeatherHandler implements CommandHandler {
    private static final String COMMAND_WEATHER = "weather";
    private static final String COMMAND_USER_WEATHER = "userweather";
    private static final String COMMAND_WEATHER_CONFIG = "cfgweather";
    private static final String COMMAND_SET_LOCATION = "setlocation";
    private static final Set<String> COMMANDS = ImmutableSet.of(COMMAND_WEATHER, COMMAND_USER_WEATHER, COMMAND_WEATHER_CONFIG, COMMAND_SET_LOCATION);

    private static final String CFG_OP_SHOW_CFG = "show";
    private static final String CFG_OP_SET_API_KEY = "apisetkey";
    private static final String CFG_OP_SET_API_DAILY_CAP = "apisetdayrate";
    private static final String CFG_OP_SET_API_MINUTE_CAP = "apisetminuterate";
    private static final String CFG_OP_SET_API_BURST_SIZE = "apisetburst";
    private static final String CFG_OP_ENABLE = "enable";
    private static final String CFG_OP_DISABLE = "disable";
    private static final String CFG_OP_ALERTS_ENABLE = "alertsenable";
    private static final String CFG_OP_ALERTS_DISABLE = "alertsdisable";
    private static final Set<String> CFG_OPERATIONS = ImmutableSet.of(CFG_OP_SHOW_CFG, CFG_OP_SET_API_KEY,
        CFG_OP_SET_API_DAILY_CAP, CFG_OP_SET_API_MINUTE_CAP, CFG_OP_SET_API_BURST_SIZE, CFG_OP_ENABLE, CFG_OP_DISABLE,
        CFG_OP_ALERTS_ENABLE, CFG_OP_ALERTS_DISABLE);

    private static final String PERM_CFGWEATHER = "cfgweather";

    // Autocomplete API URL is easy - just append your query to the end
    private static final String WUNDERGROUND_AUTOCOMPLETE_ENDPOINT = "http://autocomplete.wunderground.com/aq?format=JSON&h=0&query=";
    // The actual Weather API is a bit more complex - we need to insert the API key, query type, and location into the appropriate places
    // Index 1 is the API key, index 2 is the query type, index 3 is the location - which as of this writing includes a leading slash.
    private static final String WUNDERGROUND_API_ENDPOINT = "http://api.wunderground.com/api/%1$s/%2$s%3$s.json";

    private final String httpUserAgent;
    private final PersistenceManager pm;
    private final AccessControl acl;
    private final Map<String, String> defaultLocations;
    private final WeatherApiRateLimiter rateLimiter;

    private boolean isEnabled;
    private String apiKey;
    private boolean enableAlerts;

    public WeatherHandler(PersistenceManager pm, AccessControl acl) {
        this.pm = pm;
        this.acl = acl;

        defaultLocations = new HashMap<>(pm.getMap("defaultLocations").entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Entry::getValue, (o, n) -> n)) // In case of collision, arbitrarily discard old value
        );

        apiKey = pm.get("apiKey").orElse(null);
        enableAlerts = pm.getBoolean("enableAlerts").orElse(false);

        httpUserAgent = Miscellaneous.generateHttpUserAgent();

        rateLimiter = new WeatherApiRateLimiter(pm);

        if (apiKey != null && rateLimiter.getApiCallsPerDay() > 0 && rateLimiter.getApiCallsPerMinute() > 0) {
            isEnabled = pm.getBoolean("enabled").orElse(false);
        } else {
            System.err.println("WARNING: WeatherHandler not configured - API key and rate limit must be set.  Refusing to enable.");
            System.err.println("Perform the necessary configuration from IRC, then use the \"" + COMMAND_WEATHER_CONFIG + ' ' +
                CFG_OP_ENABLE + "\" command to enable the \"" + COMMAND_WEATHER + "\" command.");
            isEnabled = false;
        }
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(COMMAND_WEATHER_CONFIG)) {
            return CFG_OPERATIONS;
        }

        if (commands.size() == 1 && commands.get(0).equals(COMMAND_USER_WEATHER)) {
            if (!(event instanceof GenericChannelEvent)) {
                return Collections.emptySet();
            }

            GenericChannelEvent gce = (GenericChannelEvent) event;
            return gce.getChannel().getUsers().stream()
                .map(User::getNick)
                .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent event, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        remainder = remainder.trim();
        String[] args = remainder.split(" ");

        switch (commands.get(0)) {
            case COMMAND_WEATHER:
                if (isEnabled) {
                    if (remainder.isEmpty()) {
                        String userHost = (event.getUser().getLogin() + '@' + event.getUser().getHostmask()).toLowerCase();

                        if (defaultLocations.containsKey(userHost)) {
                            remainder = defaultLocations.get(userHost);
                        } else {
                            event.respond("Error: You didn't provide a location to check, and you don't have a default location set.");
                            event.respond("To lookup weather for a location, use syntax: " + COMMAND_WEATHER + " [location]");
                            event.respond("To set a default location for yourself, use syntax: " + COMMAND_SET_LOCATION + " [location]");
                            return;
                        }
                    }

                    getWeather(event, remainder);
                } else {
                    event.respond("Error: The " + COMMAND_WEATHER + " command is disabled.  An authorized user should try using the \"" +
                        COMMAND_WEATHER_CONFIG + ' ' + CFG_OP_ENABLE + "\" command.");
                }
                break;
            case COMMAND_USER_WEATHER:
                if (isEnabled) {
                    if (event instanceof GenericChannelEvent) {
                        if (commands.size() >= 2) {
                            User user = event.getBot().getUserChannelDao().getUser(commands.get(1));
                            String userHost = (user.getLogin() + '@' + user.getHostmask()).toLowerCase();

                            if (defaultLocations.containsKey(userHost)) {
                                getWeather(event, defaultLocations.get(userHost));
                            } else {
                                event.respond("Unable to comply: User " + commands.get(1) + " hasn't used the \"" + COMMAND_SET_LOCATION +
                                    "\" command to set their default location.");
                            }
                        } else {
                            event.respond("Error: Invalid arguments. Usage: " + COMMAND_USER_WEATHER + " [nickname]");
                            event.respond("Note: The \"" + COMMAND_USER_WEATHER + "\" command looks up weather by using the saved locations of *other* IRC users (set using the \"" +
                                COMMAND_SET_LOCATION + "\" command). To look up weather by location, or using your own saved location, use the \"" + COMMAND_WEATHER + "\" command.");
                        }
                    } else {
                        event.respond("Error: This command must be run in a channel.");
                    }
                } else {
                    event.respond("Error: The " + COMMAND_USER_WEATHER + " command is disabled.  An authorized user should try using the \"" +
                        COMMAND_WEATHER_CONFIG + ' ' + CFG_OP_ENABLE + "\" command.");
                }
                break;
            case COMMAND_WEATHER_CONFIG:
                if (commands.size() >= 2) {
                    if (acl.hasPermission(event, PERM_CFGWEATHER)) {
                        switch (commands.get(1)) {
                            case CFG_OP_DISABLE:
                                isEnabled = false;
                                sync();
                                event.respond(COMMAND_WEATHER + " command disabled.");
                                break;
                            case CFG_OP_ENABLE:
                                if (apiKey != null && rateLimiter.getApiCallsPerDay() > 0 && rateLimiter.getApiCallsPerMinute() > 0) {
                                    isEnabled = true;
                                    sync();
                                    event.respond(COMMAND_WEATHER + " command enabled.");
                                } else {
                                    event.respond("Unable to comply: You must configure an API key and API rate limit (daily and minutely) to enable the " + COMMAND_WEATHER + " command.");
                                }
                                break;
                            case CFG_OP_SET_API_BURST_SIZE:
                                if (!remainder.isEmpty()) {
                                    int newBurstSize;
                                    try {
                                        newBurstSize = Integer.parseInt(args[0]);
                                    } catch (NumberFormatException e) {
                                        event.respond("Error: \"" + args[0] + "\" isn't a valid integer!");
                                        return;
                                    }

                                    if (newBurstSize >= 1) {
                                        rateLimiter.setApiBurstSize(newBurstSize);
                                        sync();
                                        event.respond("API burst size set to " + newBurstSize + '.');
                                    } else {
                                        event.respond("Error: Argument must be greater or equal to 1.");
                                    }
                                } else {
                                    event.respond("Error: Invalid arguments. Syntax: " + COMMAND_WEATHER_CONFIG + ' ' + CFG_OP_SET_API_BURST_SIZE +
                                        " [burstSize]");
                                }
                                break;
                            case CFG_OP_SET_API_DAILY_CAP:
                                if (!remainder.isEmpty()) {
                                    int newRateLimit;
                                    try {
                                        newRateLimit = Integer.parseInt(args[0]);
                                    } catch (NumberFormatException e) {
                                        event.respond("Error: \"" + args[0] + "\" isn't a valid integer!");
                                        return;
                                    }

                                    if (newRateLimit >= 1) {
                                        rateLimiter.setApiCallsPerDay(newRateLimit);
                                        sync();
                                        event.respond("API daily rate limit set to " + newRateLimit + " calls per day.");
                                        event.respond("Note: API daily usage and progressive rate limit reset.");
                                    } else {
                                        event.respond("Error: Argument must be greater or equal to 1.");
                                    }
                                } else {
                                    event.respond("Error: Invalid arguments. Syntax: " + COMMAND_WEATHER_CONFIG + ' ' + CFG_OP_SET_API_DAILY_CAP +
                                        " [callsPerDay]");
                                }
                                break;
                            case CFG_OP_SET_API_MINUTE_CAP:
                                if (!remainder.isEmpty()) {
                                    int newRateLimit;
                                    try {
                                        newRateLimit = Integer.parseInt(args[0]);
                                    } catch (NumberFormatException e) {
                                        event.respond("Error: \"" + args[0] + "\" isn't a valid integer!");
                                        return;
                                    }

                                    if (newRateLimit >= 1) {
                                        rateLimiter.setApiCallsPerMinute(newRateLimit);
                                        sync();
                                        event.respond("API minutely rate limit set to " + newRateLimit + " calls per minute.");
                                        event.respond("Note: API call minutely usage reset.");
                                    } else {
                                        event.respond("Error: Argument must be greater or equal to 1.");
                                    }
                                } else {
                                    event.respond("Error: Invalid arguments. Syntax: " + COMMAND_WEATHER_CONFIG + ' ' + CFG_OP_SET_API_MINUTE_CAP +
                                        " [callsPerDay]");
                                }
                                break;
                            case CFG_OP_SET_API_KEY:
                                if (!remainder.isEmpty()) {
                                    apiKey = args[0];
                                    rateLimiter.reset();
                                    sync();
                                    event.respond("Weather Underground API key set to " + apiKey + '.');
                                    event.respond("Note: API daily and minutely usage and progressive rate limit reset.");
                                } else {
                                    event.respond("Error: Invalid arguments. Syntax: " + COMMAND_WEATHER_CONFIG + ' ' + CFG_OP_SET_API_KEY +
                                        " [apiKey]");
                                }
                                break;
                            case CFG_OP_ALERTS_ENABLE:
                                enableAlerts = true;
                                sync();
                                event.respond("Severe weather alerts display with weather data enabled.  WARNING: If your API key does not allow for alerts access (\"Cumulus Plan\"), this will cause API errors to occur!");
                                break;
                            case CFG_OP_ALERTS_DISABLE:
                                enableAlerts = false;
                                sync();
                                event.respond("Severe weather alerts display with weather data disabled.");
                                break;
                            case CFG_OP_SHOW_CFG:
                                if (!(event instanceof PrivateMessageEvent)) {
                                    event.respond("See private message.");
                                }

                                String target = event.getUser().getNick();
                                OutputIRC outputIRC = event.getBot().sendIRC();

                                outputIRC.message(target, "WeatherHandler configuration:");
                                outputIRC.message(target, "WeatherUnderground API key: " + ((apiKey == null) ? "NOT SET" : apiKey));
                                outputIRC.message(target, "API rate daily limit: " + ((rateLimiter.getApiCallsPerDay() > 0) ? rateLimiter.getApiCallsPerDay() + " per day" : "NOT SET"));
                                outputIRC.message(target, "API rate minutely limit: " + ((rateLimiter.getApiCallsPerMinute() > 0) ? rateLimiter.getApiCallsPerMinute() + " per minute" : "NOT SET"));
                                outputIRC.message(target, "API burst size: " + rateLimiter.getApiBurstSize() + " API calls");
                                outputIRC.message(target, "Severe weather alerts display: " + ((enableAlerts) ? "Enabled" : "Disabled"));
                                outputIRC.message(target, "WeatherHandler status: " + ((isEnabled) ? "ENABLED" : "DISABLED"));
                                outputIRC.message(target, "Note: Any values marked as \"NOT SET\" above will prevent WeatherHandler from being enabled.");
                                outputIRC.message(target, "Rate limiter status:");
                                outputIRC.message(target, "Time until per-minute rate limit reset: " + rateLimiter.getTimeRemainingThisMinute() + " seconds.");
                                outputIRC.message(target, "API calls remaining this minute: " + rateLimiter.getApiCallsMinuteRemaining());
                                outputIRC.message(target, "Note: If \"Time until per-minute rate limit reset\" is negative, \"API calls remaining this minute\" will be reset on next API call.");
                                outputIRC.message(target, "Time until per-day rate limit reset: " + rateLimiter.getTimeRemainingThisDay() + " seconds.");
                                outputIRC.message(target, "API calls remaining this day: " + rateLimiter.getApiCallsDayRemaining() + " calls (~" + (rateLimiter.getApiCallsDayRemaining() / rateLimiter.getApiBurstSize()) + " bursts)."); // Yes, integer division is intentional.

                                String apiRateDayString = "API rate limit for rest of day: ";
                                double apiRateDay = (double)rateLimiter.getApiCallsDayRemaining() / rateLimiter.getTimeRemainingThisDay();
                                if (apiRateDay < 0) {
                                    apiRateDayString += "(Will be reset on next API call)";
                                } else if (apiRateDay == 0) {
                                    apiRateDayString += "EXHAUSTED";
                                } else if (apiRateDay > 0 && apiRateDay < 1) {
                                    apiRateDay = 1 / apiRateDay;
                                    apiRateDayString += apiRateDay + " seconds per API call (~";
                                    apiRateDay *= rateLimiter.getApiBurstSize();
                                    apiRateDayString += apiRateDay + " seconds per API burst).";
                                } else {
                                    apiRateDayString += apiRateDay + " API calls per second (~";
                                    apiRateDay /= rateLimiter.getApiBurstSize();
                                    apiRateDayString += apiRateDay + " API bursts per second).";
                                }

                                outputIRC.message(target, apiRateDayString);
                                outputIRC.message(target, "Note: If \"Time until per-day rate limit reset\" is negative, \"API calls remaining this day\" will be reset on next API call.");
                                outputIRC.message(target, "API calls this burst period: " + rateLimiter.getApiCallsThisBurst());
                                outputIRC.message(target, "Burst period ends in: " + rateLimiter.getTimeRemainingThisBurstPeriod() + " seconds.");
                                outputIRC.message(target, "Current burst period length: " + rateLimiter.getBurstRateLimitStandoffSeconds() + " seconds.");
                                break;
                        }
                    } else {
                        event.respond("No u!  (You don't have the permissions necessary to do this.)");
                    }
                } else {
                    event.respond("Error: Too few arguments.  Syntax: " + COMMAND_WEATHER_CONFIG + " <" +
                        Miscellaneous.getStringRepresentation(CFG_OPERATIONS, "|") + "> {[operand]}");
                }
                break;
            case COMMAND_SET_LOCATION:
                String userHost = (event.getUser().getLogin() + '@' + event.getUser().getHostmask()).toLowerCase();

                if (!remainder.isEmpty()) {
                    if (resolveLocation(event, remainder) != null) {
                        defaultLocations.put(userHost, remainder);
                        sync();
                        event.respond("Done!  Set your default location for the \"" + COMMAND_WEATHER + "\" command to " + remainder + '.');
                    }
                } else {
                    defaultLocations.remove(userHost);
                    sync();
                    event.respond("Removed your default location setting for the \"" + COMMAND_WEATHER + "\" command.");
                }
                break;
        }
    }

    private synchronized void sync() {
        pm.setBoolean("enabled", isEnabled);
        if (apiKey != null) {
            pm.set("apiKey", apiKey);
        }
        pm.setBoolean("enableAlerts", enableAlerts);
        pm.setMap("defaultLocations", defaultLocations);
        rateLimiter.sync();
        // rateLimiter.sync() calls pm.sync()
    }

    private String resolveLocation(GenericMessageEvent event, String query) {
        LocationApiResponse locations;
        Gson gson = new Gson();
        try {
            locations = gson.fromJson(getAutocompleteData(query), LocationApiResponse.class);
        } catch (IOException e) {
            event.respond("Error: IOException while trying to get a list of matching locations: " + e.getMessage());
            return null;
        }

        if (locations.results.isEmpty()) {
            event.respond("Unable to comply: Your query for location \"" + query + "\" returned no usable locations from Weather Underground.");
            return null;
        } else if (locations.results.size() > 1) {
            event.respond("Unable to comply: Ambiguous query: Your query for location \"" + query + "\" returned multiple results from Weather Underground.  Please specify:");
            event.respond(
                locations.results.stream()
                    .map(r -> r.name)
                    .collect(Collectors.joining("; "))
            );
            return null;
        } else {
            String queryLocation;
            if (locations.results.get(0).type.equals(("pws"))) {
                queryLocation = "/q/PWS:" + query;
            } else {
                queryLocation = locations.results.get(0).link;
            }

            return queryLocation;
        }
    }

    private void getWeather(GenericMessageEvent event, String arg) {
        String queryLocation;
        queryLocation = resolveLocation(event, arg);
        if (queryLocation != null) {
            if (!queryLocation.startsWith("/q/")) {
                event.respond("Error: The Weather Underground location API returned a location result for your query, but it isn't usable for the Weather API itself.  Perhaps try a more specific location?");
                return;
            }

            WeatherApiResponse weatherData;
            try {
                Gson gson = new Gson();
                weatherData = gson.fromJson(getWeatherData(queryLocation), WeatherApiResponse.class);
            } catch (ApiRateLimitException e) {
                event.respond("Error: Rate limited.  " + e.getMessage() + '.');
                return;
            } catch (ApiGeneralException e) {
                event.respond("Error: Weather API returned an error: " + e.getMessage());
                return;
            } catch (IOException e) {
                event.respond("Error: IOException while trying to get weather data: " + e.getMessage());
                return;
            }

            List<WeatherApiAlert> alerts = weatherData.alerts;
            WeatherApiObservation obs = weatherData.currentObservation;

            String line1 = "Conditions at %s (%s) at %s: %s, temperature %s, humidity %s (feels like %s), dewpoint %s. Winds out of the %s (bearing %s) at %s MPH (%s KPH) with gusts to %s MPH (%s KPH).";
            line1 = String.format(line1, obs.observationLocation.fullName, obs.stationId, obs.observationTime, obs.conditions, obs.temperatureString, obs.relativeHumidity, obs.feelsLikeString, obs.dewpointString, obs.windDirection, obs.windBearing, obs.windSpeedMph, obs.windSpeedKph, obs.windGustsMph, obs.windGustsKph);
            String line2 = "Pressure was %s inHg (%s mb)%s. Visibility was %s miles (%s km). Solar radiation was %s W/m^2 and the UV index was %s. Today's precipitation was %s; current precip rate is %s per hour.";
            line2 = String.format(line2, obs.pressureInHg, obs.pressureMillibars, obs.getPressureTrendText(), obs.visibilityMi, obs.visibilityKm, obs.getSolarRadiationText(), obs.uvIndex, obs.precipTodayString, obs.precipHourString);
            String line3 = "Active severe weather alerts: %s";

            if (enableAlerts && alerts != null) {
                if (alerts.isEmpty()) {
                    line3 = String.format(line3, "NONE");
                } else {
                    String alert = "%s from %s to %s";
                    String output = alerts.stream()
                        .map(s -> String.format(alert, s.description, s.issued, s.expires))
                        .collect(Collectors.joining(", "));
                    line3 = String.format(line3, "\0034" + output + "\0034");
                }
            }
            // WARNING: Attribution is REQUIRED by the Terms of Service for the Weather Underground API
            String line4 = "Data provided by Weather Underground: <http://www.wunderground.com>. Forecast for this location: <%s>; conditions for this location: <%s>.";
            line4 = String.format(line4, obs.forecastUrl, obs.historyUrl);

            String target = event.getUser().getNick();
            if (event instanceof GenericChannelEvent) {
                target = ((GenericChannelEvent) event).getChannel().getName();
            }

            event.getBot().sendIRC().message(target, line1);
            event.getBot().sendIRC().message(target, line2);
            if (enableAlerts) {
                event.getBot().sendIRC().message(target, line3);
            }
            event.getBot().sendIRC().message(target, line4);
        }
    }

    private BufferedReader getAutocompleteData(String query) throws IOException {
        URL url = new URL(WUNDERGROUND_AUTOCOMPLETE_ENDPOINT + URLEncoder.encode(query, "UTF-8"));
        return getApiData(url);
    }

    private BufferedReader getWeatherData(String queryLocation) throws IOException {
        rateLimiter.check();
        URL url = new URL(String.format(WUNDERGROUND_API_ENDPOINT, apiKey, enableAlerts ? "alerts/conditions" : "conditions", queryLocation));
        return getApiData(url);
    }

    private BufferedReader getApiData(URL url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("User-Agent", httpUserAgent);
        httpURLConnection.connect();

        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error interacting with API: Got return status " + httpURLConnection.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) httpURLConnection.getContent(),
                StandardCharsets.UTF_8));

        if (httpURLConnection.getHeaderField("X-API-Error") != null) {
            if (httpURLConnection.getHeaderField("X-API-Error").equals("true")) {
                Gson gson = new Gson();
                WeatherApiErrorData error = gson.fromJson(reader, WeatherApiResponse.class).response.error;
                throw new ApiGeneralException(error.type + " (" + error.description + ')');
            }
        }

        return reader;
    }
}
