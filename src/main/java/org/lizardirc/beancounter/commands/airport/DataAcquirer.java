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
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import org.lizardirc.beancounter.utils.Miscellaneous;

class DataAcquirer implements Runnable {
    private final DataHandler handler;

    private Set<Airport> airports;

    private Map<Pattern, String> runwaySurfaceMaterials = new HashMap<>();

    public DataAcquirer(DataHandler handler) {
        this.handler = handler;

        addSurfaceMaterialReplacementPattern("asph?", "asphalt");
        addSurfaceMaterialReplacementPattern("conc?", "concrete");
        addSurfaceMaterialReplacementPattern("grs", "grass");
        addSurfaceMaterialReplacementPattern("san", "sand");
        addSurfaceMaterialReplacementPattern("cop|com", "composite");
        addSurfaceMaterialReplacementPattern("gr?vl", "gravel");
        addSurfaceMaterialReplacementPattern("gre", "graded earth");
        addSurfaceMaterialReplacementPattern("cla", "clay");
        addSurfaceMaterialReplacementPattern("cor", "coral");
        addSurfaceMaterialReplacementPattern("pem", "partially concrete, asphalt or bitumen-bound macadam");
        addSurfaceMaterialReplacementPattern("per", "permanent surface");
        addSurfaceMaterialReplacementPattern("bit", "bitumenous asphalt or tarmac");
        addSurfaceMaterialReplacementPattern("lat", "laterite");
        addSurfaceMaterialReplacementPattern("bri", "bricks");
        addSurfaceMaterialReplacementPattern("mac", "macadam");
        addSurfaceMaterialReplacementPattern("smt", "Sommerfeld Tracking");
        addSurfaceMaterialReplacementPattern("psp|mats?", "Marston Matting");
    }

    private void addSurfaceMaterialReplacementPattern(String pattern, String replacement) {
        runwaySurfaceMaterials.put(Pattern.compile("\\b" + pattern + "\\b", Pattern.CASE_INSENSITIVE), replacement);
    }

    private String doSurfaceMaterialReplacement(String input) {
        String output = input;

        for (Map.Entry<Pattern, String> entry : runwaySurfaceMaterials.entrySet()) {
            output = entry.getKey().matcher(output).replaceAll(entry.getValue());
        }

        return output;
    }

    @Override
    public void run() {
        System.err.println("airport.DataAcquirer: Acquiring latest airport data, please wait.");
        airports = new HashSet<>();
        handler.setDone(false);

        Map<DataSources, Path> tempFiles = null;

        try {
            tempFiles = downloadData();
            parse(tempFiles);
        } catch (Exception e) {
            handler.setThrownException(e);
            System.err.println("airport.DataAcquirer ERROR: Failed to acquire airport data: " + e.toString());
            return;
        } finally {
            try {
                if (tempFiles != null) {
                    cleanupTempFiles(tempFiles);
                }
            } catch (Exception e) {
                System.err.println("airport.DataAcquirer WARNING: Failed to clean up temporary files: " + e.toString());
            }
        }

        handler.setDone(true);
        System.err.println("airport.DataAcquirer: Done acquiring and loading latest airport data");
    }

    private Map<DataSources, Path> downloadData() throws IOException {
        // I feel it's better to store the CSV data in tempfiles instead of shoving it all into memory, since the datasets
        // are already several megabytes in size.  We'll be loading it all into memory soon enough.

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
        final Map<String, List<Runway>> runways = new HashMap<>();

        for (CSVRecord record : records) {
            if (record.get("closed").equals("1")) {
                continue;
            }

            if (!runways.containsKey(record.get("airport_ref"))) {
                runways.put(record.get("airport_ref"), new ArrayList<>());
            }

            Runway runway = new Runway();
            StringBuilder runwayIdentifier = new StringBuilder(record.get("le_ident"));

            if (!record.get("he_ident").isEmpty()) {
                runwayIdentifier.append('/')
                    .append(record.get("he_ident"));
            }

            runway.identifier = runwayIdentifier.toString();

            runway.lit = record.get("lighted").equals("1");

            runway.surface = doSurfaceMaterialReplacement(record.get("surface"));

            try {
                runway.lengthInFt = Integer.parseInt(record.get("length_ft"));
            } catch (NumberFormatException e) {
                runway.lengthInFt = null;
            }

            runways.get(record.get("airport_ref")).add(runway);
        }

        // Finally, build our airport data
        in = new InputStreamReader(Files.newInputStream(dataSourcesPathMap.get(DataSources.AIRPORTS), StandardOpenOption.READ), "UTF-8");
        records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

        for (CSVRecord record : records) {
            if (record.get("type").equalsIgnoreCase("closed")) {
                continue;
            }

            Airport airport = new Airport();
            airport.iataCode = record.get("iata_code"); // WARNING - This may be an empty string
            airport.icaoCode = record.get("gps_code"); // WARNING - This may also be an empty string!
            airport.name = record.get("name");
            airport.type = record.get("type").replaceAll("_", " ");

            try {
                airport.elevationInFt = Integer.parseInt(record.get("elevation_ft"));
            } catch (NumberFormatException e) {
                airport.elevationInFt = null;
            }

            if (runways.containsKey(record.get("id"))) {
                airport.runways = runways.get(record.get("id")).toArray(new Runway[0]);
            } else {
                airport.runways = new Runway[0];
            }
            airport.municipality = record.get("municipality");
            airport.regionAndCountry = regions.get(record.get("iso_region"));
            airport.wikipediaUrl = record.get("wikipedia_link");

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
