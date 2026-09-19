package br.pucminas.model;

import br.pucminas.persistence.Registro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Pergunta implements Registro {
    private int idPergunta;
    private int idUsuario;
    private long criacao;
    private long alteracao;
    private short nota;
    private String pergunta;
    private String palavrasChave;
    private boolean ativa;

    public Pergunta() {
    }

    public Pergunta(int idUsuario, String pergunta, String palavrasChave) {
        long agora = System.currentTimeMillis();
        this.idUsuario = idUsuario;
        this.criacao = agora;
        this.alteracao = agora;
        this.nota = 0;
        this.pergunta = pergunta;
        this.palavrasChave = palavrasChave;
        this.ativa = true;
    }

    public static Pergunta fromByteArray(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Pergunta pergunta = new Pergunta();
            pergunta.idPergunta = in.readInt();
            pergunta.idUsuario = in.readInt();
            pergunta.criacao = in.readLong();
            pergunta.alteracao = in.readLong();
            pergunta.nota = in.readShort();
            pergunta.pergunta = in.readUTF();
            pergunta.palavrasChave = in.readUTF();
            pergunta.ativa = in.readBoolean();
            return pergunta;
        } catch (IOException e) {
            throw new IllegalArgumentException("Registro de pergunta inválido", e);
        }
    }

    @Override
    public byte[] toByteArray() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(idPergunta);
            out.writeInt(idUsuario);
            out.writeLong(criacao);
            out.writeLong(alteracao);
            out.writeShort(nota);
            out.writeUTF(safe(pergunta));
            out.writeUTF(safe(palavrasChave));
            out.writeBoolean(ativa);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível serializar pergunta", e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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