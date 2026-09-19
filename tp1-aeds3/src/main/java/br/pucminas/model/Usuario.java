package br.pucminas.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

// Agora o Usuario segue a interface da disciplina (aed3.InterfaceRegistro),
// para poder ser gravado pela classe Arquivo do professor.
public class Usuario implements aed3.InterfaceRegistro {
    private int idUsuario;
    private String nome;
    private String email;
    private String hashSenha;
    private String perguntaSecreta;
    private String hashRespostaSecreta;

    // O construtor vazio e obrigatorio: o Arquivo usa reflexao para criar o objeto
    public Usuario() {
        this("", "", "", "", "");
    }

    public Usuario(String nome, String email, String hashSenha,
                   String perguntaSecreta, String hashRespostaSecreta) {
        this.idUsuario = -1;
        this.nome = nome;
        this.email = email;
        this.hashSenha = hashSenha;
        this.perguntaSecreta = perguntaSecreta;
        this.hashRespostaSecreta = hashRespostaSecreta;
    }

    @Override
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(idUsuario);
        dos.writeUTF(texto(nome));
        dos.writeUTF(texto(email));
        dos.writeUTF(texto(hashSenha));
        dos.writeUTF(texto(perguntaSecreta));
        dos.writeUTF(texto(hashRespostaSecreta));
        return baos.toByteArray();
    }

    @Override
    public void deserialize(byte[] ba) throws Exception {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(ba));
        this.idUsuario = dis.readInt();
        this.nome = dis.readUTF();
        this.email = dis.readUTF();
        this.hashSenha = dis.readUTF();
        this.perguntaSecreta = dis.readUTF();
        this.hashRespostaSecreta = dis.readUTF();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    @Override
    public int getId() {
        return idUsuario;
    }

    @Override
    public void setId(int id) {
        this.idUsuario = id;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashSenha() {
        return hashSenha;
    }

    public void setHashSenha(String hashSenha) {
        this.hashSenha = hashSenha;
    }

    public String getPerguntaSecreta() {
        return perguntaSecreta;
    }

    public void setPerguntaSecreta(String perguntaSecreta) {
        this.perguntaSecreta = perguntaSecreta;
    }

    public String getHashRespostaSecreta() {
        return hashRespostaSecreta;
    }

    public void setHashRespostaSecreta(String hashRespostaSecreta) {
        this.hashRespostaSecreta = hashRespostaSecreta;
    }

    @Override
    public String toString() {
        return idUsuario + " - " + nome + " (" + email + ")";
    }
}
