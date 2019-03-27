package it.dariocast.miserere.classi;

public class Coordinate {
    public String confraternita;
    public String colore;
    public double lat;
    public double lon;
    public String estremo;

    public Coordinate(String confraternita, String colore, double lat, double lon, String estremo) {
        this.confraternita = confraternita;
        this.colore = colore;
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
