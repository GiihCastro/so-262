# Atividade 04 — Gerência do Processador no SOsim

**Aluna:** Giovanna Castro  
**Disciplina:** Sistemas Operacionais (SO-262)

## Sobre a atividade

A atividade utiliza o **SOsim** para analisar o comportamento de processos CPU-bound e I/O-bound em diferentes configurações de escalonamento.

Foram realizados quatro exercícios:

1. Escalonamento circular sem prioridade;
2. Escalonamento circular com prioridade maior para o processo I/O-bound;
3. Prioridade maior para o processo CPU-bound;
4. Escalonamento com prioridade dinâmica.

Durante os testes, foram observados conceitos como quantum, troca de contexto, estados dos processos, prioridades e starvation.

## Conclusão

Os resultados mostraram que o uso da CPU depende tanto da prioridade quanto do comportamento de cada processo. A prioridade dinâmica ajuda a distribuir melhor o processamento e evita que processos permaneçam indefinidamente na fila de Pronto.
