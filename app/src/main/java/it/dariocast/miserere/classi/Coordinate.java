package it.dariocast.miserere.classi;

public class Coordinate {
    public String confraternita;
    public double lat;
    public double lon;
    public String estremo;

    public Coordinate(String confraternita, double lat, double lon, String estremo) {
        this.confraternita = confraternita;
        this.lat = lat;
        this.lon = lon;
        this.estremo = estremo;
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "confraternita='" + confraternita + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", estremo='" + estremo + '\'' +
                '}';
    }
}
