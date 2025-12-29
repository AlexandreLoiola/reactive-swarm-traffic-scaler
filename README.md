# Reactive Swarm Traffic Scaler

# 1. Introdução

## 1.1 Visão Geral:

Este projeto consiste em uma investigação **experimental e analítica** sobre a resiliência e a elasticidade de sistemas distribuídos. O foco principal é o estudo de **loops de controle adaptativos** (Self-Adaptive Control Loops) aplicados a clusters de orquestração, observando como o software pode manter a estabilidade e a disponibilidade sob regimes de alta carga em ambientes de **recursos computacionais restritos**.

Diferente de soluções de escalonamento prontas (como o HPA do Kubernetes), este projeto implementa um motor de decisão customizado que consome telemetria em tempo real para orquestrar o estado do cluster **Docker Swarm**, priorizando a previsibilidade e a análise empírica dos *trade-offs* entre latência, throughput e custo de infraestrutura.

---

## 1.2 Objetivo Central:

Desenvolver e analisar um **Loop de Controle Reativo** capaz de gerenciar a elasticidade de serviços em um cluster **Docker Swarm**, garantindo a disponibilidade e a estabilidade de uma aplicação Java sob regimes de tráfego variável em um ambiente de infraestrutura estritamente limitada.

## 1.3 Objetivos Específicos:
Para atingir o objetivo central, o projeto se baseia nos seguintes pilares de implementação e análise:

- **Implementação de Telemetria Dinâmica:** Configurar o Nginx como sensor de carga em tempo real, utilizando *Shared Memory Zones* e *Stub Status* para extrair métricas de vazão e concorrência.
- **Automação do Ciclo de Vida de Containers:** Desenvolver um motor em Spring Boot que interaja com a API nativa do Docker para realizar operações de *Scale-Out* e *Scale-In* baseadas em lógica de limiares (*thresholds*).
- **Otimização de Service Discovery:** Implementar e validar a resolução de DNS dinâmico e o *Connection Pooling* (Keepalive) para garantir que o tráfego seja redistribuído instantaneamente após a criação de novas réplicas.
- **Refinamento de Resource Quotas:** Analisar o comportamento da JVM sob limites rígidos (CPU Throttling e Memory Capping), ajustando as flags de sobrevivência do container para evitar falhas por exaustão de recursos (*OOMKilled*).
- **Mensuração de Throughput:** Avaliar a degradação graciosa do sistema, identificando o ponto de saturação onde o aumento de réplicas deixa de mitigar a latência devido a gargalos de rede ou gerência de estado.
- **Elasticidade Reativa:** Avaliar a eficácia do escalonamento horizontal automático baseado em telemetria de tráfego.

---

## 1.4 Vetores de Investigação Técnica:

O projeto foca na análise técnica de variáveis críticas que determinam o sucesso da orquestração automática:

1. **Latência de Provisionamento ($\Delta t$):** Qual o intervalo temporal entre a detecção de saturação e a estabilização efetiva de novas instâncias no balanceador de carga?
2. **Impacto do Escalonamento na Experiência do Usuário:** Como o processo de *Scale-Out* (expansão) influencia os percentis de latência ($p95, p99$) durante o período de aquecimento (*warm-up*) das réplicas?
3. **Determinismo de Carga:** Existe uma correlação linear entre o aumento de instâncias e a redução da taxa de erro (5xx) sob cenários de tráfego intenso e recursos limitados?
4. **Resiliência do Balanceamento:** Qual a eficácia da técnica de *Connection Pooling* (Keepalive) na mitigação de gargalos de rede em camadas de transporte (TCP)?

---

## 1.5 Metodologia 
A metodologia adotada para este estudo baseia-se em um ciclo experimental controlado, dividido em quatro etapas fundamentais que permitem observar a relação entre demanda de tráfego e resposta de infraestrutura.

### 1.5.1 Configuração do Ambiente Controlado (Isolamento)
A primeira etapa consiste no isolamento do sistema em um ambiente com restrições fixas de processamento e memória. O objetivo é criar um cenário de escassez controlada, onde a eficiência do algoritmo de escalonamento seja o fator determinante para a sobrevivência da aplicação, e não a abundância de recursos de hardware.

### 1.5.2 Coleta de Sinais e Métricas (Monitoramento)
O sistema é instrumentado para atuar como um organismo sensorial. Nesta fase, estabelecem-se os mecanismos que observam o fluxo de entrada (requisições de usuários) e o estado interno dos componentes. Essa coleta de dados é contínua e serve como a base de informação para qualquer tomada de decisão futura.

### 1.5.3 Protocolo de Estimulação de Carga (Teste de Estresse)
Para validar a hipótese de escalabilidade, o sistema é submetido a diferentes perfis de carga simulada. Esses perfis variam desde aumentos graduais e previsíveis até picos súbitos e agressivos. O intuito é observar como o sistema se comporta não apenas no estado de equilíbrio, mas principalmente nos momentos de transição e instabilidade.

### 1.5.4 Ciclo de Decisão e Ajuste (Feedback)
A etapa final da metodologia é o fechamento do ciclo de controle. Com base nas métricas coletadas, um motor de decisão avalia se o sistema deve expandir sua capacidade (adicionando novas unidades de processamento) ou contraí-la (removendo unidades ociosas). A análise foca na precisão e na velocidade dessa resposta, buscando minimizar o tempo de exposição do sistema a estados de sobrecarga.

---