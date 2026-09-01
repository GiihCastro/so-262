# Atividade 03 — Especificação do Gerenciamento de Processos

**Aluno:** Eduardo Monteiro Oliveira e Giovanna Bandeira De Castro
**Disciplina:** Sistemas Operacionais (SO-262)

---

## 1. Visão Geral e Arquitetura do Simulador

### 1.1 Contexto

O simulador é um programa que roda inteiramente em **modo usuário** e imita,
em software, o comportamento de um núcleo de sistema operacional no que diz
respeito ao gerenciamento de processos. Ele não interage com hardware real
nem com o escalonador do SO hospedeiro: toda "CPU", toda "interrupção de
relógio" e toda "operação de E/S" são estruturas de dados e eventos
simulados dentro do próprio programa.

O propósito é permitir observar, de forma determinística e
reproduzível, o ciclo pronto → execução → bloqueado → pronto de vários
processos concorrentes, sob diferentes políticas de escalonamento.

### 1.2 Fluxo geral de execução

1. O simulador lê um **arquivo de tarefas** (ver Seção 5) descrevendo os
   processos a criar e suas sequências de rajadas de CPU/E-S.
2. O **núcleo simulado** cria os processos (evento equivalente a `fork`),
   inicializando o PCB de cada um e inserindo-os na fila de prontos.
3. Um laço principal avança o **relógio lógico** em incrementos discretos
   (*ticks*). A cada tick:
   - o processo em execução (se houver) consome uma unidade de sua rajada
     de CPU atual;
   - o núcleo verifica se ocorreu algum evento (quantum expirado, rajada de
     CPU concluída, E/S concluída, término de processo);
   - o **escalonador** é acionado sempre que a CPU fica ociosa ou um evento
     de reescalonamento ocorre, escolhendo o próximo processo pronto para
     ocupar a CPU.
4. O laço termina quando todos os processos alcançam o estado **Terminado**.
5. Ao final, o simulador emite os relatórios descritos na Seção 5.

### 1.3 Hardware simulado

Para que o simulador seja independente de qualquer arquitetura real, define-se
uma **CPU virtual** mínima:

| Componente | Descrição |
| --- | --- |
| Registradores gerais | Vetor de N inteiros (ex.: `R0..R7`), sem semântica de instrução — servem apenas para serem salvos/restaurados na troca de contexto. |
| Contador de Programa (PC) | Inteiro representando a posição simulada de execução do processo. |
| Relógio lógico (`clock`) | Contador global de ticks, incrementado uma vez por iteração do laço principal; é a base de tempo de todo o simulador. |
| Interrupção de relógio | Evento disparado pelo núcleo quando `clock` atinge o fim do quantum do processo corrente. |

Não há memória física simulada; o foco é exclusivamente o
gerenciamento de processos e threads, não a gerência de memória.

### 1.4 Relação com threads (Cap. 2, seção 2.2 de Tanenbaum)

Tanenbaum trata processos e threads no mesmo capítulo porque uma thread é,
essencialmente, uma unidade de execução que compartilha o espaço de
endereçamento (e demais recursos) de um processo, mas mantém sua própria
pilha e seu próprio contexto de CPU (registradores + PC). Nesta primeira
versão do simulador, **cada processo simulado é tratado como
single-threaded**: o PCB descrito na Seção 2 já contém tudo o que, no modelo
de Tanenbaum, pertenceria à *única* thread daquele processo (registradores,
PC, estado).

Para manter o simulador extensível ao modelo multithread sem redesenhar o
núcleo, a especificação adota a seguinte convenção:

- O `programa` (sequência de rajadas de CPU/E-S) e o contexto salvo
  (`registradores`, PC) ficam logicamente associados a uma **thread**, não
  ao processo como um todo — nesta versão, simplesmente há uma thread por
  processo, então o PCB os representa juntos sem distinção explícita de
  campos.
- Uma extensão multithread futura bastaria "particionar" o PCB em (a) um
  bloco de processo (recursos compartilhados: `pid`, tabela de processos,
  prioridade herdada) e (b) um ou mais blocos de thread (estado,
  registradores, PC, `programa`, tempos de CPU/espera individuais) — o
  escalonador (Seção 4) passaria a escalonar threads, e não processos,
  seguindo o mesmo grafo de estados da Seção 3.
- Essa separação **não é implementada** nesta atividade; fica registrada
  aqui apenas para deixar explícito que o modelo de PCB foi desenhado de
  forma compatível com o tratamento processo/thread do Capítulo 2, e não
  apenas com o modelo de processo isolado.

---

## 2. Especificação do Bloco de Controle de Processo (PCB) e Tabela de Processos

### 2.1 Estrutura do PCB

Cada processo é representado por um **Bloco de Controle de Processo (PCB)**
com, no mínimo, os seguintes campos:

| Campo | Tipo | Descrição |
| --- | --- | --- |
| `pid` | inteiro | Identificador único do processo, atribuído na criação. |
| `estado` | enum | Um de `NOVO`, `PRONTO`, `EXECUTANDO`, `BLOQUEADO`, `TERMINADO` (ver Seção 3). |
| `registradores` | vetor de inteiros | Cópia do contexto da CPU virtual (registradores + PC) salva na última troca de contexto. |
| `prioridade` | inteiro | Usada pelo escalonador por prioridades; menor valor = maior prioridade (convenção a documentar no código). |
| `prioridade_base` | inteiro | Prioridade original, preservada para permitir o *aging* (ver Seção 4.2) sem perder o valor de referência. |
| `tempo_cpu_total` | inteiro | Soma de ticks em que o processo esteve no estado Executando. |
| `tempo_espera_total` | inteiro | Soma de ticks em que o processo esteve no estado Pronto (aguardando CPU). |
| `tempo_chegada` | inteiro | Valor do relógio lógico no instante da criação do processo. |
| `tempo_termino` | inteiro | Valor do relógio lógico no instante em que o processo termina (`-1` enquanto não terminado). |
| `programa` | lista de rajadas | Sequência de pares (tipo de rajada, duração) lida do arquivo de tarefas — ex.: `CPU 5`, `E/S 3`, `CPU 2`, ... |
| `indice_rajada_atual` | inteiro | Posição corrente dentro de `programa`. |
| `ticks_restantes_rajada` | inteiro | Quanto falta da rajada corrente (de CPU ou de E/S). |

### 2.2 Tabela de processos

O núcleo mantém uma **tabela de processos**: uma coleção indexada por `pid`
contendo todos os PCBs conhecidos pelo simulador, independentemente do
estado. A tabela é a fonte única de verdade sobre a existência de um
processo; as filas do escalonador (pronto, bloqueado) armazenam apenas
referências (ex.: `pid`) para entradas dessa tabela, nunca cópias do PCB —
evitando duas versões divergentes do mesmo processo.

Operações mínimas exigidas sobre a tabela:

- `criar_processo(programa, prioridade) -> pid`
- `obter_pcb(pid) -> PCB`
- `remover_processo(pid)` (chamada após o processo entrar em `TERMINADO` e
  seus dados finais serem consolidados nas estatísticas de saída)
- `listar_processos(estado?) -> lista de pid` (com filtro opcional por estado,
  usado tanto pelo escalonador quanto pelos relatórios)

---

## 3. Ciclo de Vida e Grafo de Transição de Estados

### 3.1 Estados

O simulador modela três estados operacionais principais — **Pronto**,
**Em Execução** e **Bloqueado** — mais dois estados de borda que delimitam o
ciclo de vida: **Novo** (antes da admissão) e **Terminado** (após o `exit`).

```
        criar_processo (fork)
   NOVO ───────────────────────► PRONTO
                                    │  ▲
                    escalonador     │  │ E/S concluída
                    escolhe o       │  │ (interrupção de E/S)
                    processo        ▼  │
                              EM EXECUÇÃO
                                 │  │  │
   quantum expirado ─────────────┘  │  └────────────► BLOQUEADO
   (interrupção de relógio)         │   solicitação de E/S
                                     │
                                     ▼
                                TERMINADO
                              (chamada exit)
```

### 3.2 Transições especificadas

| # | Transição | Gatilho | Efeito sobre o PCB |
| --- | --- | --- | --- |
| T1 | Novo → Pronto | `criar_processo` (equivalente a `fork`) é processado pelo núcleo. | PCB é alocado, `estado = PRONTO`, `tempo_chegada = clock`, processo é inserido na fila de prontos do escalonador. |
| T2 | Pronto → Em Execução | O escalonador seleciona o processo (despacho). | `estado = EXECUCAO`; contexto salvo do processo anterior é trocado pelo contexto do novo processo (troca de contexto); quantum é (re)armado se o algoritmo for Round Robin. |
| T3 | Em Execução → Pronto | Interrupção periódica de relógio: quantum expira antes da rajada de CPU terminar. | Contexto (registradores + PC) é salvo no PCB; `estado = PRONTO`; processo volta ao fim da fila circular (Round Robin) ou é reinserido conforme sua prioridade. |
| T4 | Em Execução → Bloqueado | Processo emite uma solicitação fictícia de E/S (a rajada de CPU corrente termina e a próxima rajada do `programa` é de E/S). | Contexto é salvo; `estado = BLOQUEADO`; processo é movido para a fila de espera do dispositivo de E/S simulado; `ticks_restantes_rajada` é ajustado para a duração da rajada de E/S. |
| T5 | Bloqueado → Pronto | E/S concluída: `ticks_restantes_rajada` chega a zero para uma rajada de E/S. | `estado = PRONTO`; processo é reinserido na fila de prontos; avança-se `indice_rajada_atual` para a próxima rajada (de CPU). |
| T6 | Em Execução → Terminado | Chamada `exit`: a última rajada do `programa` (de CPU) é concluída. | `estado = TERMINADO`; `tempo_termino = clock`; processo é removido da disputa por CPU; estatísticas finais são consolidadas. |

Todas as transições são disparadas pelo núcleo simulado dentro do mesmo laço
de ticks descrito na Seção 1.2 — não há concorrência real entre "processos":
a concorrência é simulada por meio da alternância determinística de contexto
sob controle do escalonador.

---

## 4. Especificação do Escalonador de CPU

O simulador deve suportar, de forma **intercambiável** (isto é, selecionável
por parâmetro de configuração/linha de comando, sem alterar o restante do
núcleo), pelo menos os dois algoritmos a seguir. Ambos devem implementar uma
interface comum, por exemplo:

```
Escalonador {
    adicionar(pid)          // insere um processo pronto na estrutura do escalonador
    proximo() -> pid        // escolhe o próximo processo a executar
    notificar_tick()        // permite ao escalonador reagir a cada tick (ex.: aging, quantum)
}
```

### 4.1 Round Robin (Circular)

- Fila de prontos organizada como **fila circular** (FIFO com reinserção no
  final).
- Parâmetro configurável: `quantum` (em ticks).
- A cada despacho (T2), arma-se um contador de quantum para o processo em
  execução.
- A cada tick em que o processo está em execução, o contador de quantum é
  decrementado.
- Se o contador de quantum chegar a zero **antes** de a rajada de CPU atual
  terminar, ocorre a transição T3 (interrupção de relógio) e o processo é
  reinserido no **final** da fila circular.
- Se a rajada de CPU terminar **antes** do quantum expirar, a transição
  aplicável é T4 (se a próxima rajada for E/S) ou T6 (se for a última rajada).

### 4.2 Prioridades (Estáticas ou Dinâmicas) com prevenção de inanição

- Fila de prontos organizada como fila de prioridade (menor valor numérico =
  maior prioridade, ou o inverso — a convenção deve ser documentada e
  consistente em todo o código).
- **Modo estático:** `prioridade` é definida na criação do processo e nunca
  muda por conta própria do escalonador.
- **Modo dinâmico com envelhecimento (*aging*):** a cada N ticks em que um
  processo permanece no estado Pronto sem ser escolhido, sua `prioridade` é
  incrementada (melhorada) em um passo configurável, até no máximo igualar a
  prioridade mais alta em disputa. Isso evita **inanição (starvation)** de
  processos de baixa prioridade original.
- Quando o processo finalmente é despachado, sua `prioridade` deve ser
  restaurada a partir de `prioridade_base` (para não acumular vantagem
  indefinidamente entre diferentes execuções na CPU) — este comportamento
  deve ser explicitado na implementação.
- Em caso de empate de prioridade, desempate por ordem de chegada
  (`tempo_chegada`), garantindo determinismo na simulação.

### 4.3 Requisito de intercambialidade

O núcleo do simulador deve depender apenas da interface comum do
escalonador, nunca de detalhes internos de um algoritmo específico. A troca
de algoritmo (ex.: `--escalonador=rr --quantum=4` vs.
`--escalonador=prioridade --aging=10`) não deve exigir alteração em nenhum
outro módulo (PCB, tabela de processos, laço principal, geração de
relatórios).

---

## 5. Entradas, Casos de Teste e Diretrizes de Entrega

### 5.1 Arquivo de tarefas (entrada)

O simulador lê um arquivo de texto descrevendo os processos a simular. Cada
linha representa um processo e sua sequência alternada de rajadas de
CPU/E-S. Formato proposto:

```
# pid_sugerido  prioridade  chegada  rajadas("CPU:n" ou "IO:n", alternadas, terminando em CPU)
P1  2  0  CPU:5 IO:3 CPU:2
P2  1  0  CPU:3 IO:2 CPU:4 IO:1 CPU:2
P3  3  2  CPU:8
```

Regras de leitura:

- Linhas iniciadas por `#` são comentários e devem ser ignoradas.
- A sequência de rajadas de um processo deve sempre começar e terminar com
  uma rajada de CPU (um processo que só faz E/S não tem sentido no modelo).
- `chegada` é o tick do relógio lógico em que o processo deve ser criado
  (permite testar admissão de processos em instantes diferentes, não só em
  `t=0`).

### 5.2 Casos de teste mínimos exigidos

1. **Processo único, sem E/S:** valida a transição direta
   Novo → Pronto → Execução → Terminado (T1, T2, T6).
2. **Dois processos disputando CPU sob Round Robin:** valida a fila circular
   e a interrupção por quantum (T3).
3. **Processo com múltiplas rajadas de E/S:** valida as transições T4/T5
   repetidamente e a consistência do PCB (`indice_rajada_atual`,
   `ticks_restantes_rajada`).
4. **Cenário de inanição sob prioridades estáticas** (um processo de baixa
   prioridade nunca é escolhido) **vs. o mesmo cenário sob prioridades
   dinâmicas com aging**, comparando `tempo_espera_total` do processo de
   baixa prioridade nos dois modos — deve demonstrar que o aging reduz a
   espera.
5. **Chegada tardia de processos** (`chegada > 0`): valida que o escalonador
   só considera um processo elegível a partir do tick de sua chegada.

### 5.3 Saída do simulador

O simulador deve produzir, ao final da execução:

- **Gráfico de Gantt textual**, mostrando, tick a tick (ou por intervalo
  contíguo de um mesmo processo, para compactar a saída), qual processo
  ocupou a CPU:

  ```
  | P1 | P1 | P1 | P2 | P2 | P1 | ... |
  0    1    2    3    4    5    6
  ```

- **Log de transições de estado**, uma linha por evento, no formato:

  ```
  [tick=7] P2: EXECUCAO -> BLOQUEADO (solicitacao de E/S, duracao=3)
  [tick=10] P2: BLOQUEADO -> PRONTO (E/S concluida)
  ```

- **Estatísticas de uso de CPU**, por processo e agregadas:
  - tempo de retorno (*turnaround time* = `tempo_termino - tempo_chegada`);
  - `tempo_espera_total`;
  - `tempo_cpu_total`;
  - utilização da CPU no período simulado (percentual de ticks com algum
    processo em execução);
  - throughput (processos terminados por unidade de tempo simulado).
