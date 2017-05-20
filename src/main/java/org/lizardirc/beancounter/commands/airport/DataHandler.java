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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

class DataHandler {
    private DataAcquirer dataAcquirer;
    private volatile boolean done = false;
    private volatile Exception thrownException = null;

    private Set<Airport> airports = new HashSet<>();
    private Map<String, List<Airport>> icaoCodeIndex;
    private Map<String, List<Airport>> iataCodeIndex;

    public void acquireData() {
        System.err.println("airport.DataHandler: Updating airport data in background");
        dataAcquirer = new DataAcquirer(this);
        Thread t = new Thread(dataAcquirer);
        t.start();
    }

    public synchronized boolean isDone() {
        return done;
    }

    public synchronized void setDone(boolean done) {
        this.done = done;

        if (this.done) {
            airports = dataAcquirer.getAirports();
            populateIndices();
        }
    }

    public synchronized void setThrownException(Exception thrownException) {
        this.thrownException = thrownException;
        done = false;
    }

    private void populateIndices() {
        // Ugh, duplicates

        icaoCodeIndex = new HashMap<>(airports.stream()
            .filter(a -> !a.icaoCode.isEmpty())
            .collect(Collectors.toMap(a -> a.icaoCode.toLowerCase(), Lists::newArrayList, (o, n) -> {
                System.err.println("airport.DataHandler WARNING: ICAO/GPS code " + n.get(0).icaoCode + " has duplicates!");
                return resolveCollision(o, n);
            }))
        );

        iataCodeIndex = new HashMap<>(airports.stream()
            .filter(a -> !a.iataCode.isEmpty())
            .filter(a -> !a.iataCode.equals("0"))
            .collect(Collectors.toMap(a -> a.iataCode.toLowerCase(), Lists::newArrayList, (o, n) -> {
                System.err.println("airport.DataHandler WARNING: IATA code " + n.get(0).iataCode + " has duplicates!");
                return resolveCollision(o, n);
            }))
        );
    }

    public String getByIcaoCode(String icaoCode) throws Exception {
        return getByCode(icaoCodeIndex, icaoCode);
    }

    public String getByIataCode(String iataCode) throws Exception {
        return getByCode(iataCodeIndex, iataCode);
    }

    private String getByCode(Map<String, List<Airport>> index, String code) throws Exception {
        checkDataReady();

        List<Airport> airport = index.get(code);

        if (airport == null || airport.isEmpty()) {
            return null;
        } else {
            return airportsToString(airport);
        }
    }

    public String searchByName(String name) throws Exception {
        // Expensive operation!
        checkDataReady();

        List<Airport> results = airports.stream()
            .filter(a -> a.name.toLowerCase().contains(name))
            .collect(Collectors.toList());

        if (results.isEmpty()) {
            return null;
        } else {
            return airportsToString(results);
        }
    }

    private String airportsToString(List<Airport> airports) {
        if (airports.isEmpty()) {
            throw new IllegalArgumentException("airportsToString() cannot accept an empty set");
        } else if (airports.size() == 1) {
            Airport airport = airports.get(0);

            StringBuilder retval = new StringBuilder(airport.name)
                .append(" (")
                .append(airport.type);

            if (!airport.iataCode.isEmpty()) {
                retval.append("; IATA: ")
                    .append(airport.iataCode);
            }

            if (!airport.icaoCode.isEmpty()) {
                retval.append("; ICAO/GPS: ")
                    .append(airport.icaoCode);
            }
            retval.append("), ")
                .append(airport.municipality)
                .append(", ")
                .append(airport.regionAndCountry)
                .append(", ")
                .append(airport.continent);

            if (airport.runways.length >= 1) {
                retval.append("; Runways/landing pads:");

                for (String runway : airport.runways) {
                    retval.append(' ')
                        .append(runway);
                }
            }

            return retval.toString();
        } else if (airports.size() <= 15) {
            StringBuilder retval = new StringBuilder(Integer.toString(airports.size()))
                .append(" matches; specify one for more details: ");

            Iterator<Airport> iter = airports.iterator();
            while (iter.hasNext()) {
                Airport airport = iter.next();

                retval.append(airport.name);

                if (!airport.icaoCode.isEmpty() || !airport.iataCode.isEmpty()) {
                    retval.append(" (");
                }

                if (!airport.iataCode.isEmpty()) {
                    retval.append("IATA: ")
                        .append(airport.iataCode);

                    if (!airport.icaoCode.isEmpty()) {
                        retval.append("; ");
                    }
                }

                if (!airport.icaoCode.isEmpty()) {
                    retval.append("ICAO/GPS: ")
                        .append(airport.icaoCode);
                }

                if (!airport.icaoCode.isEmpty() || !airport.iataCode.isEmpty()) {
                    retval.append(')');
                }

                if (iter.hasNext()) {
                    retval.append("; ");
                }
            }

            return retval.toString();
        } else {
            return Integer.toString(airports.size()) + " airports matched your query; please be more specific.";
        }
    }

    private void checkDataReady() throws Exception {
        if (!isDone()) {
            if (thrownException != null) {
                throw thrownException;
            } else {
                throw new DataNotReadyException();
            }
        }
    }

    private static ArrayList<Airport> resolveCollision(ArrayList<Airport> old, ArrayList<Airport> colliding) {
        // If any airports in the "old" set are equal (same name, IATA, and ICAO/GPS codes), pick the one that has
        // runways defined.  If neither or both have runways defined, discard the colliding.  The colliding set will
        // always contain only one airport.

        Iterator<Airport> iter = old.iterator();
        Airport newAirport = colliding.get(0);
        while (iter.hasNext()) {
            Airport oldAirport = iter.next();
            if (oldAirport.sameAirport(newAirport)) {
                if (oldAirport.runways.length > 0 ^ newAirport.runways.length > 0) {
                    if (oldAirport.runways.length > 0) {
                        return old;
                    } else {
                        iter.remove();
                    }
                } else {
                    return old;
                }
            }
        }

        old.addAll(colliding);
        return old;
    }
}
