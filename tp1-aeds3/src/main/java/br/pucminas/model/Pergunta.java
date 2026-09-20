package br.pucminas.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

// Mesma ideia do Usuario: implementa a interface da disciplina.
public class Pergunta implements aed3.InterfaceRegistro {
    private int idPergunta;
    private int idUsuario;
    private long criacao;
    private long alteracao;
    private short nota;
    private String pergunta;
    private String palavrasChave;
    private boolean ativa;

    public Pergunta() {
        this(-1, "", "");
    }

    public Pergunta(int idUsuario, String pergunta, String palavrasChave) {
        long agora = System.currentTimeMillis();
        this.idPergunta = -1;
        this.idUsuario = idUsuario;
        this.criacao = agora;
        this.alteracao = agora;
        this.nota = 0;
        this.pergunta = pergunta;
        this.palavrasChave = palavrasChave;
        this.ativa = true;
    }

    @Override
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(idPergunta);
        dos.writeInt(idUsuario);
        dos.writeLong(criacao);
        dos.writeLong(alteracao);
        dos.writeShort(nota);
        dos.writeUTF(texto(pergunta));
        dos.writeUTF(texto(palavrasChave));
        dos.writeBoolean(ativa);
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        this.idPergunta = dis.readInt();
        this.idUsuario = dis.readInt();
        this.criacao = dis.readLong();
        this.alteracao = dis.readLong();
        this.nota = dis.readShort();
        this.pergunta = dis.readUTF();
        this.palavrasChave = dis.readUTF();
        this.ativa = dis.readBoolean();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    @Override
    public int getId() {
        return idPergunta;
    }

    @Override
    public void setId(int id) {
        this.idPergunta = id;
    }

    public int getIdPergunta() {
        return idPergunta;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public long getCriacao() {
        return criacao;
    }

    public long getAlteracao() {
        return alteracao;
    }

    public void setAlteracao(long alteracao) {
        this.alteracao = alteracao;
    }

    public short getNota() {
        return nota;
    }

    public void setNota(short nota) {
        this.nota = nota;
    }

    public String getPergunta() {
        return pergunta;
    }

    public void setPergunta(String pergunta) {
        this.pergunta = pergunta;
    }

    public String getPalavrasChave() {
        return palavrasChave;
    }

    public void setPalavrasChave(String palavrasChave) {
        this.palavrasChave = palavrasChave;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}
