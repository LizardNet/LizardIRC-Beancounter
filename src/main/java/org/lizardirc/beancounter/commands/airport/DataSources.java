package org.lizardirc.beancounter.commands.airport;

enum DataSources {
    AIRPORTS("http://ourairports.com/data/airports.csv", "Airports"),
    RUNWAYS("http://ourairports.com/data/runways.csv", "Runways"),
    REGIONS("http://ourairports.com/data/regions.csv", "Regions"),
    COUNTRIES("http://ourairports.com/data/countries.csv", "Countries");

    private final String url;
    private final String friendlyName;

    DataSources(String url, String friendlyName) {
        this.url = url;
        this.friendlyName = friendlyName;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return friendlyName;
    }
}
