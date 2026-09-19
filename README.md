# Relatório do Trabalho Prático 1 — AEDS III

## Sistema Ajuda Aí 1.0

### Participantes

> Preencher antes da entrega.

| Nome completo | Matrícula |
| --- | --- |
| [Ana Flávia Menezes de Almeida] | [8976988] |
| [Clarisse de Assis Pereira] | [903541] |
| [Eduardo Borges de Souza] | [890309] |
| [Rian Chaves Pimenta] | [898910] |


- **Turma:** [(8203100) Algoritmos e Estruturas de Dados III]
- **Professor:** [Marcos André Silveira Kutova]
- **Repositório:** [https://github.com/Ana-hub2000/Aeds3_TP1]
- **Vídeo de demonstração:** [INSERIR LINK]

---

## 1. Descrição do trabalho

O Ajuda Aí 1.0 é um sistema de perguntas desenvolvido em Java, com interface de texto e armazenamento em arquivos binários. Nesta primeira etapa foram trabalhados o cadastro e a autenticação de usuários, a recuperação de senha e o gerenciamento das perguntas do usuário que está conectado.

Depois do login, o usuário pode consultar e alterar seus dados, incluir perguntas, listar suas perguntas ativas, alterar o texto e as palavras-chave de uma pergunta e arquivá-la. As áreas de respostas, votos e busca global aparecem no menu, mas foram deixadas para etapas posteriores, conforme previsto no enunciado.

Os dados não dependem de um banco de dados externo. Usuários, perguntas e índices são gravados em arquivos no diretório `data`. Quando o programa é iniciado, esses arquivos são abertos e os índices são reconstruídos a partir dos registros válidos.

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

---

## 3. Organização das classes

### Inicialização e interface

- **`Main`**: define o diretório dos dados, abre o banco e inicia o menu.
- **`App`**: ponto de entrada mantido para executar `Main` pelo Maven.
- **`Menu`**: controla as telas, lê as opções e coordena cadastro, login, recuperação, edição de dados e gestão de perguntas.

### Entidades

- **`Usuario`**: representa o usuário e implementa `aed3.InterfaceRegistro` (`serialize`/`deserialize`, `getId`/`setId`).
- **`Pergunta`**: representa a pergunta, seus metadados, seu autor e seu estado de arquivamento; também implementa `aed3.InterfaceRegistro`.

### Persistência e índices (classes do professor, pacote `aed3`)

- **`Arquivo<T>`**: CRUD fornecido na disciplina. Grava lápide, indicador de tamanho em `short` e o vetor de bytes, e mantém o índice direto ID → endereço em uma **Tabela Hash Extensível** (`HashExtensivel<ParIDEndereco>`).
- **`HashExtensivel<T>`**: tabela hash extensível com diretório e cestos gravados em disco.
- **`ArvoreBMais<T>`**: Árvore B+ real (páginas, divisão de páginas e ligação entre folhas).
- **`ParIdId`**: par `(idUsuario, idPergunta)` guardado na Árvore B+ do relacionamento 1:N.
- **`ParEmailId`** (`br.pucminas.persistence`): adaptação do `ParNomeId` do professor, com campo de 60 bytes, usada como índice indireto e-mail → ID do usuário na Árvore B+.
- **`BancoDados`**: concentra os dois arquivos de dados, os índices e as regras que envolvem mais de um arquivo.

A única alteração feita nas classes do professor foi um construtor extra em `Arquivo`, que recebe a pasta base dos dados (o original grava sempre em `./dados`). Isso foi necessário para os testes automatizados, que usam pasta temporária; o construtor original continua existindo e com o mesmo comportamento.

### Segurança

- **`Seguranca`**: calcula hashes SHA-256, normaliza respostas secretas e compara a senha informada com o hash gravado.

### Testes

- **`BancoDadosTest`** (em `src/test/java`): persistência de usuários e perguntas, alteração, arquivamento, listagem com e sem arquivadas, recusa de pergunta com usuário inexistente, carga de 25 perguntas para forçar a divisão de páginas da Árvore B+ e reabertura do banco.
- **`AppTest`**: teste básico gerado pelo modelo inicial do Maven.

---

## 4. Estrutura dos arquivos

As entidades convertem seus campos para vetores de bytes com `DataOutputStream` e fazem o caminho inverso com `DataInputStream`.

Cada registro no arquivo de dados contém:

1. uma lápide de um byte, que informa se o registro está ativo ou excluído;
2. o tamanho do vetor de bytes gravado em `short` (`writeShort` / `readUnsignedShort`);
3. o vetor de bytes produzido pela própria entidade.

O `Arquivo` do professor ainda reaproveita espaço: registros excluídos entram em uma lista de espaços livres e podem ser reutilizados por novos registros do mesmo tamanho.

Os arquivos ficam em `dados/usuarios/`, `dados/perguntas/` (dados + índice direto em hash extensível) e em `dados/indiceEmail.bplus` e `dados/indicePerguntas.bplus` (árvores B+).

## 5. Índices e relacionamento 1:N

### Índice direto por ID

É o `HashExtensivel<ParIDEndereco>` que já vem dentro da classe `Arquivo`: a chave é o ID da entidade e o valor é o endereço do registro no arquivo, o que evita percorrer o arquivo em cada leitura.

### Índice de e-mail

Índice indireto em Árvore B+ de pares `(email, idUsuario)`, usando a classe `ParEmailId`. O e-mail é normalizado (sem espaços, em minúsculas) antes de entrar na árvore. Quando o usuário troca o e-mail, o par antigo é removido e o novo é inserido.

### Relacionamento usuário–perguntas

O vínculo 1:N usa a Árvore B+ de pares `(idUsuario, idPergunta)` (`ParIdId`). Como o `compareTo` do `ParIdId` ignora o segundo ID quando ele vale `-1`, a busca por `new ParIdId(idUsuario)` devolve de uma vez todas as perguntas daquele usuário.

---

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

---

## 7. Checklist obrigatório

Respostas conferidas após as correções, com `mvn test` executado em **19/09/2026** (`Tests run: 4, Failures: 0, Errors: 0` e `BUILD SUCCESS`).

### Há um CRUD de usuários (que estende a classe Arquivo, acrescentando Tabelas Hash Extensíveis e Árvores B+ como índices diretos e indiretos conforme necessidade) que funciona corretamente?

**Sim.** Os usuários são gravados pela classe `Arquivo` do professor, que já traz o índice direto em tabela hash extensível (ID → endereço). O índice indireto por e-mail é uma Árvore B+ de pares `(email, idUsuario)`.

### Há um CRUD de perguntas (que estende a classe Arquivo, acrescentando Tabelas Hash Extensíveis e Árvores B+ como índices diretos e indiretos conforme necessidade) que funciona corretamente?

**Sim.** As perguntas usam o mesmo `Arquivo` (com hash extensível por ID) e uma Árvore B+ com os pares `(idUsuario, idPergunta)`. A listagem mostra ao autor também as perguntas arquivadas.

### As perguntas estão vinculadas aos usuários usando o idUsuario como chave estrangeira?

**Sim.** A entidade `Pergunta` guarda o `idUsuario` e `BancoDados.criarPergunta` verifica se esse usuário existe antes de gravar.

### Há uma árvore B+ que registre o relacionamento 1:N entre usuários e perguntas?

**Sim.** É a `ArvoreBMais<ParIdId>` fornecida pelo professor, gravada em `dados/indicePerguntas.bplus`. O teste com 25 perguntas do mesmo usuário passa pela divisão de páginas e continua recuperando todos os registros.

### O trabalho compila corretamente?

**Sim.** `mvn test` termina com `BUILD SUCCESS`.

### O trabalho está completo e funcionando sem erros de execução?

**Sim, para o escopo do TP1.** Cadastro, login, recuperação de senha, alteração de dados e gestão de perguntas funcionam. Busca global, respostas e votos são etapas seguintes e não fazem parte deste trabalho.

### O trabalho é original e não a cópia de um trabalho de outro grupo?

**[RESPONDER PELOS INTEGRANTES: SIM/NÃO].** Essa informação não pode ser confirmada somente pela leitura do repositório.

---

## 8. Pendências antes da entrega

1. Preencher nomes, matrículas, turma, URL do GitHub e link do vídeo.
2. Inserir as capturas indicadas na seção seguinte.
3. Confirmar com o professor se o e-mail pode ficar limitado a 60 bytes no índice (limite do registro de tamanho fixo da Árvore B+).


---

## 9. Capturas de tela para o relatório

Substituir cada marcação por uma imagem da execução real:

### Tela inicial // EDUARDO

`[INSERIR CAPTURA DA TELA DE LOGIN/NOVO USUÁRIO]`

### Cadastro concluído// EDUARDO

`[INSERIR CAPTURA DO CADASTRO DE UM NOVO USUÁRIO]`

### Recuperação de senha// EDUARDO

`[INSERIR CAPTURA DO LOGIN FALHANDO E DA RECUPERAÇÃO]`

### Menu principal (Clarisse de Assis)
Após realizar o login, o usuário é direcionado ao menu principal do sistema Ajuda Aí 1.0. Nesse menu, são disponibilizadas as opções de acesso à área do usuário, busca de perguntas e encerramento do sistema.

A opção Minha área permite acessar funcionalidades relacionadas ao usuário conectado, incluindo seus dados e suas perguntas. A opção Buscar perguntas está presente no menu, porém a busca global não faz parte do escopo implementado nesta etapa do trabalho.

<img width="276" height="195" alt="image" src="https://github.com/user-attachments/assets/d3712a4d-33d7-4620-90a7-0c0a352ea861" />
Figura 4 – Menu principal do sistema após o login.


### Alteração de e-mail (Ana Flávia Menezes)

`[INSERIR CAPTURA DA ALTERAÇÃO DO E-MAIL]`

### Gestão de perguntas (Ana Flávia Menezes)

`[INSERIR CAPTURA DA INCLUSÃO E DA LISTAGEM]`

`[INSERIR CAPTURA DA ALTERAÇÃO DE UMA PERGUNTA]`

`[INSERIR CAPTURA DO ARQUIVAMENTO]`

---

## 10. Roteiro sugerido para o vídeo

O vídeo deve ter no máximo cinco minutos. Uma sequência possível é:

1. apresentar rapidamente o objetivo do Ajuda Aí 1.0;
2. cadastrar um novo usuário;
3. tentar entrar com uma senha incorreta;
4. escolher a recuperação, responder à pergunta secreta e definir uma nova senha;
5. fazer o login corretamente;
6. alterar o e-mail e explicar `Menu.meusDados` e `BancoDados.atualizarUsuario`, destacando a atualização do índice de e-mail na Árvore B+;
7. incluir uma pergunta e mostrar que o autor é associado automaticamente;
8. listar as perguntas do usuário;
9. alterar o texto e as palavras-chave de uma pergunta;
10. arquivar a pergunta e explicar `Menu.arquivarPergunta`, o campo `ativa` e a atualização feita pelo CRUD;
11. encerrar indicando onde estão as classes principais no repositório.

---

## 11. Compilação, testes e execução

O projeto exige Java 19 e Maven.

```bash
cd tp1-aeds3
mvn test
mvn exec:java
```

A última execução de `mvn test` terminou com `Tests run: 4, Failures: 0, Errors: 0` e `BUILD SUCCESS`. Os dados ficam na pasta `dados/` (pode ser trocada com `-Dajudaai.data=...`).
