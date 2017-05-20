package org.lizardirc.beancounter.commands.airport;

class Airport {
    public String iataCode;
    public String icaoCode;
    public String name;
    public String type;
    public String[] runways;
    public String municipality;
    public String regionAndCountry;
    public String continent;

    public boolean sameAirport(Airport other) {
        return this.iataCode.equalsIgnoreCase(other.iataCode)
            && this.icaoCode.equalsIgnoreCase(other.icaoCode)
            && this.name.equalsIgnoreCase(other.name);
    }
}
