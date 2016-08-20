/*
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

package org.lizardirc.beancounter.commands.weather;

import com.google.gson.annotations.SerializedName;

class WeatherApiObservation {
    @SerializedName("observation_location")
    public WeatherApiObservationLocation observationLocation;
    @SerializedName("station_id")
    public String stationId;
    @SerializedName("observation_time_rfc822")
    public String observationTime;
    @SerializedName("weather")
    public String conditions;
    @SerializedName("temperature_string")
    public String temperatureString;
    @SerializedName("relative_humidity")
    public String relativeHumidity;
    @SerializedName("wind_dir")
    public String windDirection;
    @SerializedName("wind_degrees")
    public String windBearing;
    @SerializedName("wind_mph")
    public String windSpeedMph;
    @SerializedName("wind_gust_mph")
    public String windGustsMph;
    @SerializedName("wind_kph")
    public String windSpeedKph;
    @SerializedName("wind_gust_kph")
    public String windGustsKph;
    @SerializedName("pressure_mb")
    public String pressureMillibars;
    @SerializedName("pressure_in")
    public String pressureInHg;
    @SerializedName("pressure_trend")
    public String pressureTrend;
    @SerializedName("dewpoint_string")
    public String dewpointString;
    @SerializedName("feelslike_string")
    public String feelsLikeString;
    @SerializedName("visibility_mi")
    public String visibilityMi;
    @SerializedName("visibility_km")
    public String visibilityKm;
    @SerializedName("solarradiation")
    public String solarRadiation;
    @SerializedName("UV")
    public String uvIndex;
    @SerializedName("precip_today_string")
    public String precipTodayString;
    @SerializedName("precip_1hr_string")
    public String precipHourString;
    @SerializedName("forecast_url")
    public String forecastUrl;
    @SerializedName("history_url")
    public String historyUrl;

    public String getPressureTrendText() {
        switch (pressureTrend) {
            case "+":
                return " and rising";
            case "-":
                return " and falling";
            case "0":
                return " and steady";
            default:
                return "";
        }
    }

    public String getSolarRadiationText() {
        if (solarRadiation.isEmpty() || solarRadiation.equals("--")) {
            return "N/A";
        } else {
            return solarRadiation;
        }
    }
}
