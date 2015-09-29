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
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import org.lizardirc.beancounter.hooks.CommandHandler;
import org.lizardirc.beancounter.persistence.PersistenceManager;
import org.lizardirc.beancounter.security.AccessControl;
import org.lizardirc.beancounter.utils.Miscellaneous;

public class WeatherHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String COMMAND_WEATHER = "weather";
    private static final String COMMAND_WEATHER_CONFIG = "cfgweather";
    private static final String COMMAND_SET_LOCATION = "setlocation";
    private static final Set<String> COMMANDS = ImmutableSet.of(COMMAND_WEATHER, COMMAND_WEATHER_CONFIG, COMMAND_SET_LOCATION);

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
    private final AccessControl<T> acl;
    private final Map<String, String> defaultLocations;

    private boolean isEnabled;
    private String apiKey;
    private int apiCallsPerDay;
    private int apiCallsPerMinute;
    private int apiBurstSize;
    private boolean enableAlerts;
    private LocalDateTime startOfApiMinute;
    private int apiCallsMinuteRemaining;
    private ZonedDateTime startOfApiDay;
    private int apiCallsDayRemaining;
    private int apiCallsThisBurst;
    private LocalDateTime burstRateLimitStart;
    private double burstRateLimitStandoffSeconds;

    public WeatherHandler(PersistenceManager pm, AccessControl<T> acl) {
        this.pm = pm;
        this.acl = acl;

        defaultLocations = new HashMap<>(pm.getMap("defaultLocations"));

        apiKey = pm.get("apiKey").orElse(null);
        apiCallsPerDay = pm.getInt("apiCallsPerDay").orElse(0);
        apiCallsPerMinute = pm.getInt("apiCallsPerMinute").orElse(0);
        apiBurstSize = pm.getInt("apiBurstSize").orElse(1);
        enableAlerts = pm.getBoolean("enableAlerts").orElse(false);

        if (apiKey != null && apiCallsPerDay > 0 && apiCallsPerMinute > 0) {
            isEnabled = pm.getBoolean("enabled").orElse(false);
        } else {
            System.err.println("WARNING: WeatherHandler not configured - API key and rate limit must be set.  Refusing to enable.");
            System.err.println("Perform the necessary configuration from IRC, then use the \"" + COMMAND_WEATHER_CONFIG + ' ' +
                CFG_OP_ENABLE + "\" command to enable the \"" + COMMAND_WEATHER + "\" command.");
            isEnabled = false;
        }

        String artifactVersion = getClass().getPackage().getImplementationVersion();
        if (artifactVersion == null) {
            artifactVersion = "";
        } else {
            artifactVersion = '/' + artifactVersion;
        }

        httpUserAgent = "LizardIRC-Beancounter" + artifactVersion + " (compatible; +https://www.lizardirc.org/index.php?page=beancounter)";

        apiCallsMinuteRemaining = apiCallsPerMinute;
        startOfApiMinute = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

        apiCallsDayRemaining = (pm.getInt("apiCallsDayRemaining").orElse(apiCallsPerDay));

        Optional<Long> tempStartOfApiDay = pm.getLong("startOfApiDay");
        if (tempStartOfApiDay.isPresent()) {
            startOfApiDay = ZonedDateTime.ofInstant(Instant.ofEpochSecond(tempStartOfApiDay.get()), ZoneId.of("America/New_York"));
        } else {
            startOfApiDay = ZonedDateTime.now(ZoneId.of("America/New_York"));
            startOfApiDay = startOfApiDay.withSecond(0).withMinute(0).withHour(0);
        }

        apiCallsThisBurst = 0;

        burstRateLimitStart = LocalDateTime.now();
        burstRateLimitStandoffSeconds = 0;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.size() == 0) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(COMMAND_WEATHER_CONFIG)) {
            return CFG_OPERATIONS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (commands.size() < 1) {
            return;
        }

        remainder = remainder.trim();
        String[] args = remainder.split(" ");

        switch (commands.get(0)) {
            case COMMAND_WEATHER:
                if (isEnabled) {
                    if (remainder.isEmpty()) {
                        String userHost = event.getUser().getLogin() + '@' + event.getUser().getHostmask();

                        if (defaultLocations.containsKey(userHost)) {
                            remainder = defaultLocations.get(userHost);
                        } else {
                            event.respond("Error: You didn't provide a location to check, and you don't have a default location set.");
                            event.respond("To lookup weather for a location, use syntax: " + COMMAND_WEATHER + " [location]");
                            event.respond("To set a default location for yourself, use syntax: " + COMMAND_SET_LOCATION + " [location]");
                            return;
                        }
                    }

                    String queryLocation;
                    queryLocation = resolveLocation(event, remainder);
                    if (queryLocation != null) {
                        if (!queryLocation.startsWith("/q/")) {
                            event.respond("Error: The Weather Underground location API returned a location result for your query, but it isn't usable for the Weather API itself.  Perhaps try a more specific location?");
                            return;
                        }

                        JsonObject weatherData;
                        try {
                            weatherData = getApiData(queryLocation);
                        } catch (APIRateLimitException e) {
                            event.respond("Error: Rate limited.  " + e.getMessage() + '.');
                            return;
                        } catch (APIGeneralException e) {
                            event.respond("Error: Weather API returned an error: " + e.getMessage());
                            return;
                        } catch (IOException e) {
                            event.respond("Error: IOException while trying to get weather data: " + e.getMessage());
                            return;
                        }

                        JsonArray alerts;

                        if (enableAlerts) {
                            alerts = weatherData.get("alerts").getAsJsonArray();
                        } else {
                            alerts = null;
                        }
                        weatherData = weatherData.get("current_observation").getAsJsonObject();

                        String location = weatherData.get("observation_location").getAsJsonObject().get("full").getAsString();
                        String stationId = weatherData.get("station_id").getAsString();
                        String observationTime = weatherData.get("observation_time_rfc822").getAsString();
                        String conditions = weatherData.get("weather").getAsString();
                        String temperature = weatherData.get("temperature_string").getAsString();
                        String humidity = weatherData.get("relative_humidity").getAsString();
                        String windDirection = weatherData.get("wind_dir").getAsString();
                        String windBearing = weatherData.get("wind_degrees").getAsString();
                        String windSpeedMph = weatherData.get("wind_mph").getAsString();
                        String windGustsMph = weatherData.get("wind_gust_mph").getAsString();
                        String windSpeedKph = weatherData.get("wind_kph").getAsString();
                        String windGustsKph = weatherData.get("wind_gust_kph").getAsString();
                        String pressureMb = weatherData.get("pressure_mb").getAsString();
                        String pressureInHg = weatherData.get("pressure_in").getAsString();
                        String pressureTrend;
                        switch (weatherData.get("pressure_trend").getAsString()) {
                            case "+":
                                pressureTrend = " and rising";
                                break;
                            case "-":
                                pressureTrend = " and falling";
                                break;
                            case "0":
                                pressureTrend = " and steady";
                                break;
                            default:
                                pressureTrend = "";
                                break;
                        }
                        String dewpoint = weatherData.get("dewpoint_string").getAsString();
                        String feelsLike = weatherData.get("feelslike_string").getAsString();
                        String visibilityMi = weatherData.get("visibility_mi").getAsString();
                        String visibilityKm = weatherData.get("visibility_km").getAsString();
                        String solarRadiation = weatherData.get("solarradiation").getAsString();
                        String uvIndex = weatherData.get("UV").getAsString();
                        String precipitationDaily = weatherData.get("precip_today_string").getAsString();
                        String precipitationHourly = weatherData.get("precip_1hr_string").getAsString();
                        String forecastURL = weatherData.get("forecast_url").getAsString();
                        String historyURL = weatherData.get("history_url").getAsString();

                        String line1 = "Conditions at %s (%s) at %s: %s, temperature %s, humidity %s (feels like %s), dewpoint %s. Winds out of the %s (bearing %s) at %s MPH (%s KPH) with gusts to %s MPH (%s KPH).";
                        line1 = String.format(line1, location, stationId, observationTime, conditions, temperature, humidity, feelsLike, dewpoint, windDirection, windBearing, windSpeedMph, windSpeedKph, windGustsMph, windGustsKph);
                        String line2 = "Pressure was %s inHg (%s mb)%s. Visiblity was %s miles (%s km). Solar radiation was %s W/m^2 and the UV index was %s. Today's precipitation was %s; current last hour precipitation is %s.";
                        line2 = String.format(line2, pressureInHg, pressureMb, pressureTrend, visibilityMi, visibilityKm, (solarRadiation.isEmpty()) || solarRadiation.equals("--") ? "N/A" : solarRadiation, uvIndex, precipitationDaily, precipitationHourly);
                        String line3 = "Active severe weather alerts: %s";

                        if (enableAlerts && alerts != null) {
                            if (alerts.size() == 0) {
                                line3 = String.format(line3, "NONE");
                            } else {
                                String alert = "%s from %s to %s";
                                List<String> alertTexts = new ArrayList<>();

                                for (int i = 0; i < alerts.size(); i++) {
                                    JsonObject alertObject = alerts.get(i).getAsJsonObject();
                                    alertTexts.add(String.format(alert, alertObject.get("description").getAsString(), alertObject.get("date").getAsString(), alertObject.get("expires").getAsString()));
                                }

                                line3 = String.format(line3, "\0034" + Miscellaneous.getStringRepresentation(alertTexts) + "\0034");
                            }
                        }
                        // WARNING: Attribution is REQUIRED by the Terms of Service for the Weather Underground API
                        String line4 = "Data provided by Weather Underground: <http://www.wunderground.com>. Forecast for this location: <%s>; conditions for this location: <%s>.";
                        line4 = String.format(line4, forecastURL, historyURL);

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
                } else {
                    event.respond("Error: The " + COMMAND_WEATHER + " command is disabled.  An authorized user should try using the \"" +
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
                                if (apiKey != null && apiCallsPerDay > 0 && apiCallsPerMinute > 0) {
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
                                        apiBurstSize = newBurstSize;
                                        sync();
                                        event.respond("API burst size set to " + apiBurstSize + '.');
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
                                        apiCallsPerDay = newRateLimit;
                                        apiCallsDayRemaining = apiCallsPerDay;
                                        burstRateLimitStandoffSeconds = 0;
                                        sync();
                                        event.respond("API daily rate limit set to " + apiCallsPerDay + " calls per day.");
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
                                        apiCallsPerMinute = newRateLimit;
                                        sync();
                                        event.respond("API minutely rate limit set to " + apiCallsPerMinute + " calls per minute.");
                                        apiCallsMinuteRemaining = apiCallsPerMinute;
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
                                    sync();
                                    event.respond("Weather Underground API key set to " + apiKey + '.');

                                    apiCallsDayRemaining = apiCallsPerDay;
                                    apiCallsMinuteRemaining = apiCallsPerMinute;
                                    burstRateLimitStandoffSeconds = 0;

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
                                outputIRC.message(target, "API rate daily limit: " + ((apiCallsPerDay > 0) ? apiCallsPerDay + " per day" : "NOT SET"));
                                outputIRC.message(target, "API rate minutely limit: " + ((apiCallsPerMinute > 0) ? apiCallsPerMinute + " per minute" : "NOT SET"));
                                outputIRC.message(target, "API burst size: " + apiBurstSize + " API calls");
                                outputIRC.message(target, "Severe weather alerts display: " + ((enableAlerts) ? "Enabled" : "Disabled"));
                                outputIRC.message(target, "WeatherHandler status: " + ((isEnabled) ? "ENABLED" : "DISABLED"));
                                outputIRC.message(target, "Note: Any values marked as \"NOT SET\" above will prevent WeatherHandler from being enabled.");
                                outputIRC.message(target, "Rate limiter status:");
                                outputIRC.message(target, "Time until per-minute rate limit reset: " + (60 - startOfApiMinute.until(LocalDateTime.now(), ChronoUnit.SECONDS)) + " seconds.");
                                outputIRC.message(target, "API calls remaining this minute: " + apiCallsMinuteRemaining);
                                outputIRC.message(target, "Note: If \"Time until per-minute rate limit reset\" is negative, \"API calls remaining this minute\" will be reset on next API call.");
                                outputIRC.message(target, "Time until per-day rate limit reset: " + (86400 - startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS)) + " seconds.");
                                outputIRC.message(target, "API calls remaining this day: " + apiCallsDayRemaining + " calls (~" + (apiCallsDayRemaining / apiBurstSize) + " bursts)."); // Yes, integer division is intentional.

                                String apiRateDayString = "API rate limit for rest of day: ";
                                double apiRateDay = (double) apiCallsDayRemaining / (86400 - startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS));
                                if (apiRateDay < 0) {
                                    apiRateDayString += "(Will be reset on next API call)";
                                } else if (apiRateDay == 0) {
                                    apiRateDayString += "EXHAUSTED";
                                } else if (apiRateDay > 0 && apiRateDay < 1) {
                                    apiRateDay = 1 / apiRateDay;
                                    apiRateDayString += apiRateDay + " seconds per API call (~";
                                    apiRateDay *= apiBurstSize;
                                    apiRateDayString += apiRateDay + " seconds per API burst).";
                                } else {
                                    apiRateDayString += apiRateDay + " API calls per second (~";
                                    apiRateDay /= apiBurstSize;
                                    apiRateDayString += apiRateDay + " API bursts per second).";
                                }

                                outputIRC.message(target, apiRateDayString);
                                outputIRC.message(target, "Note: If \"Time until per-day rate limit reset\" is negative, \"API calls remaining this day\" will be reset on next API call.");
                                outputIRC.message(target, "API calls this burst period: " + apiCallsThisBurst);
                                outputIRC.message(target, "Burst period ends in: " + (burstRateLimitStandoffSeconds - (burstRateLimitStart.until(LocalDateTime.now(), ChronoUnit.SECONDS))) + " seconds.");
                                outputIRC.message(target, "Current burst period length: " + burstRateLimitStandoffSeconds + " seconds.");
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
                String userHost = event.getUser().getLogin() + '@' + event.getUser().getHostmask();

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
        pm.setInt("apiCallsPerDay", apiCallsPerDay);
        pm.setInt("apiCallsPerMinute", apiCallsPerMinute);
        pm.setInt("apiBurstSize", apiBurstSize);
        pm.setBoolean("enableAlerts", enableAlerts);
        pm.setMap("defaultLocations", defaultLocations);
        pm.setInt("apiCallsDayRemaining", apiCallsDayRemaining);
        pm.setLong("startOfApiDay", startOfApiDay.toEpochSecond());
        pm.sync();
    }

    private String resolveLocation(GenericMessageEvent<T> event, String query) {
        LocationAPIResponse locations;
        Gson gson = new Gson();
        try {
            locations = gson.fromJson(getAutocompleteData(query), LocationAPIResponse.class);
        } catch (IOException e) {
            event.respond("Error: IOException while trying to get a list of matching locations: " + e.getMessage());
            return null;
        }

        if (locations.RESULTS.length == 0) {
            event.respond("Unable to comply: Your query for location \"" + query + "\" returned no usable locations from Weather Underground.");
            return null;
        } else if (locations.RESULTS.length > 1) {
            List<String> locationList = new ArrayList<>();
            event.respond("Unable to comply: Ambiguous query: Your query for location \"" + query + "\" returned multiple results from Weather Underground.  Please specify:");
            for (int i = 0; i < locations.RESULTS.length; i++) { // I'm sure TLUL will find a better way to do this, meh.
                locationList.add(locations.RESULTS[i].name);
            }
            event.respond(Miscellaneous.getStringRepresentation(locationList, "; "));
            return null;
        } else {
            String queryLocation;
            if (locations.RESULTS[0].type.equals(("pws"))) {
                queryLocation = "/q/PWS:" + query;
            } else {
                queryLocation = locations.RESULTS[0].l;
            }

            return queryLocation;
        }
    }

    private String getAutocompleteData(String query) throws IOException {
        URL url = new URL(WUNDERGROUND_AUTOCOMPLETE_ENDPOINT + URLEncoder.encode(query, "UTF-8"));
        return getApiData(url);
    }

    private JsonObject getApiData(String queryLocation) throws IOException {
        // First, check the hard per-minute ratelimit
        if (startOfApiMinute.until(LocalDateTime.now(), ChronoUnit.SECONDS) >= 60) {
            startOfApiMinute = LocalDateTime.now();
            apiCallsMinuteRemaining = apiCallsPerMinute;
        }

        if (apiCallsMinuteRemaining > 0) {
            apiCallsMinuteRemaining--;
        } else {
            long timeSinceMinuteStart = startOfApiMinute.until(LocalDateTime.now(), ChronoUnit.SECONDS);
            throw new APIRateLimitException("Per-minute rate limit exceeded; try again in " + (60 - timeSinceMinuteStart) + " seconds");
        }

        // Next, check the hard per-day ratelimit
        if (startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS) >= 86400) {
            startOfApiDay = ZonedDateTime.now(ZoneId.of("America/New_York"));
            startOfApiDay = startOfApiDay.withSecond(0).withMinute(0).withHour(0);
            apiCallsDayRemaining = apiCallsPerDay;
            apiCallsThisBurst = 0;
        }

        if (apiCallsDayRemaining > 0) {
            apiCallsDayRemaining--;
        } else {
            apiCallsMinuteRemaining++; // "Refund" the per-minute API call
            long timeSinceDayStart = startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
            throw new APIRateLimitException("Per-day rate limit exceeded; try again in " + (86400 - timeSinceDayStart) + " seconds");
        }

        // Finally, check the progressive burst-based ratelimit
        // First, check if we're currently in a burst period
        if (burstRateLimitStandoffSeconds - (burstRateLimitStart.until(LocalDateTime.now(), ChronoUnit.SECONDS)) > 0) {
            // Yes, we're in a burst period.  Check if we've sent more API calls than allowed in a burst; if so, stand off
            if (apiCallsThisBurst >= apiBurstSize) {
                // We're standing off, refund the daily and minutely API calls and throw an exception
                apiCallsDayRemaining++;
                apiCallsMinuteRemaining++;
                throw new APIRateLimitException("Progressive rate limit exceeded; try again in " +
                    (burstRateLimitStandoffSeconds - (burstRateLimitStart.until(LocalDateTime.now(), ChronoUnit.SECONDS))) + " seconds");
            } else {
                // We're not standing off, but we're still in a burst period.
                apiCallsThisBurst++;
            }
        } else {
            // We're not currently in a burst period.  Reset the burst count, and begin a new burst period.
            apiCallsThisBurst = 1;
            // Calculate the number of bursts remaining today, rounded up
            double todayRemainingBursts = Math.ceil((double) (apiCallsDayRemaining + 1) / apiBurstSize);
            // Calculate the number of seconds between bursts, rounded down
            long timeSinceDayStart = startOfApiDay.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
            double secondsBetweenBursts = Math.floor((86400 - timeSinceDayStart) / todayRemainingBursts);
            burstRateLimitStart = LocalDateTime.now();
            burstRateLimitStandoffSeconds = secondsBetweenBursts;
        }

        sync();

        URL url = new URL(String.format(WUNDERGROUND_API_ENDPOINT, apiKey, enableAlerts ? "alerts/conditions" : "conditions", queryLocation));
        return getJsonData(url);
    }

    private JsonObject getJsonData(URL url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("User-Agent", httpUserAgent);
        httpURLConnection.connect();

        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error interacting with API: Got return status " + httpURLConnection.getResponseCode());
        }

        JsonParser jsonParser = new JsonParser();
        JsonElement root = jsonParser.parse(new InputStreamReader((InputStream) httpURLConnection.getContent(), "UTF-8"));
        JsonObject jsonObject = root.getAsJsonObject();

        if (httpURLConnection.getHeaderField("X-API-Error") != null) {
            if (httpURLConnection.getHeaderField("X-API-Error").equals("true")) {
                JsonObject error = jsonObject.get("response").getAsJsonObject().get("error").getAsJsonObject();
                throw new APIGeneralException(error.get("type").getAsString() + " (" + error.get("description").getAsString() + ')');
            }
        }

        return jsonObject;
    }

    private String getApiData(URL url) throws IOException {
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
        return retval.toString();

        /*if (httpURLConnection.getHeaderField("X-API-Error") != null) {
            if (httpURLConnection.getHeaderField("X-API-Error").equals("true")) {
                JsonObject error = jsonObject.get("response").getAsJsonObject().get("error").getAsJsonObject();
                throw new APIGeneralException(error.get("type").getAsString() + " (" + error.get("description").getAsString() + ')');
            }
        } */
    }

    private static class LocationAPIResponse {
        public LocationAPIResult[] RESULTS;
    }

    private static class LocationAPIResult {
        public String name;
        public String type;
        public String l;
    }

    private static class APIGeneralException extends IOException {
        public APIGeneralException() {
            super();
        }

        public APIGeneralException(String message) {
            super(message);
        }
    }

    private static class APIRateLimitException extends APIGeneralException {
        public APIRateLimitException() {
            super();
        }

        public APIRateLimitException(String message) {
            super(message);
        }
    }
}
