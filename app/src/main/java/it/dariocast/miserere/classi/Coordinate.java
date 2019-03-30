package it.dariocast.miserere.classi;

public class Coordinate {
    public int confraternitaId;
    public double lat;
    public double lon;
    public String estremo;

    public Coordinate(int confraternitaId, double lat, double lon, String estremo) {
        this.confraternitaId = confraternitaId;
        this.lat = lat;
        this.lon = lon;
        this.estremo = estremo;
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "confraternitaId='" + confraternitaId + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", estremo='" + estremo + '\'' +
                '}';
    }
}
