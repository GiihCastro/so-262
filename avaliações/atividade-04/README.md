# Atividade 04 — Laboratório de Gerência do Processador no SOsim

**Aluna:** Giovanna Castro  
**Disciplina:** Sistemas Operacionais (SO-262)

---

## 1. Visão geral

Esta atividade utiliza o **SOsim** para observar o comportamento de processos durante o escalonamento da CPU.

Foram comparados processos de dois tipos:

- **CPU-bound:** utiliza a CPU por períodos mais longos e realiza poucas operações de entrada e saída.
- **I/O-bound:** utiliza a CPU por períodos curtos e passa mais tempo aguardando operações de entrada e saída.

Durante os exercícios, foram analisados o escalonamento circular, o uso de prioridades, a alteração do quantum e o mecanismo de prioridade dinâmica.

---

## 2. Exercício 1 — Escalonamento circular

### 2.1 Configuração

- Escalonamento circular sem prioridade.
- Um processo CPU-bound.
- Um processo I/O-bound.
- Os dois processos foram criados com a mesma prioridade.

### 2.2 Resultado observado

| Processo | Prioridade | Tempo de CPU |
| --- | ---: | ---: |
| CPU-bound | 0 | 84 |
| I/O-bound | 0 | 15 |

Após três minutos, o processo CPU-bound utilizou a CPU mais vezes, mesmo com os dois processos na mesma prioridade.

Isso acontece porque o processo I/O-bound cede o processador voluntariamente quando precisa realizar uma operação de entrada ou saída. Nesse momento, ele passa para o estado de **Espera/Bloqueado**, enquanto o CPU-bound continua pronto para utilizar a CPU.

### 2.3 Pergunta

**O que acontece se o tempo de time slice (quantum) aumentar ou diminuir?**

Se o quantum aumentar, ocorrerão menos trocas de contexto e o escalonamento ficará mais próximo do FCFS. Porém, o processo CPU-bound poderá permanecer mais tempo na CPU, prejudicando o tempo de resposta do I/O-bound.

Se o quantum diminuir, o processo I/O-bound poderá voltar à CPU mais rapidamente. Em compensação, haverá mais trocas de contexto, aumentando a sobrecarga do sistema.

---

## 3. Exercício 2 — Escalonamento circular com prioridades

### 3.1 Configuração

- Escalonamento circular com prioridades.
- Processo CPU-bound com prioridade 3.
- Processo I/O-bound com prioridade 4.

### 3.2 Resultado observado

| Processo | Prioridade | Tempo de CPU |
| --- | ---: | ---: |
| CPU-bound | 3 | 96 |
| I/O-bound | 4 | 19 |

A distribuição do tempo continuou semelhante à observada no primeiro exercício.

Mesmo com prioridade maior, o processo I/O-bound passa boa parte da execução no estado de **Espera/Bloqueado**. Enquanto ele aguarda a conclusão da operação de entrada ou saída, o CPU-bound permanece pronto e utiliza o processador.

### 3.3 Pergunta

**O que acontece se o tempo de espera do processo I/O-bound aumentar ou diminuir?**

Se o tempo de espera aumentar, o processo I/O-bound permanecerá bloqueado por mais tempo e utilizará a CPU com menos frequência. Com isso, o processador ficará disponível por mais tempo para o CPU-bound.

Se o tempo de espera diminuir, o processo I/O-bound retornará ao estado de Pronto mais rapidamente. Como ele utiliza a CPU em períodos curtos, voltará para I/O logo depois, mas será atendido com mais frequência.

---

## 4. Exercício 3 — Prioridade maior para o processo CPU-bound

### 4.1 Configuração

- Escalonamento circular com prioridades.
- Processo CPU-bound com prioridade 4.
- Processo I/O-bound com prioridade 3.

### 4.2 Resultado observado

| Processo | Prioridade | Tempo de CPU |
| --- | ---: | ---: |
| CPU-bound | 4 | 92 |
| I/O-bound | 3 | 0 |

O processo I/O-bound permaneceu com o tempo de CPU zerado, enquanto o CPU-bound monopolizou o processador.

Como o CPU-bound possuía prioridade maior e não precisava interromper sua execução para realizar operações de entrada ou saída, ele continuou sendo selecionado. O processo I/O-bound permaneceu na fila de Pronto sem conseguir executar.

Essa situação representa um caso de **starvation**, também chamado de inanição.

### 4.3 Pergunta

**Quais devem ser os critérios para determinar as prioridades de processos?**

Os principais critérios são:

- **Perfil do processo:** processos I/O-bound podem receber prioridade maior para manter a resposta do sistema ao usuário.
- **Importância e urgência:** tarefas críticas ou de tempo real podem precisar de prioridade maior.
- **Política do sistema:** serviços essenciais e processos administrativos podem receber tratamento diferente.
- **Tempo de espera:** processos que permanecem muito tempo na fila devem receber algum tipo de favorecimento.
- **Prevenção de starvation:** a prioridade pode ser aumentada gradualmente para evitar que um processo espere indefinidamente.

---

## 5. Exercício 4 — Prioridade dinâmica

### 5.1 Configuração

- Escalonamento com prioridade dinâmica.
- Um processo CPU-bound.
- Um processo I/O-bound.
- Os dois processos foram criados com a mesma prioridade inicial.

### 5.2 Resultado observado

| Processo | Prioridade inicial | Tempo de CPU |
| --- | ---: | ---: |
| CPU-bound | 0 | 106 |
| I/O-bound | 0 | 22 |

O resultado ficou parecido com o observado no segundo exercício. O processo I/O-bound continuou passando boa parte do tempo bloqueado, enquanto o CPU-bound utilizou a CPU com maior frequência.

A principal diferença foi o ajuste automático das prioridades durante a execução. Nesse caso, a prioridade não precisou ser definida manualmente para cada processo.

### 5.3 Pergunta

**Qual a vantagem desse escalonamento em processos I/O-bound de perfis diferentes?**

A principal vantagem é a capacidade de adaptação.

Processos I/O-bound podem apresentar comportamentos diferentes. Alguns realizam operações de entrada e saída com mais frequência, enquanto outros permanecem bloqueados por períodos maiores.

Com a prioridade dinâmica, o sistema pode ajustar o atendimento de acordo com o comportamento de cada processo. Dessa forma, processos que utilizam a CPU por pouco tempo e retornam rapidamente para I/O podem receber prioridade maior quando ficam prontos novamente.

Isso ajuda a manter a responsividade do sistema sem exigir alterações manuais do usuário ou do administrador.

---

## 6. Comparação dos resultados

| Exercício | Escalonamento | CPU-bound | I/O-bound | Principal observação |
| --- | --- | ---: | ---: | --- |
| 1 | Circular sem prioridade | 84 | 15 | O I/O-bound permanece mais tempo bloqueado. |
| 2 | Circular com prioridade maior para o I/O-bound | 96 | 19 | A prioridade não produz efeito enquanto o processo está bloqueado. |
| 3 | Prioridade maior para o CPU-bound | 92 | 0 | O processo I/O-bound sofre starvation. |
| 4 | Prioridade dinâmica | 106 | 22 | As prioridades são ajustadas durante a execução. |

---

## 7. Conclusão

Os testes mostraram que a distribuição da CPU não depende apenas da prioridade. O comportamento de cada processo também influencia diretamente o resultado.

Processos CPU-bound permanecem prontos por mais tempo e utilizam a CPU de maneira contínua. Já os processos I/O-bound alternam entre execução e espera, pois dependem da conclusão de operações de entrada e saída.

O uso de prioridades fixas pode causar starvation quando um processo de maior prioridade permanece sempre pronto. A prioridade dinâmica ajuda a reduzir esse problema, pois permite que o sistema adapte o escalonamento ao comportamento dos processos.
