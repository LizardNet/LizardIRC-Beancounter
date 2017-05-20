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

class Airport {
    public String iataCode;
    public String icaoCode;
    public String name;
    public String type;
    public Integer elevationInFt;
    public Runway[] runways;
    public String municipality;
    public String regionAndCountry;
    public String continent;
    public String wikipediaUrl;

    public boolean sameAirport(Airport other) {
        return this.iataCode.equalsIgnoreCase(other.iataCode)
            && this.icaoCode.equalsIgnoreCase(other.icaoCode)
            && this.name.equalsIgnoreCase(other.name);
    }

    @Override
    public String toString() {
        StringBuilder retval = new StringBuilder(name)
            .append(" (")
            .append(type);

        if (!iataCode.isEmpty()) {
            retval.append("; IATA: ")
                .append(iataCode);
        }

        if (!icaoCode.isEmpty()) {
            retval.append("; ICAO/GPS: ")
                .append(icaoCode);
        }
        retval.append("), ")
            .append(municipality)
            .append(", ")
            .append(regionAndCountry)
            .append(", ")
            .append(continent);

        if (elevationInFt != null) {
            retval.append("; Elevation: ")
                .append(elevationInFt)
                .append(" ft / ")
                .append(elevetionInM())
                .append(" m");
        }

        if (runways.length >= 1) {
            retval.append("; Runways/landing pads:");

            for (Runway runway : runways) {
                retval.append(' ')
                    .append(runway);
            }
        }

        if (!wikipediaUrl.isEmpty()) {
            retval.append(" - ")
                .append(wikipediaUrl);
        }

        return retval.toString();
    }

    public String toShortString() {
        StringBuilder retval = new StringBuilder(name);

        if (!icaoCode.isEmpty() || !iataCode.isEmpty()) {
            retval.append(" (");
        }

        if (!iataCode.isEmpty()) {
            retval.append("IATA: ")
                .append(iataCode);

            if (!icaoCode.isEmpty()) {
                retval.append("; ");
            }
        }

        if (!icaoCode.isEmpty()) {
            retval.append("ICAO/GPS: ")
                .append(icaoCode);
        }

        if (!icaoCode.isEmpty() || !iataCode.isEmpty()) {
            retval.append(')');
        }

        return retval.toString();
    }

    public Integer elevetionInM() {
        if (elevationInFt == null) {
            return null;
        }

        return AirportHandler.ftToM(elevationInFt);
    }
}
