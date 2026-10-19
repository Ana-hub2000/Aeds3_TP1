
package br.pucminas.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Avaliacao implements aed3.InterfaceRegistro {

    private int idAvaliacao;
    private int idUsuario;
    private int idResposta;
    private boolean avaliacao;

    public Avaliacao() {
        this(-1, -1, -1, false);
    }

    public Avaliacao(int idAvaliacao, int idUsuario,
                     int idResposta, boolean avaliacao) {
        this.idAvaliacao = idAvaliacao;
        this.idUsuario = idUsuario;
        this.idResposta = idResposta;
        this.avaliacao = avaliacao;
    }

    public int getId() {
        return idAvaliacao;
    }

    public int get_IdUsuario() {
        return idUsuario;
    }

    public int get_IdResposta() {
        return idResposta;
    }

    public boolean get_avaliacao() {
        return avaliacao;
    }

    public void setId(int idAvaliacao) {
        this.idAvaliacao = idAvaliacao;
    }

    public void set_IdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public void set_IdResposta(int idResposta) {
        this.idResposta = idResposta;
    }

    public void set_avaliacao(boolean avaliacao) {
        this.avaliacao = avaliacao;
    }

    @Override
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(idAvaliacao);
        dos.writeInt(idUsuario);
        dos.writeInt(idResposta);
        dos.writeBoolean(avaliacao);

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        this.idAvaliacao = dis.readInt();
        this.idUsuario = dis.readInt();
        this.idResposta = dis.readInt();
        this.avaliacao = dis.readBoolean();
    }
    
}
