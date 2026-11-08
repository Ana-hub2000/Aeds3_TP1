# Relatório do Trabalho Prático 1 — AEDS III

### Sistema Ajuda Aí 1.0

### Participantes

| Nome completo | Matrícula |
| --- | --- |
| Ana Flávia Menezes de Almeida | 8976988 |
| Clarisse de Assis Pereira | 903541 |
| Eduardo Borges de Souza | 890309 |
| Rian Chaves Pimenta | 898910 |


- **Turma:** (8203100) Algoritmos e Estruturas de Dados III
- **Professor:** Marcos André Silveira Kutova
- **Repositório:** https://github.com/Ana-hub2000/Aeds3_TP1
- **Vídeo de demonstração:** https://github.com/Ana-hub2000/Aeds3_TP1/tree/main/video

## 1. Descrição do trabalho

O Ajuda Aí 1.0 é um sistema de perguntas desenvolvido em Java, com interface de texto e armazenamento em arquivos binários. Nesta primeira etapa foram trabalhados o cadastro e a autenticação de usuários, a recuperação de senha e o gerenciamento das perguntas do usuário que está conectado.

Após o login, o usuário pode consultar e alterar seus dados, incluir perguntas, listar suas perguntas ativas, alterar o texto e as palavras-chave de uma pergunta e arquivá-la. As áreas de respostas, votos e busca global aparecem no menu, mas foram deixadas para etapas posteriores, conforme previsto no enunciado.

Os dados não dependem de um banco de dados externo. Usuários, perguntas e índices são gravados em arquivos binários no diretório `data`. Quando o programa é iniciado, esses arquivos são abertos e os índices são reconstruídos a partir dos registros válidos.

## 2. Funcionalidades implementadas

### 2.1 Cadastro de usuário

O cadastro solicita nome, e-mail, senha, pergunta secreta e resposta secreta. O e-mail é convertido para letras minúsculas e tem os espaços das extremidades removidos antes de ser pesquisado ou gravado. Isso impede, por exemplo, que `Aluno@Email.com` e `aluno@email.com` sejam tratados como contas diferentes.

Antes da gravação, o sistema consulta o índice de e-mails. Caso encontre outra conta com o mesmo endereço, apresenta a mensagem `E-mail já cadastrado.`. Os IDs são inteiros positivos e gerados automaticamente pelo CRUD.

**Observação:** a tela atual lê todos os campos e só depois verifica se o e-mail já existe. O enunciado sugere verificar o e-mail antes de solicitar os demais dados.

### 2.2 Login e recuperação de senha

O login pesquisa o usuário pelo e-mail e compara o hash da senha informada com o hash armazenado. Quando o login falha, o sistema permite tentar novamente, recuperar a senha ou voltar ao menu anterior.

Na recuperação, a pergunta secreta cadastrada é apresentada. A resposta é normalizada antes do cálculo do hash: seus espaços externos e acentos são removidos e as letras são transformadas em minúsculas. Assim, respostas como `São Paulo` e `sao paulo` produzem o mesmo resultado. Se a resposta estiver correta, uma nova senha pode ser cadastrada.

As senhas e respostas secretas não são armazenadas como texto. O programa grava somente seus hashes SHA-256.

### 2.3 Alteração dos dados do usuário

Na opção **Meus dados**, podem ser alterados:

- nome;
- e-mail;
- senha;
- pergunta e resposta de recuperação.

A alteração de e-mail exige uma operação especial. O método `BancoDados.atualizarUsuario` normaliza e verifica o novo endereço, atualiza o registro do usuário, remove a entrada do e-mail antigo da tabela hash e inclui a entrada do novo e-mail apontando para o mesmo ID. O ID do usuário não é alterado.

### 2.4 Cadastro e vínculo das perguntas

Uma pergunta armazena os seguintes campos:

- `idPergunta`: identificador sequencial e único no sistema;
- `idUsuario`: identificador do autor;
- `criacao`: data e hora de criação em milissegundos;
- `alteracao`: data e hora da última alteração em milissegundos;
- `nota`: soma dos votos, iniciada em zero;
- `pergunta`: texto da pergunta;
- `palavrasChave`: termos separados por ponto e vírgula;
- `ativa`: indica se a pergunta está ativa ou arquivada.

Ao incluir uma pergunta pela interface, o programa usa automaticamente o ID do usuário conectado. O construtor também preenche as datas de criação e alteração, define a nota como zero e marca a pergunta como ativa.

### 2.5 Listagem e alteração das perguntas

A listagem consulta o índice que relaciona o ID do usuário aos IDs de suas perguntas. O menu mostra uma numeração temporária, iniciada em 1, sem expor os IDs internos. A data é apresentada no formato `dd/MM/yyyy HH:mm`.

Para alterar uma pergunta, o usuário escolhe essa numeração. O sistema recupera o objeto correspondente, permite alterar texto e palavras-chave e atualiza o campo `alteracao` com o horário atual.

### 2.6 Arquivamento

O arquivamento não apaga fisicamente o registro. O método `Menu.arquivarPergunta` muda `ativa` para `false`, atualiza a data de alteração e chama `BancoDados.atualizarPergunta`. O CRUD marca a versão anterior com uma lápide e acrescenta a versão atualizada ao final do arquivo.

O sistema não oferece uma opção de desarquivamento, portanto o procedimento é definitivo pela interface atual.

**Limitação conhecida:** a consulta `perguntasDoUsuario` filtra as perguntas inativas. Por esse motivo, uma pergunta arquivada deixa de aparecer até para seu autor, enquanto o enunciado pede que ela continue visível ao autor com a indicação `ARQUIVADA`.

## 3. Organização das classes

### 3.1 Inicialização e interface

- **`Main`**: define o diretório dos dados, abre o banco e inicia o menu.
- **`App`**: ponto de entrada mantido para executar `Main` pelo Maven.
- **`Menu`**: controla as telas, lê as opções e coordena cadastro, login, recuperação, edição de dados e gestão de perguntas.

### 3.2 Entidades

- **`Usuario`**: representa o usuário e implementa `aed3.InterfaceRegistro` (`serialize`/`deserialize`, `getId`/`setId`).
- **`Pergunta`**: representa a pergunta, seus metadados, seu autor e seu estado de arquivamento; também implementa `aed3.InterfaceRegistro`.

### 3.3 Persistência e índices (classes do professor, pacote `aed3`)

- **`Arquivo<T>`**: CRUD fornecido na disciplina. Grava lápide, indicador de tamanho em `short` e o vetor de bytes, e mantém o índice direto ID → endereço em uma **Tabela Hash Extensível** (`HashExtensivel<ParIDEndereco>`).
- **`HashExtensivel<T>`**: tabela hash extensível com diretório e cestos gravados em disco.
- **`ArvoreBMais<T>`**: Árvore B+ real (páginas, divisão de páginas e ligação entre folhas).
- **`ParIdId`**: par `(idUsuario, idPergunta)` guardado na Árvore B+ do relacionamento 1:N.
- **`ParEmailId`** (`br.pucminas.persistence`): adaptação do `ParNomeId` do professor, com campo de 60 bytes, usada como índice indireto e-mail → ID do usuário na Árvore B+.
- **`BancoDados`**: concentra os dois arquivos de dados, os índices e as regras que envolvem mais de um arquivo.

Nós acrescentamos um construtor extra em `Arquivo`, que recebe a pasta base dos dados (o original grava sempre em `./dados`). Isso permite usar pastas temporárias nos testes; o construtor original continua existindo. No TP3, também acrescentamos `AutoCloseable` e `close()` à cópia Maven de `ListaInvertida`, para fechar os arquivos do dicionário e dos blocos. Nós mantivemos os algoritmos de armazenamento fornecidos na disciplina.

### 3.4 Segurança

- **`Seguranca`**: calcula hashes SHA-256, normaliza respostas secretas e compara a senha informada com o hash gravado.

### 3.5 Testes

- **`BancoDadosTest`** (em `src/test/java`): persistência de usuários e perguntas, alteração, arquivamento, listagem com e sem arquivadas, recusa de pergunta com usuário inexistente, carga de 25 perguntas para forçar a divisão de páginas da Árvore B+ e reabertura do banco.
- **`AppTest`**: teste básico gerado pelo modelo inicial do Maven.

## 4. Estrutura dos arquivos

As entidades convertem seus campos para vetores de bytes com `DataOutputStream` e fazem o caminho inverso com `DataInputStream`.

Cada registro no arquivo de dados contém:

1. uma lápide de um byte, que informa se o registro está ativo ou excluído;
2. o tamanho do vetor de bytes gravado em `short` (`writeShort` / `readUnsignedShort`);
3. o vetor de bytes produzido pela própria entidade.

O `Arquivo` do professor ainda reaproveita espaço: registros excluídos entram em uma lista de espaços livres e podem ser reutilizados por novos registros do mesmo tamanho.

Os arquivos ficam em `dados/usuarios/`, `dados/perguntas/` (dados + índice direto em hash extensível) e em `dados/indiceEmail.bplus` e `dados/indicePerguntas.bplus` (árvores B+).

## 5. Índices e relacionamento 1:N

### 5.1 Índice direto por ID

É o `HashExtensivel<ParIDEndereco>` que já vem dentro da classe `Arquivo`: a chave é o ID da entidade e o valor é o endereço do registro no arquivo, o que evita percorrer o arquivo em cada leitura.

### 5.2 Índice de e-mail

Índice indireto em Árvore B+ de pares `(email, idUsuario)`, usando a classe `ParEmailId`. O e-mail é normalizado (sem espaços, em minúsculas) antes de entrar na árvore. Quando o usuário troca o e-mail, o par antigo é removido e o novo é inserido.

### 5.3 Relacionamento usuário–perguntas

O vínculo 1:N usa a Árvore B+ de pares `(idUsuario, idPergunta)` (`ParIdId`). Como o `compareTo` do `ParIdId` ignora o segundo ID quando ele vale `-1`, a busca por `new ParIdId(idUsuario)` devolve de uma vez todas as perguntas daquele usuário.

## 6. Operações especiais implementadas

1. **Geração automática de IDs:** o cabeçalho do arquivo guarda o último ID usado e o `Arquivo` apenas incrementa esse valor.
2. **Lápide e reaproveitamento de espaço:** a exclusão marca a lápide e devolve o espaço para a lista de livres.
3. **Normalização de e-mail:** as buscas e gravações usam e-mail sem espaços externos e em letras minúsculas.
4. **Atualização do índice de e-mail:** ao trocar o e-mail, o par antigo é apagado da árvore e o novo é inserido.
5. **Proteção de senha e resposta:** somente hashes SHA-256 são persistidos.
6. **Normalização da resposta secreta:** acentos e diferenças entre maiúsculas e minúsculas são desconsiderados.
7. **Associação 1:N:** cada pergunta guarda o `idUsuario`, e a Árvore B+ relaciona um usuário a várias perguntas.
8. **Validação de integridade:** `BancoDados.criarPergunta` recusa perguntas cujo `idUsuario` não existe no arquivo de usuários.
9. **Reconstrução dos índices:** se um arquivo de índice estiver vazio, ele é remontado a partir dos arquivos de dados na abertura do sistema.
10. **Arquivamento:** a pergunta continua no arquivo com `ativa = false` e aparece para o autor marcada como `ARQUIVADA`.

## 7. Checklist obrigatório

* Há um CRUD de usuários (que estende a classe Arquivo, acrescentando Tabelas Hash Extensíveis e Árvores B+ como índices diretos e indiretos conforme necessidade) que funciona corretamente?

**Sim.** Os usuários são gravados pela classe `Arquivo` do professor, que já traz o índice direto em tabela hash extensível (ID → endereço). O índice indireto por e-mail é uma Árvore B+ de pares `(email, idUsuario)`.

* Há um CRUD de perguntas (que estende a classe Arquivo, acrescentando Tabelas Hash Extensíveis e Árvores B+ como índices diretos e indiretos conforme necessidade) que funciona corretamente?

**Sim.** As perguntas usam o mesmo `Arquivo` (com hash extensível por ID) e uma Árvore B+ com os pares `(idUsuario, idPergunta)`. A listagem mostra ao autor também as perguntas arquivadas.

* As perguntas estão vinculadas aos usuários usando o idUsuario como chave estrangeira?

**Sim.** A entidade `Pergunta` guarda o `idUsuario` e `BancoDados.criarPergunta` verifica se esse usuário existe antes de gravar.

* Há uma árvore B+ que registre o relacionamento 1:N entre usuários e perguntas?

**Sim.** É a `ArvoreBMais<ParIdId>` fornecida pelo professor, gravada em `dados/indicePerguntas.bplus`. O teste com 25 perguntas do mesmo usuário passa pela divisão de páginas e continua recuperando todos os registros.

* O trabalho compila corretamente?

**Sim.** `mvn test` termina com `BUILD SUCCESS`.

* O trabalho está completo e funcionando sem erros de execução?

**Sim, para o escopo do TP1.** Cadastro, login, recuperação de senha, alteração de dados e gestão de perguntas funcionam. Busca global, respostas e votos são etapas seguintes e não fazem parte deste trabalho.

* O trabalho é original e não a cópia de um trabalho de outro grupo?

**Sim,** este trabalho foi realizado com base no que foi passado em sala de aula, apezar de, também, contar com auxílio de inteligência artificial como ferramenta de suporte.

## 8. Capturas de tela

A seguir algumas imagens de funcionalidades aplicadas.

<center><h3> Tela inicial </h3></center> 
<p align="center">
    <img width="400" alt="image" src="imgs\anterior.png" />
</p>

<center><h3> Cadastro </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\novo_usuario.jpeg" />
</p>

<center><h3> Recuperação de senha </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\senha_alterada.jpeg" />
</p>

<center><h3> Menu principal </h3></center>
<p align="center">
    <img width="400" alt="image" src="https://github.com/user-attachments/assets/d3712a4d-33d7-4620-90a7-0c0a352ea861" />
</p>

<center><h3> Menu de dados </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\antes.jpg" />
</p>

<center><h3> Dados / alteração de email </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\novo_email.jpg" />
</p>

<center><h3> Área de perguntas do usuário </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\perguntas_menu.jpg" /> 
</p>

<center><h3> Área de perguntas / cadastro de pergunta </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\add_perguntas.jpg" />
</p>

<center><h3> Área de perguntas / alteração de pergunta </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\caminho_para_alterar.jpg" />
</p>

<center><h3> Área de perguntas / exclusão de pergunta </h3></center>
<p align="center">
    <img width="400" alt="image" src="imgs\perguntas_alteradas.jpg" />
</p>


## 9. Compilação, testes e execução

O projeto exige Java 19 e Maven.

```bash
cd tp1-aeds3
mvn test
mvn exec:java
```

> Caso necessário obtenha o Maven acessando o site https://maven.apache.org/download.cgi e baixando o pacote 	`apache-maven-3.9.16-bin.zip`  \
> Certifique-se que a variável de sistema `mvn` esteja configurada na máquina referenciando a pasta `bin`.

---

## 10. Trabalho Prático 2 — Relacionamento N:N

Nesta segunda etapa, o Ajuda Aí 1.0 evoluiu para suportar o relacionamento N:N entre Usuários e Perguntas por meio da entidade `Resposta`, bem como o relacionamento entre Usuários e Respostas por meio da entidade `Avaliacao`.

### 10.1 Novas funcionalidades implementadas

*   **Busca e Paginação (Pessoa 1):** Listagem das perguntas mais recentes de forma paginada (5 por página) utilizando um novo índice de Árvore B+ ordenado por data de criação decrescente.
*   **Respostas:** [A CARGO DA PESSOA 2 - Explicar brevemente o CRUD de Respostas e a Árvore B+ de relacionamento].
*   **Avaliações:** [A CARGO DA PESSOA 3 - Explicar brevemente o sistema de avaliação, notas e impedimento de voto duplo].
*   **Área Pessoal e Integração:** [A CARGO DA PESSOA 4 - Explicar a integração na área do usuário, gerenciamento de respostas recebidas e avaliações feitas].

### 10.2 Checklist Obrigatório (TP2)

*   Há uma listagem paginada de perguntas, que mostra as perguntas em ordem decrescente de data a partir de uma Árvore B+ acrescentada ao CRUD de Perguntas?
**Sim.** Foi criada a classe `ParDataId` (data de criação `long` + `idPergunta` `int`, 12 bytes fixos), cujo `compareTo` é invertido na data. Assim, a pergunta mais recente fica na primeira folha da `ArvoreBMais<ParDataId>` (arquivo `indicePerguntasData.bplus`), adicionada ao `BancoDados` junto do CRUD de perguntas. Toda pergunta criada é inserida nesse índice e, se o arquivo estiver vazio, o índice é reconstruído a partir dos registros. O método `perguntasRecentes()` percorre as folhas encadeadas da árvore (`read(null)`) e já recebe os IDs em ordem decrescente, sem ordenação em memória; depois lê cada pergunta pelo CRUD e descarta as arquivadas. No menu "Buscar perguntas → A", a listagem mostra 5 perguntas por página, com `>` (próxima), `<` (anterior), `0` (voltar) e a digitação do ID para ver os detalhes completos.

*   Há uma listagem das respostas a uma pergunta selecionada, em ordem decrescente de nota?
**[A CARGO DA PESSOA 2]** - Responder Sim/Não e justificar.

*   O usuário pode responder a uma pergunta?
**[A CARGO DA PESSOA 2]** - Responder Sim/Não e justificar.

*   O usuário pode arquivar a própria resposta?
**[A CARGO DA PESSOA 2]** - Responder Sim/Não e justificar.

*   O usuário pode avaliar uma resposta?
**[A CARGO DA PESSOA 3]** - Responder Sim/Não e justificar.

*   Na área do usuário, o usuário pode visualizar as respostas às suas perguntas?
**[A CARGO DA PESSOA 4]** - Responder Sim/Não e justificar.

*   Na área do usuário, o usuário pode arquivar uma resposta de outro usuário a uma das suas perguntas?
**[A CARGO DA PESSOA 4]** - Responder Sim/Não e justificar.

*   Na área do usuário, o usuário pode visualizar e arquivar as respostas que escreveu?
**[A CARGO DA PESSOA 4]** - Responder Sim/Não e justificar.

*   Na área do usuário, o usuário pode visualizar e arquivar a avaliação que realizou em uma resposta?
**[A CARGO DA PESSOA 4]** - Responder Sim/Não e justificar.

*   A integridade do relacionamento entre perguntas e respostas está mantida em todas as operações?
**[A CARGO DA EQUIPE / PESSOA 2]** - Responder Sim/Não e justificar.

*   A integridade do relacionamento entre respostas e avaliações está mantida em todas as operações?
**[A CARGO DA EQUIPE / PESSOA 3]** - Responder Sim/Não e justificar.

### 10.3 Vídeo de demonstração (TP2)

- **Link do Vídeo:** [INSERIR LINK DO NOVO VÍDEO AQUI QUANDO FINALIZADO]

## 11. Trabalho Prático 3 — Índice Invertido

Nós implementamos o índice invertido das palavras-chave de `Pergunta` usando as classes `aed3.ListaInvertida` e `aed3.ElementoLista` fornecidas na disciplina. Criamos `TratamentoTexto.palavrasValidas` para converter o texto para minúsculas, retirar os acentos e substituir pontuações por espaços. Em `removerStopWords`, utilizamos um `HashSet` com palavras vazias em português. Nós aplicamos esse mesmo tratamento na indexação e na consulta; palavras repetidas continuam no documento para o cálculo da frequência.

Nós concentramos a indexação em `IndicePalavrasChave`. Para cada ocorrência válida, utilizamos a contribuição `1 / total de palavras válidas`; consultamos `lista.read(termo, idPergunta)` e criamos o par quando ele não existe, ou atualizamos sua frequência somando a nova contribuição. Assim, o TF final corresponde à frequência do termo dividida pelo total de palavras válidas, sem repetir o ID na lista do termo. Nós integramos esse índice a `BancoDados.criarPergunta`, `atualizarPergunta` e `excluirPergunta`: uma inserção incrementa o total de entidades, uma alteração retira os termos antigos antes de inserir os novos, e um arquivamento ou exclusão remove as ocorrências e reduz o total. Nós consideramos como documentos as perguntas ativas, inclusive aquelas sem palavras válidas; perguntas arquivadas não entram na busca. Nós gravamos o índice em `dados/palavrasChave.dicionario.db` e `dados/palavrasChave.blocos.db` e o reconstruímos a partir dos registros existentes se faltar um desses arquivos.

Na consulta, nós usamos `IndicePalavrasChave.buscar` para recuperar `lista.read(termo)` e calcular o IDF por `Math.log10((double) lista.numeroEntidades() / ocorrencias.length)`. Nós multiplicamos o TF de cada ocorrência pelo IDF e somamos as pontuações quando termos diferentes apontam para a mesma pergunta. Retornamos os IDs em ordem decrescente de pontuação, com desempate pelo menor ID; termos repetidos na consulta são considerados uma única vez. Nós ignoramos termos sem ocorrências para evitar divisão por zero. Em `BancoDados.buscarPerguntasPorPalavrasChave`, disponibilizamos o resultado para `Menu.buscarPorPalavrasChave`, acessível em **Menu principal → Buscar perguntas → B**. Nós reaproveitamos a paginação de cinco perguntas e a abertura dos detalhes, mantendo a ordem de relevância.

### 11.1 Checklist Obrigatório (TP3)

* O índice invertido com palavras-chave foi criado usando a classe ListaInvertida?

**Sim.** Nós utilizamos a classe da disciplina em `IndicePalavrasChave`, armazenando pares de ID e TF com `ElementoLista`. Nós preservamos a autoria indicada no código original.

* É possível buscar perguntas por palavras-chave no menu de buscar perguntas?

**Sim.** Nós implementamos a opção B e verificamos o fluxo real no terminal: cadastro, login, inclusão de três perguntas, consulta `JAVA e RÁRO!` e abertura dos detalhes da pergunta retornada.

* As perguntas retornadas estão corretamente classificadas pelo modelo TFxIDF?

**Sim.** Nós somamos `TF × log10(N / df)` por ID e ordenamos por pontuação decrescente. Nós testamos frequências diferentes, a soma de termos, consultas normalizadas e desempates. Na demonstração pelo terminal, a pergunta com `java raro` apareceu antes daquela com apenas `java`.

* O trabalho compila corretamente?

**Sim.** Nós executamos `mvn test`: **BUILD SUCCESS**, com **14 testes, nenhuma falha, nenhum erro e nenhum teste ignorado**. Desses, dez são os novos testes de `IndicePalavrasChaveTest`.

* O trabalho está completo e funcionando sem erros de execução?

**Sim.** Nós testamos normalização, stop words, TF, contagem de documentos, ordenação TF×IDF, alteração de palavras-chave, arquivamento, exclusão, reabertura e reconstrução do índice, todos sem erros. Nós também executamos o fluxo completo pelo menu (inclusão, busca por palavras-chave e abertura dos detalhes) sem nenhuma exceção em tempo de execução.

* O trabalho é original e não a cópia de um trabalho de outro grupo?

**Sim.** O trabalho foi desenvolvido pela nossa equipe com base no material da disciplina. Nós utilizamos apenas as classes fornecidas pelo professor (`ListaInvertida`, `ElementoLista`, `Arquivo`, `ArvoreBMais` e `HashExtensivel`) e escrevemos as classes `TratamentoTexto`, `IndicePalavrasChave` e as alterações em `BancoDados` e `Menu`.

### 11.2 Vídeo de demonstração (TP3)

- **Link do Vídeo:** [INSERIR LINK DO VÍDEO DO TP3 AQUI]

Nós podemos demonstrar a opção **Buscar perguntas → B**, consultar palavras com acentos e mostrar a ordem dos resultados. Para explicar o código, nós mostramos `TratamentoTexto.palavrasValidas`, o trecho `read/create/update` de `IndicePalavrasChave.incluir` e o cálculo de IDF com a ordenação em `IndicePalavrasChave.buscar`.