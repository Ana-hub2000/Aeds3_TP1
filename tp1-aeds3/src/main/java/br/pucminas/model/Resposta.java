package br.pucminas.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

// Entidade Resposta (TP2). Segue o mesmo padrao de Pergunta e Usuario:
// implementa a interface da disciplina (aed3.InterfaceRegistro) e tem
// construtor vazio, porque o Arquivo cria os objetos por reflexao.
public class Resposta implements aed3.InterfaceRegistro {
    private int idResposta;
    private int idPergunta;
    private int idUsuario;
    private long criacao;
    private String resposta;
    private short nota;
    private short avaliacoes;
    private boolean ativa;

    public Resposta() {
        this(-1, -1, "");
    }

    public Resposta(int idPergunta, int idUsuario, String resposta) {
        this.idResposta = -1;
        this.idPergunta = idPergunta;
        this.idUsuario = idUsuario;
        this.criacao = System.currentTimeMillis();
        this.resposta = resposta == null ? "" : resposta.trim();
        this.nota = 0;
        this.avaliacoes = 0;
        this.ativa = true;
    }

    @Override
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(idResposta);
        dos.writeInt(idPergunta);
        dos.writeInt(idUsuario);
        dos.writeLong(criacao);
        dos.writeUTF(resposta == null ? "" : resposta);
        dos.writeShort(nota);
        dos.writeShort(avaliacoes);
        dos.writeBoolean(ativa);
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        this.idResposta = dis.readInt();
        this.idPergunta = dis.readInt();
        this.idUsuario = dis.readInt();
        this.criacao = dis.readLong();
        this.resposta = dis.readUTF();
        this.nota = dis.readShort();
        this.avaliacoes = dis.readShort();
        this.ativa = dis.readBoolean();
    }

    @Override
    public int getId() {
        return idResposta;
    }

    @Override
    public void setId(int id) {
        this.idResposta = id;
    }

    public int getIdResposta() {
        return idResposta;
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

    // Nao ha setResposta: a resposta nao pode ser editada depois de criada
    // (ela pode ja ter recebido avaliacoes). Para corrigir, arquiva e cria outra.
    public String getResposta() {
        return resposta;
    }

    public short getNota() {
        return nota;
    }

    public void setNota(short nota) {
        this.nota = nota;
    }

    public short getAvaliacoes() {
        return avaliacoes;
    }

    public void setAvaliacoes(short avaliacoes) {
        this.avaliacoes = avaliacoes;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}
