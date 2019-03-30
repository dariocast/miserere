package it.dariocast.miserere.classi;

public class Confraternita {
    public int id;
    public String nome;
    public String colore;
    public String passcode;

    public Confraternita(int id, String nome, String colore, String passcode) {
        this.id = id;
        this.nome = nome;
        this.colore = colore;
        this.passcode = passcode;
    }

    @Override
    public String toString() {
        return nome;
    }
}
