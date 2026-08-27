# Atividade 02 — Produtor-Consumidor em Java

**Aluno:** Giovanna Bandeira de Castro
**Disciplina:** Sistemas Operacionais (SO-262)

## Objetivo

Evidenciar a execução do programa Produtor-Consumidor (problema do buffer limitado)
em Java, disponibilizado nos Recursos da disciplina, com o nome do aluno aparecendo
na saída do programa.

## Descrição do problema

Duas threads compartilham um buffer de tamanho fixo (`BUFFER_SIZE = 3`):

- o **produtor** gera itens (objetos `Date`) e os insere no buffer;
- o **consumidor** retira itens do buffer.

Quando o buffer está cheio, o produtor precisa esperar; quando está vazio, o
consumidor precisa esperar. Nesta versão a espera é feita por *busy waiting*
(laço `while`), sem primitivas de sincronização — a implementação **não é
thread-safe**, o que é intencional no material original (Silberschatz, Galvin e
Gagne, *Operating System Concepts with Java*), pois serve de base para a
discussão de sincronização em Java.

## Arquivos

| Arquivo | Descrição |
| --- | --- |
| `Buffer.java` | Interface do buffer, com as operações `insert` e `remove`. |
| `BoundedBuffer.java` | Implementação do buffer limitado em memória compartilhada. |
| `Producer.java` | Thread produtora: gera itens e insere no buffer. |
| `Consumer.java` | Thread consumidora: retira itens do buffer. |
| `SleepUtilities.java` | Utilitários para fazer as threads dormirem. |
| `Factory.java` | Classe principal: cria o buffer e inicia as duas threads. |

## Modificações feitas no código original

- `Factory.java`: constante `ALUNO` com o nome do aluno, impressão de um
  cabeçalho de identificação no início e de um rodapé ao final da execução.
- `Factory.java`: as threads passaram a ser *daemon* e a `main` aguarda 30
  segundos antes de encerrar, para que o programa termine sozinho em vez de
  rodar em laço infinito — facilitando a captura da tela.
- `Producer.java`, `Consumer.java` e `BoundedBuffer.java`: todas as mensagens
  impressas passaram a ser prefixadas com `[Giovanna Bandeira de Castro]`.
- `SleepUtilities.java`: o método `nap(int duration)` ignorava o parâmetro
  recebido e usava sempre a constante `NAP_TIME`; foi corrigido para usar
  `duration`. Também foi acrescentado o método `sleep(int duration)`, que dorme
  por um tempo fixo (o `nap` dorme por um tempo aleatório).

## Como compilar e executar

```bash
javac *.java
java Factory
```

## Saída esperada

```
=========================================
 Produtor-Consumidor (Bounded Buffer)
 Aluno: Giovanna Bandeira de Castro
 Disciplina: Sistemas Operacionais - SO-262
 Atividade 02
=========================================
[Giovanna Bandeira de Castro] Producer napping
[Giovanna Bandeira de Castro] Consumer napping
[Giovanna Bandeira de Castro] Producer produced Wed Aug 26 21:05:43 GFT 2026
[Giovanna Bandeira de Castro] Producer Entered Wed Aug 26 21:05:43 GFT 2026 Buffer Size = 1
[Giovanna Bandeira de Castro] Consumer wants to consume.
[Giovanna Bandeira de Castro] Consumer Consumed Wed Aug 26 21:05:43 GFT 2026 Buffer EMPTY
...
[Giovanna Bandeira de Castro] Producer Entered Wed Aug 26 21:05:56 GFT 2026 Buffer FULL
...
=========================================
 Fim da execucao - Giovanna Bandeira de Castro
=========================================
```

As mensagens `Buffer FULL` e `Buffer EMPTY` evidenciam os dois casos-limite do
buffer: produtor bloqueado com o buffer cheio e consumidor bloqueado com o
buffer vazio.
