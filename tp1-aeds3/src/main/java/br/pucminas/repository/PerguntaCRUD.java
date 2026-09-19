package br.pucminas.repository;

import br.pucminas.model.Pergunta;
import br.pucminas.persistence.CRUD2;

import java.io.IOException;
import java.nio.file.Path;

public class PerguntaCRUD extends CRUD2<Pergunta> {
    public PerguntaCRUD(Path file) throws IOException {
        super(file, Pergunta::fromByteArray);
    }
}