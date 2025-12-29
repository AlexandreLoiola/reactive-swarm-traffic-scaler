# Teste de Performance do Backend

## Objetivo
Medir a performance do serviço backend (`/backend`) sob carga, avaliando **throughput**, **latência** e **estabilidade**, considerando processamento distribuído.

## Cenário de Teste
- **Plano JMeter:** `jmeter/teste-carga.jmx`  
- **Threads:** 200000 usuários simultâneos  
- **Loops:** 1 por usuário  
- **Ramp-up:** 60 segundos  
- **Pré-aquecimento:** 10 instâncias ativas  
- **Endpoints testados:**
  - `GET /api/load`  
- **Ambiente:** Docker Compose local (`docker-compose up`)

## Resultados
| Métrica                  | Valor                      |
|---------------------------|----------------------------|
| Total de Requisições      | 200000                     |
| Erros                     | 34 (0,017%)                |
| Latência Média            | 347 ms                     |
| Mediana da Latência       | 323 ms                     |
| Latência Mínima           | 0 ms                       |
| Latência Máxima           | 604 ms                     |
| 90º Percentil (pct3)      | 549 ms                     |
| Throughput                | 6.097 req/s                |
| Dados Recebidos           | 959 KB/s                   |
| Dados Enviados            | 709 KB/s                   |

> **Observação:**  
> O teste foi executado com **10 instâncias do backend ativas**, o que resultou em um aumento significativo de throughput e redução consistente da latência em comparação ao teste com instância única.

## Gráficos
> Os gráficos podem ser exportados manualmente pelo GUI do JMeter ou usando plugins.  
> Para visualização detalhada, utilize o relatório HTML gerado.

- **Dashboard HTML:** [Abrir relatório](jmeter/reports/html/index.html)  
- **Resumo CSV:** [Baixar CSV](jmeter/reports/results.jtl)

## Observações
- Este teste foi executado em ambiente local, limitado pelo hardware da máquina.  
- A taxa de erro permaneceu extremamente baixa (**0,017%**), indicando boa estabilidade mesmo sob carga elevada.  
- O uso de múltiplas instâncias mostrou-se eficaz para escalar throughput e reduzir latência.  
- Os resultados servem como base para análises de escalabilidade e tuning de performance da aplicação.