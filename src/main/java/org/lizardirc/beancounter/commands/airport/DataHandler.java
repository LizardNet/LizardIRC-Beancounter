package org.lizardirc.beancounter.commands.airport;

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
                o.addAll(n);
                return o;
            }))
        );

        iataCodeIndex = new HashMap<>(airports.stream()
            .filter(a -> !a.iataCode.isEmpty())
            .filter(a -> !a.iataCode.equals("0"))
            .collect(Collectors.toMap(a -> a.iataCode.toLowerCase(), Lists::newArrayList, (o, n) -> {
                System.err.println("airport.DataHandler WARNING: IATA code " + n.get(0).iataCode + " has duplicates!");
                o.addAll(n);
                return o;
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
}
