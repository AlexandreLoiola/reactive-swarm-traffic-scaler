<div align="center"><h1>Reactive Swarm Traffic Scaler</h1></div>

# 1. Introdução

## 1.1 Visão Geral:

Este projeto consiste em uma investigação **experimental e analítica** sobre a resiliência e a elasticidade de sistemas distribuídos. O foco principal é o estudo de **loops de controle reativos** (*Self-Adaptive Control Loops*) aplicados a clusters de orquestração, observando como o software pode manter a estabilidade e a disponibilidade sob regimes de alta carga em ambientes de **recursos computacionais restritos**.

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
O núcleo do projeto reside no modelo de controle reativo, que transforma telemetria em ações de infraestrutura. O objetivo deste capítulo é formalizar as regras lógicas e os algoritmos que garantem a estabilidade do sistema sob carga.

## 3.1 Processamento de Telemetria
A métrica primária utilizada pelo controlador é a taxa média de requisições por segundo (RPS), derivada a partir de contadores cumulativos expostos pelo ponto de observabilidade do Ingress. 

A transformação de contadores em taxas é realizada por diferenciação temporal explícita, garantindo que o sinal de entrada do controlador represente a intensidade instantânea de carga, e não valores históricos acumulados.

## 3.2 Modelo de Utilização e Capacidade
O controlador adota um modelo determinístico de capacidade, no qual cada instância possui uma vazão máxima segura previamente estimada. A utilização do sistema é definida como a razão entre a carga média por instância e essa capacidade segura, permitindo que decisões de escalonamento sejam tomadas com base em limites estáveis e previsíveis.

## 3.3 Algoritmo de Escalonamento
O algoritmo de escalonamento constitui a lógica operacional que traduz indicadores de carga em decisões de infraestrutura. Diferente de uma resposta linear simples, este modelo busca equilibrar a agilidade da expansão (para proteger a disponibilidade) com a cautela da contração (para preservar a estabilidade). O objetivo primordial é converter a variabilidade do tráfego em uma topologia de rede que se ajuste dinamicamente, mantendo o sistema operando dentro de uma janela de eficiência onde o custo de computação e o desempenho de resposta estejam otimizados.

### 3.3.1 Scale-Up Proporcional
A expansão de capacidade segue um modelo proporcional ao erro de utilização, permitindo que o sistema reaja rapidamente a eventos de saturação. 

Essa decisão prioriza a redução imediata da pressão sobre cada unidade de processamento, aceitando maior agressividade no crescimento em troca de estabilidade operacional.

### 3.3.2 Scale-Down Amortecido + Temporal
O processo de contração de capacidade incorpora múltiplos mecanismos de estabilização: 
- Uma zona morta para absorção de ruído
- Uma função de amortecimento não linear para suavização da magnitude da decisão
- Um critério de estabilidade temporal baseado em amostras consecutivas de baixa utilização. 

Essa combinação reduz significativamente o risco de `thrashing` e preserva a capacidade do sistema de absorver picos subsequentes sem degradação perceptível.

## 3.4 Temporização e Estados de Transição
Após cada ação de escalonamento, o sistema entra em um estado de repouso forçado (cooldown), durante o qual novas decisões são suprimidas. Esse mecanismo garante que o impacto real da alteração de capacidade seja observado antes de qualquer nova intervenção, respeitando o tempo de aquecimento das instâncias e a latência do orquestrador.

## 3.5 Limitações do Modelo
Esta seção detalha os fatores intrínsecos e extrínsecos que restringem a capacidade de resposta do sistema, servindo como um guia para a interpretação dos resultados experimentais e para a identificação de cenários onde o modelo pode apresentar comportamentos sub-otimizados.

Como qualquer sistema baseado em loops de controle reativos, este modelo possui fronteiras de eficácia delimitadas por suas próprias premissas de design. Reconhecer essas limitações é fundamental para compreender em quais cenários a automação oferece resiliência e onde ela atinge o seu ponto de saturação técnica. 

### 3.5.1 Limitação 1 — Métrica Única de Decisão (RPS)
O modelo de controle adotado neste estudo utiliza exclusivamente a taxa de requisições por segundo (RPS) como sinal primário de carga. Embora essa métrica seja adequada para capturar a pressão externa exercida sobre o sistema, ela não reflete diretamente o custo computacional individual de cada requisição. Assim, diferentes perfis de carga podem produzir sinais de RPS semelhantes, apesar de apresentarem impactos significativamente distintos sobre os recursos internos das instâncias.

### 3.5.2 Limitação 2 — Capacidade Estática por Instância
A política de escalonamento assume uma capacidade máxima segura fixa para cada instância de processamento. Essa simplificação implica que todas as unidades são consideradas homogêneas e que sua capacidade permanece constante ao longo do tempo. Na prática, fatores como aquecimento do runtime, coleta de lixo e contenção de recursos no host podem introduzir variações temporais que não são capturadas por esse modelo.

### 3.5.3 Limitação 3 — Controle Reativo Não Preditivo
O controlador implementado adota uma abordagem estritamente reativa, tomando decisões apenas com base no estado atual do sistema. Como consequência, o escalonamento ocorre somente após a detecção efetiva de saturação, o que implica a existência de uma janela temporal na qual o sistema pode operar sob carga elevada antes que novas instâncias estejam plenamente disponíveis.

### 3.5.4 Limitação 4 — Latência de Provisionamento e Warm-up
A eficácia do escalonamento é fortemente impactada pela latência de provisionamento das instâncias. Mesmo após a decisão de expansão, existe um intervalo não desprezível até que as novas réplicas estejam prontas para atender requisições de forma eficiente. Esse período de aquecimento representa um limite físico do modelo, especialmente relevante em cenários de picos abruptos de tráfego.

### 3.5.5 Limitação 5 — Escopo Restrito ao Plano de Dados
Este estudo restringe deliberadamente o domínio de escalonamento à camada de processamento da aplicação. Componentes como o serviço de decisão e a camada de intermediação são tratados como estáticos e fora do escopo de falhas analisado, o que simplifica o modelo, mas limita sua aplicabilidade em cenários de produção de grande escala.

### 3.5.6 Síntese
As limitações apresentadas não invalidam os resultados obtidos, mas delimitam claramente o contexto no qual o modelo se mostra eficaz. O objetivo deste trabalho não é propor um sistema de escalonamento universal, mas sim analisar, de forma controlada e empírica, os efeitos de um loop de controle reativo simples, interpretável e estável em um ambiente de recursos restritos.

## 3.6 Formalização Matemática do Modelo de Controle
Esta seção apresenta a formalização matemática do modelo de controle implementado, estabelecendo explicitamente as variáveis, funções e transformações utilizadas pelo sistema para converter sinais de carga em decisões de escalonamento. O objetivo é tornar explícitas as hipóteses e os cálculos subjacentes à lógica descrita nas seções anteriores.

### 3.6.1 Variáveis e Parâmetros do Sistema
Sejam definidas as seguintes variáveis de estado e parâmetros de controle:

- $R(t)$: taxa média total de requisições por segundo observada no instante 𝑡
- $N(t)$: número de instâncias ativas no instante 𝑡
- $C(max)$: capacidade máxima segura de uma instância (RPS)
- $U(t)$: utilização normalizada do sistema
- $U(target)$: utilização alvo configurada
- $N(min)$,$N(max)$: limites inferior e superior de instâncias

Parâmetros de controle adicionais:

- $𝛼$: fator de agressividade de scale-up
- $β$: fator de conservadorismo de scale-down
- $δ$: largura da zona morta
- $γ$: fator de amortecimento não linear

### 3.6.2 Cálculo da Utilização Normalizada
A carga média por instância é definida como:

$$
r_i(t) = \frac{R(t)}{N(t)}
$$

A utilização normalizada do sistema é então calculada por:

$$
U(t) = \frac{r_i(t)}{C_{\text{max}}}
     = \frac{R(t)}{N(t) \cdot C_{\text{max}}}
$$

Essa normalização permite que o controlador opere de forma independente da escala absoluta de tráfego e do número de instâncias ativas.

### 3.6.3 Erro de Controle (Delta de Utilização)
O erro de controle é definido como a diferença entre a utilização observada e a utilização alvo:

$$
\Delta U(t) = U(t) - U_{\text{target}}
$$

Esse termo representa a magnitude e a direção da correção necessária:

- ΔU(t)>0: sistema sobrecarregado
- ΔU(t)<0: sistema subutilizado

### 3.6.4 Função de Decisão de Scale-Up
A expansão de capacidade ocorre quando o erro de utilização é positivo e o sistema não atingiu o limite máximo de instâncias:

$$
\text{Scale-Up} \iff
\begin{cases}
\Delta U(t) > 0 \\
N(t) < N_{\max}
\end{cases}
$$

O número de instâncias adicionadas é calculado por um modelo proporcional:
$$
\Delta N^{+}(t) =
\left\lceil
\alpha \cdot \Delta U(t) \cdot N(t)
\right\rceil
$$

Com saturação superior:
$$
\Delta N^{+}(t) \leq N_{\max} - N(t)
$$

Esse modelo permite crescimento mais agressivo à medida que o sistema escala, favorecendo a proteção da disponibilidade durante picos abruptos de carga.

### 3.6.5 Função de Decisão de Scale-Down Amortecido
A contração de capacidade ocorre apenas quando a subutilização excede a zona morta configurada:

$$
\text{Scale-Down} \iff
\begin{cases}
\Delta U(t) < -\delta \\
N(t) > N_{\min}
\end{cases}
$$

Define-se inicialmente a subutilização absoluta:
$$
U_{\text{under}}(t) = |\Delta U(t)|
$$

Aplica-se então um amortecimento não linear:
$$
D(t) = U_{\text{under}}(t)^{\frac{1}{\gamma}}
$$


O número de instâncias removidas é dado por:
$$
\Delta N^{-}(t) =
\left\lfloor
D(t) \cdot \beta \cdot N(t)
\right\rfloor
$$

Com restrições:
$$
1 \leq \Delta N^{-}(t) \leq N(t) - N_{\min}
$$

Esse mecanismo reduz a sensibilidade do sistema a pequenas flutuações de carga e previne reduções agressivas prematuras.

### 3.6.6 Evolução Discreta do Estado do Sistema

O sistema evolui em tempo discreto, sendo o número de instâncias atualizado conforme a decisão tomada:

$$
N(t+1) =
\begin{cases}
N(t) + \Delta N^{+}(t), & \text{se Scale-Up} \\
N(t) - \Delta N^{-}(t), & \text{se Scale-Down} \\
N(t), & \text{caso contrário}
\end{cases}
$$

Após cada transição de estado, o controlador entra em um período de inatividade forçada (cooldown), durante o qual novas decisões são suprimidas.

### 3.6.7 Classificação do Controlador
Do ponto de vista de sistemas de controle, o modelo pode ser caracterizado como:

- Controlador proporcional assimétrico
- Operando em tempo discreto
- Com saturação, zona morta e amortecimento não linear
- Sujeito a atraso de atuação (latência de provisionamento)

Embora não seja preditivo nem ótimo no sentido clássico, o modelo privilegia interpretabilidade, estabilidade e robustez operacional em ambientes de recursos limitados.

# 4. Configuração do Ambiente e Implementação
Este capítulo detalha a materialização técnica do modelo de controle discutido anteriormente. A escolha das ferramentas baseou-se na necessidade de componentes que permitissem o isolamento estrito de recursos, a exposição de telemetria de alta fidelidade e o máximo de escalabilidade possível.

## 4.1 Pilha Técnica

### 4.1.1 Orquestrador: Docker Swarm (Infraestrutura)
O Docker Swarm é a ferramenta responsável por transformar um conjunto de máquinas ou recursos em um único cluster gerenciável, ou seja, um agrupamento de nós que operam de forma coordenada, possibilitando que o sistema seja percebido e gerenciado como uma única infraestrutura 

- **Orquestração e Execução:** Gerencia o estado desejado dos serviços, garantindo que o número de instâncias ativas reflita as decisões de escalonamento $(ΔN(t))$ definidas pelo controlador.
- **Hospedagem de Containers:** Fornece o ambiente necessário para executar aplicações de forma isolada, permitindo a utilização de containers como unidades de execução.
- **Elasticidade Dinâmica:** Permite a criação rápida de novas instâncias quando a carga aumenta, assegurando suporte ao scale-out e manutenção da disponibilidade do sistema.
- **Monitoramento de Saúde:** Detecta falhas em instâncias e garante que apenas unidades saudáveis estejam ativas, preservando a estabilidade do cluster.
- **Gerenciamento de Cluster:** Coordena múltiplos nós como uma infraestrutura única e unificada, simplificando a administração de recursos distribuídos.

### 4.1.2 Controlador: Spring Boot (Loop de Decisão)
O Controlador é o componente central responsável por transformar sinais de telemetria em ações de escalonamento no cluster Docker Swarm. Ele implementa o loop de controle reativo, consumindo métricas em tempo real e determinando quando realizar operações de expansão ou contração de instâncias. 

- **Processamento de Telemetria:** Recebe métricas da camada de intermediação (Ingress) e calcula indicadores como RPS e latência média, convertendo dados brutos em sinais de controle acionáveis.
- **Lógica de Escalonamento:** Baseado em limiares e zonas mortas, emite comandos de Scale-Up e Scale-Down de forma proporcional e amortecida, preservando estabilidade e prevenindo thrashing.
- **Integração com Orquestrador:** Comunica-se com o Docker Swarm através de uma API ou interface de gerenciamento, aplicando mudanças de forma segura e controlada.
- **Período de Estabilização (Cooldown):** Impõe intervalos entre decisões sucessivas para permitir o aquecimento das novas instâncias e a estabilização do sistema antes de novas ações.

### 4.1.3 Gateway e Balanceador de Carga: Nginx (Intermediação)
O Nginx atua como gateway de entrada e balanceador de carga, sendo responsável por rotear o tráfego externo para as instâncias disponíveis da aplicação Quarkus.

- **Balanceamento de Carga:** Distribui requisições de forma eficiente entre múltiplas instâncias, mantendo o throughput alto e evitando sobrecarga em qualquer unidade.
- **Persistência de Conexões (Keep-Alive):** Mantém conexões TCP ativas para reduzir a latência de novas requisições e minimizar o overhead de handshake, essencial durante processos de scale-out.
- **Monitoramento Passivo:** Expondo métricas de acesso e saúde de conexões, fornece sinais para o controlador, contribuindo para decisões de escalonamento mais precisas.
- **Resiliência e Isolamento:** Funciona como camada de proteção, garantindo que instâncias de aplicação não sejam acessadas diretamente, preservando a integridade e o isolamento do cluster.

### 4.1.4 Alvo de Elasticidade: Quarkus (Aplicação)
O Quarkus foi escolhido como alvo de elasticidade por ser um framework Java orientado a arquiteturas Cloud Native, otimizado para inicialização rápida e baixo consumo de recursos. 

- **Eficiência em Confinamento:** O Quarkus permite que aplicações operem com uso reduzido de memória heap e menor quantidade de threads, tornando-o ideal para execução em ambientes conteinerizados e com recursos limitados.
- **Responsividade:** A velocidade inicialização do Quarkus favorece a rápida criação e disponibilização de novas instâncias, aspecto essencial em ambientes elásticos, pois diminui a janela de exposição a falhas durante processos de scale-out.
- **Execução em Modo Nativo (GraalVM):** Embora o processo de build seja mais custoso, a geração de um binário nativo permite inicializações quase instantâneas e um perfil de memória mais previsível, tornando o Quarkus particularmente adequado para cenários de elasticidade agressiva e ambientes com recursos severamente restritos.

## 4.2 Configuração do Ambiente Restrito
Conforme definido no objetivo central, o ambiente de execução foi configurado para operar em um ambiente de execução com recursos deliberadamente restritos. Foram aplicadas restrições em duas camadas: na orquestração (infraestrutura) e no nível do processo da aplicação.

### 4.1.1 Isolamento Via Orquestrador
Através do arquivo de definição do orquestração, foram impostos limites rígidos que forçam a aplicação a atingir o seu ponto de saturação precocemente:

```YAML
services:
  backend:
    image: java-backend
    deploy:
      resources:
        limits:
          cpus: '0.50'     # Limite de 50% de um núcleo (Throttling)
          memory: 512M     # Capping de memória
        reservations:
          cpus: '0.25'     # Garantia mínima de recurso
          memory: 256M     # Reserva mínima
```

### 4.1.2 Ajuste Fino a nível de aplicação
Para garantir que a aplicação opere e performe de forma estável dentro deste "confinamento", o arquivo `application.properties` foi ajustado para limitar o uso de threads e gerenciar o heap de memória de forma conservadora. Isso evita que o runtime tente alocar mais recursos do que o Docker permite, o que causaria instabilidade imediata.

```Properties
# Limitação de concorrência no nível da aplicação
quarkus.http.io-threads=2
quarkus.http.worker-threads=8

# Contrato de memória explícito
quarkus.native.initial-heap-size=64m
quarkus.native.max-heap-size=128m
```

- **Minimização de Threads:** Ao restringir o número de worker threads, evitamos o custo excessivo de troca de contexto em uma CPU limitada a 0.5 cores.
- **Gestão de Memória:** O limite de heap em 128MB, dentro de um container de 512MB, deixa margem de segurança para a memória off-heap e para o próprio sistema operacional, reduzindo drasticamente o risco de erros de Out of Memory (OOM).

## 4.3 Implementação de Loop de Controle Reativo

## 4.4 Implementação da Aplicação Alvo de Elasticidade
A aplicação foi projetada para expor um único endpoint, responsável por acionar uma rotina de simulação de trabalho síncrona a cada requisição. O ponto de entrada é um endpoint REST acessível via método GET na rota `api/load`..

### 4.4.1 Simulação de Trabalho Computacional e Latência
Cada requisição executa uma rotina de carga sintética que combina processamento intensivo (CPU-bound) e espera bloqueante (I/O-bound). Essa alternância permite simular de forma realista cenários de saturação e contenção de recursos.

O consumo de CPU é implementado por meio de um laço baseado em tempo (busy-wait), mantendo o processador ocupado com cálculos matemáticos por um intervalo pseudoaleatório:

```java
private void simulateCpu() {
    long end = System.nanoTime() +
        (200_000_000L + random.nextInt(100_000_000));

    while (System.nanoTime() < end) {
        Math.log(random.nextDouble() + 1);
    }
}
```
- O consumo de CPU é implementado por meio de um laço baseado em tempo, mantendo o processador ocupado por um intervalo pseudoaleatório:

```java
private void simulateIo() {
    Thread.sleep(100 + random.nextInt(100));
}
```
- A latência de I/O é simulada através de uma pausa bloqueante:

### 4.4.2 Métricas Expostas
O conjunto de métricas expostas prioriza indicadores de carga, latência e consumo de CPU. Tal configuração facilita o diagnóstico de impactos causados por CPU throttling e pelo confinamento de recursos do orquestrador.

- **`app_load_requests_total`**: Contador cumulativo do número total de requisições processadas pelo endpoint de carga.

- **`app_load_errors_total`**: Contador cumulativo de erros ocorridos durante a execução da rotina de carga.

- **`app_load_execution_time`**: Temporizador (Timer) do tempo de execução da rotina de simulação de carga, com publicação dos percentis p50, p95 e p99.

- **`app_process_cpu_load`**: Representa a carga de CPU específica do processo da JVM.

- **`app_system_cpu_load`**: Indica a carga total de CPU de todo o sistema operacional onde o contêiner reside.

- **`app_available_processors`**: Número de processadores disponíveis para a JVM.



# 5. Sintese e Análise dos Resultados
# 6. Conclusão