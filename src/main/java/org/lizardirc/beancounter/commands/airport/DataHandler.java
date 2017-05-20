package org.lizardirc.beancounter.commands.airport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.lizardirc.beancounter.utils.Miscellaneous;

class DataHandler {
    private DataAcquirer dataAcquirer;
    private volatile boolean done = false;
    private volatile Exception thrownException = null;

    private Set<Airport> airports = new HashSet<>();
    private Map<String, Airport> icaoCodeIndex = new HashMap<>();
    private Map<String, Airport> iataCodeIndex = new HashMap<>();

    public void acquireData() {
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
        icaoCodeIndex = new HashMap<>(airports.stream()
            .collect(Collectors.toMap(a -> a.icaoCode.toLowerCase(), a -> a, (o, n) -> n))
        );

        iataCodeIndex = new HashMap<>(airports.stream()
            .filter(a -> !a.iataCode.isEmpty())
            .collect(Collectors.toMap(a -> a.iataCode.toLowerCase(), a -> a, (o, n) -> n))
        );
    }

    public String getByIcaoCode(String icaoCode) throws Exception {
        return getByCode(icaoCodeIndex, icaoCode);
    }

    public String getByIataCode(String iataCode) throws Exception {
        return getByCode(iataCodeIndex, iataCode);
    }

    private String getByCode(Map<String, Airport> index, String code) throws Exception {
        checkDataReady();

        Airport airport = index.get(code);

        if (airport == null) {
            return null;
        } else {
            return airportToString(airport);
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
            return Miscellaneous.getStringRepresentation(results.stream()
                .map(this::airportToString)
                .collect(Collectors.toList()));
        }
    }

    private String airportToString(Airport airport) {
        StringBuilder retval = new StringBuilder(airport.name)
            .append(" (")
            .append(airport.type)
            .append("; ");

        if (!airport.iataCode.isEmpty()) {
            retval.append("IATA: ")
                .append(airport.iataCode)
                .append("; ");
        }

        retval.append("ICAO/GPS: ")
            .append(airport.icaoCode)
            .append("), ")
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
