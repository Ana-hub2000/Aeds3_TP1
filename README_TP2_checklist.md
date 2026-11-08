# Checklist TP2

**Há uma listagem paginada de perguntas, que mostra as perguntas em ordem decrescente de data a partir de uma Árvore B+ acrescentada ao CRUD de Perguntas?**
**Sim.** Foi criada a classe `ParDataId` (data de criação `long` + `idPergunta` `int`, 12 bytes fixos), cujo `compareTo` é invertido na data. Assim, a pergunta mais recente fica na primeira folha da `ArvoreBMais<ParDataId>` (arquivo `indicePerguntasData.bplus`), adicionada ao `BancoDados` junto do CRUD de perguntas. Toda pergunta criada é inserida nesse índice e, se o arquivo estiver vazio, o índice é reconstruído a partir dos registros. O método `perguntasRecentes()` percorre as folhas encadeadas da árvore (`read(null)`) e já recebe os IDs em ordem decrescente, sem ordenação em memória; depois lê cada pergunta pelo CRUD e descarta as arquivadas. No menu "Buscar perguntas → A", a listagem mostra 5 perguntas por página, com `>` (próxima), `<` (anterior), `0` (voltar) e a digitação do ID para ver os detalhes completos.

**[Item 2 do checklist]**
A cargo da Pessoa 2.

**[Item 3 do checklist]**
A cargo da Pessoa 3.

**[Item 4 do checklist]**
A cargo da Pessoa 4.
