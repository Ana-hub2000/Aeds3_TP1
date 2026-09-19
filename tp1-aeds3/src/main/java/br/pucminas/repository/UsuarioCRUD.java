package br.pucminas.repository;

import br.pucminas.model.Usuario;
import br.pucminas.persistence.CRUD2;

import java.io.IOException;
import java.nio.file.Path;

public class UsuarioCRUD extends CRUD2<Usuario> {
    public UsuarioCRUD(Path file) throws IOException {
        super(file, Usuario::fromByteArray);
    }
}