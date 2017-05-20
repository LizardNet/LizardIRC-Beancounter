package org.lizardirc.beancounter.commands.airport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import org.lizardirc.beancounter.utils.Miscellaneous;

class DataAcquirer implements Runnable {
    private final DataHandler handler;

    private Set<Airport> airports = new HashSet<>();

    public DataAcquirer(DataHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        handler.setDone(false);

        Map<DataSources, Path> tempFiles = null;

        try {
            tempFiles = downloadData();
            parse(tempFiles);
        } catch (Exception e) {
            handler.setThrownException(e);
            return;
        } finally {
            try {
                if (tempFiles != null) {
                    cleanupTempFiles(tempFiles);
                }
            } catch (Exception e) {
                // Nobody cares
            }
        }

        handler.setDone(true);
    }

    private Map<DataSources, Path> downloadData() throws IOException {
        // I feel it's better to store the data in tempfiles instead of shoving it all into memory, since the datasets
        // are already several megabytes in size.

        Map<DataSources, Path> retval = new EnumMap<>(DataSources.class);

        try {
            for (DataSources dataSource : DataSources.values()) {
                Path tempfile = Files.createTempFile("beancounter-airport-data-" + dataSource.toString(), ".tmp");
                retval.put(dataSource, tempfile);
                tempfile.toFile().deleteOnExit();
            }
        } catch (IOException e) {
            // Ensure that we delete any temporary files we successfully created, then rethrow the exception
            cleanupTempFiles(retval);
            throw e;
        }

        for (Map.Entry<DataSources, Path> entry : retval.entrySet()) {
            Files.copy(getDataFromUrl(entry.getKey()), entry.getValue(), StandardCopyOption.REPLACE_EXISTING);
        }

        return retval;
    }

    private InputStream getDataFromUrl(DataSources source) throws IOException {
        URL url = new URL(source.getUrl());

        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("User-Agent", Miscellaneous.generateHttpUserAgent());
        httpURLConnection.connect();

        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Error fetching " + source.toString() + " data: " + httpURLConnection.getResponseCode());
        }

        return (InputStream) httpURLConnection.getContent();
    }

    private void parse(Map<DataSources, Path> dataSourcesPathMap) throws IOException {
        // First, load in the country and region databases
        Reader in = new InputStreamReader(Files.newInputStream(dataSourcesPathMap.get(DataSources.COUNTRIES), StandardOpenOption.READ), "UTF-8");
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        final Map<String, String> countries = new HashMap<>();

        for (CSVRecord record : records) {
            countries.put(record.get("code"), record.get("name"));
        }

        in = new InputStreamReader(Files.newInputStream(dataSourcesPathMap.get(DataSources.REGIONS), StandardOpenOption.READ), "UTF-8");
        records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        final Map<String, String> regions = new HashMap<>();

        for (CSVRecord record : records) {
            String regionName = record.get("name") + ", " + countries.get(record.get("iso_country"));
            regions.put(record.get("code"), regionName);
        }

        // Next, read in the runway data
        in = new InputStreamReader(Files.newInputStream(dataSourcesPathMap.get(DataSources.RUNWAYS), StandardOpenOption.READ), "UTF-8");
        records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        final Map<String, List<String>> runways = new HashMap<>();

        for (CSVRecord record : records) {
            if (record.get("closed").equals("1")) {
                continue;
            }

            if (!runways.containsKey(record.get("airport_ref"))) {
                runways.put(record.get("airport_ref"), new ArrayList<>());
            }

            StringBuilder runway = new StringBuilder(record.get("le_ident"));

            if (!record.get("he_ident").isEmpty()) {
                runway.append('/')
                    .append(record.get("he_ident"));
            }

            runway.append(" (");

            if (record.get("lighted").equals("1")) {
                runway.append("lighted ");
            } else {
                runway.append("unlighted ");
            }

            runway.append(record.get("surface"))
                .append(", ")
                .append(record.get("length_ft"))
                .append(" ft)");

            runways.get(record.get("airport_ref")).add(runway.toString());
        }

        // Finally, build our airport data
        in = new InputStreamReader(Files.newInputStream(dataSourcesPathMap.get(DataSources.AIRPORTS), StandardOpenOption.READ), "UTF-8");
        records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

        for (CSVRecord record : records) {
            if (record.get("type").equalsIgnoreCase("closed") || record.get("gps_code").isEmpty()) {
                continue;
            }

            Airport airport = new Airport();
            airport.iataCode = record.get("iata_code"); // WARNING - This may be an empty string
            airport.icaoCode = record.get("gps_code");
            airport.name = record.get("name");
            airport.type = record.get("type");
            if (runways.containsKey(record.get("id"))) {
                airport.runways = runways.get(record.get("id")).toArray(new String[0]);
            } else {
                airport.runways = new String[0];
            }
            airport.municipality = record.get("municipality");
            airport.regionAndCountry = regions.get(record.get("iso_region"));

            switch (record.get("continent")) {
                case "NA":
                    airport.continent = "North America";
                    break;
                case "OC":
                    airport.continent = "Oceania";
                    break;
                case "AN":
                    airport.continent = "Antarctica";
                    break;
                case "EU":
                    airport.continent = "Europe";
                    break;
                case "AF":
                    airport.continent = "Africa";
                    break;
                case "AS":
                    airport.continent = "Asia";
                    break;
                case "SA":
                    airport.continent = "South America";
                    break;
                default:
                    airport.continent = record.get("continent");
                    break;
            }

            airports.add(airport);
        }
    }

    public Set<Airport> getAirports() {
        return airports;
    }

    private void cleanupTempFiles(Map<DataSources, Path> paths) throws IOException {
        for (Path path : paths.values()) {
            Files.delete(path);
        }
    }
}