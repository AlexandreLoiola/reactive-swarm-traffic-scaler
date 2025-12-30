# Reactive Swarm Traffic Scaler

# 1. Introdução

## 1.1 Visão Geral:

Este projeto consiste em uma investigação **experimental e analítica** sobre a resiliência e a elasticidade de sistemas distribuídos. O foco principal é o estudo de **loops de controle adaptativos** (*Self-Adaptive Control Loops*) aplicados a clusters de orquestração, observando como o software pode manter a estabilidade e a disponibilidade sob regimes de alta carga em ambientes de **recursos computacionais restritos**.

Diferente de soluções de escalonamento prontas (como o HPA do Kubernetes), este projeto implementa um motor de decisão customizado que consome telemetria em tempo real para orquestrar o estado do cluster **Docker Swarm**, priorizando a previsibilidade e a análise empírica dos *trade-offs* entre latência, *throughput* e custo de infraestrutura.

---

## 1.2 Objetivo Central:

Desenvolver e analisar um **Loop de Controle Reativo** capaz de gerenciar a elasticidade de serviços em um ambiente de orquestração de containers, garantindo a disponibilidade e a estabilidade da camada de aplicação sob regimes de tráfego variável em um cenário de infraestrutura com restrições severas de recursos.

## 1.3 Objetivos Específicos:
Para atingir o objetivo central, o projeto se baseia nos seguintes pilares de implementação e análise:

- **Implementação de Telemetria Dinâmica:** Estabelecer uma camada de monitoramento na borda do sistema para atuar como sensor de carga, extraindo métricas de vazão (*throughput*) e concorrência de requisições de forma contínua.
- **Automação da Elasticidade Horizontal:** Desenvolver um motor de orquestração capaz de realizar operações de expansão (Scale-Out) e contração (Scale-In) de instâncias, reagindo dinamicamente a limiares de utilização pré-definidos.
- **Otimização do Escoamento de Tráfego:** Implementar mecanismos de descoberta de serviços e persistência de conexões (Connection Pooling) para garantir que a redistribuição de carga entre as novas unidades de processamento ocorra sem latência residual.
- **Gerenciamento de Cotas de Recursos:** Analisar o comportamento do ambiente de execução sob limites rígidos de hardware, ajustando parâmetros de memória e processamento para garantir a resiliência operacional e evitar interrupções abruptas por exaustão de recursos.
- **Análise de Saturação e Performance:** Mensurar a capacidade máxima do sistema e identificar o ponto de "retorno decrescente", onde o acréscimo de novas unidades de processamento deixa de resultar em ganhos proporcionais de performance.

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
- A primeira etapa consiste no isolamento do sistema em um ambiente com restrições fixas de processamento e memória. O objetivo é criar um cenário de escassez controlada, onde a eficiência do algoritmo de escalonamento seja o fator determinante para a sobrevivência da aplicação, e não a abundância de recursos de hardware.

### 1.5.2 Coleta de Sinais e Métricas (Monitoramento)
- O sistema é instrumentado para atuar como um organismo sensorial. Nesta fase, estabelecem-se os mecanismos que observam o fluxo de entrada (requisições de usuários) e o estado interno dos componentes. Essa coleta de dados é contínua e serve como a base de informação para qualquer tomada de decisão futura.

- Embora o sistema possa ser instrumentado para coletar métricas internas da aplicação e da infraestrutura, o escopo deste trabalho restringe-se à análise de métricas de tráfego HTTP na borda do sistema, tratadas como sinal primário para o loop de controle.

### 1.5.3 Protocolo de Estimulação de Carga (Teste de Estresse)
- Para validar a hipótese de escalabilidade, o sistema é submetido a diferentes perfis de carga simulada. Esses perfis variam desde aumentos graduais e previsíveis até picos súbitos e agressivos. O intuito é observar como o sistema se comporta não apenas no estado de equilíbrio, mas principalmente nos momentos de transição e instabilidade.

### 1.5.4 Ciclo de Decisão e Ajuste (Feedback)
- A etapa final da metodologia é o fechamento do ciclo de controle. Com base nas métricas coletadas, um motor de decisão avalia se o sistema deve expandir sua capacidade (adicionando novas unidades de processamento) ou contraí-la (removendo unidades ociosas). A análise foca na precisão e na velocidade dessa resposta, buscando minimizar o tempo de exposição do sistema a estados de sobrecarga.

---

# 2. Arquitetura do Sistema
A arquitetura foi desenhada seguindo o modelo de Sistemas Autonômicos, onde o controle é exercido por um loop fechado de retroalimentação. O sistema é decomposto em três camadas lógicas que interagem de forma coordenada.

## 2.1 Camada de Intermediação e Sensoriamento (Ingress)
Constitui o **ponto único de entrada** e o limite de isolamento do sistema, em que todo trafego externo é canalizado. É a interface que atua como o mediador entre as demandas externas e o ambiente de processamento protegido.

- **Função de Intermediação:** Gerenciar o ciclo de vida das conexões e realizar a distribuição equitativa de carga. Por ser o único ponto de contato com a rede externa, ela garante que o isolamento do cluster seja mantido, impedindo acessos diretos às unidades de processamento.

- **Papel de Sensoriamento:** Gerar e expor telemetria bruta sobre o comportamento do tráfego. Atua como um sensor passivo que registra a densidade de requisições e a saúde das conexões, disponibilizando esses dados para que serviços externos de auditoria e decisão possam consumi-los.


## 2.2 Camada de Decisão e Controle (Logic)
É o núcleo analítico, que é responsável por processar a telemetria e coordenar as mutações no ambiente de operação. Consome os dados da Camada de Sensoriamento e os interpreta esses parâmetros para tomar a decisão que manterá o sistema estável.

Atua como um controlador externo, comunicado-se com o orquestrador dos serviços por meio de uma interface de gerenciamento. Os detalhes de implementação dessa interface de gerenciamento são abstraídos neste nível arquitetural, uma vez que o foco da análise reside no comportamento do loop de decisão, e não no mecanismo específico de integração com o orquestrador.

O serviço de decisão é tratado como um componente externo ao plano de dados e, neste estudo, não é alvo de escalonamento automático, sendo considerado fora do domínio de falhas analisado.

- **Processamento e Tradução de Telemetria:** A camada de decisão consome os dados brutos expostos pela Camada de Intermediação. Visto que métricas brutas (como contadores cumulativos) são insuficientes para uma tomada de decisão imediata, este componente realiza cálculos estatísticos para converter esses sinais em indicadores inteligíveis, como a taxa de requisições por segundo (RPS) ou a latência média em janelas de tempo específicas.

- **Lógica de Decisão e Gatilhos:** Com base nos indicadores calculados, o motor aplica políticas de controle baseadas em limiares (*thresholds*). A decisão de escala não é binária, mas sim baseada na comparação entre o estado atual e o estado desejado:

    - **Expansão (Scale-Out):** Quando os indicadores atingem o limite superior de segurança, o controlador emite uma ordem de comando para incrementar o número de unidades de processamento. O objetivo é a distribuição de carga, reduzindo a pressão individual sobre cada unidade antes que o tempo de resposta exceda os limites de aceitabilidade.

    - **Contração (Scale-In):** Quando a carga decresce e a ociosidade é confirmada por uma janela temporal segura, o controlador comanda a remoção de unidades excedentes. Esta ação visa o reordenamento de recursos e a eficiência operacional, evitando o desperdício de capacidade em um ambiente restrito.

    - **Inércia Decisória:** Caso os indicadores flutuem dentro da zona de tolerância (entre os limites de expansão e contração), o sistema opta deliberadamente por não realizar alterações. 

- **Política de Escalonamento:** A política de escalonamento adotada é baseada em limiares estáticos e janelas temporais fixas, priorizando previsibilidade, interpretabilidade e estabilidade do sistema, em detrimento de abordagens adaptativas ou preditivas mais complexas.

- **Mecanismo de Estabilização (Cooldown):** Para evitar o fenômeno de *thrashing* — instabilidade causada por decisões de escala sucessivas, contraditórias e rápidas demais — o sistema impõe um período de repouso forçado após cada ação. Este intervalo é vital para permitir que as novas instâncias passem pelo processo de aquecimento (*warm-up*) e para que o sistema se estabilize antes de uma nova reavaliação.

## 2.3 Camada de Execução e Processamento (Compute)
Esta camada é composta pelas unidades fundamentais de processamento onde a lógica de negócio é efetivamente executada. Ela representa o elemento elástico do sistema, sendo a camada que sofre as mutações diretas de estado comandadas pela decisão de controle.

- **Processamento sob Confinamento de Recursos:** Diferente das outras camadas, esta opera sob restrições severas e explícitas de hardware, como CPU e Memória. Essas limitações tornam cada unidade altamente sensível a picos de tráfego. É neste nível que fenômenos como o *CPU Throttling* e a exaustão de memória são observados e mitigados através da elasticidade.
- **Alvo de Estresse e Análise:** Esta é a camada que recebe o impacto direto da estimulação de carga. Ela funciona como o objeto de estudo principal, onde o comportamento do tempo de resposta e a taxa de erro são analisados para validar se a aplicação consegue manter a estabilidade enquanto opera próxima ao seu limite de saturação.

# 3. Modelo de Controle e Lógica de Decisão
# 4. Implementação e Configuração do Ambiente
# 5. Sintese e Análise dos Resultados
# 6. Conclusão