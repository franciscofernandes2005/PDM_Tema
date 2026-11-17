# DeviceFeatures - Contador de Passos

Projeto desenvolvido no âmbito da unidade curricular de **Programação de Dispositivos Móveis (PDM)**.

## Tema

**Funcionalidades do dispositivo, permissões e privacidade**

A aplicação demonstra a utilização de um sensor do dispositivo Android, o tratamento de permissões em tempo de execução e o armazenamento local de resultados.

## Funcionalidades

- Utilização do sensor de passos do dispositivo.
- Contagem dos passos realizados pelo utilizador.
- Pedido da permissão de atividade física em tempo de execução.
- Tratamento da aceitação e recusa da permissão.
- Verificação da disponibilidade do sensor.
- Possibilidade de guardar o resultado localmente no dispositivo.
- Não necessita de GPS, Internet ou serviços externos.

## Permissões

A aplicação utiliza a permissão:

`ACTIVITY_RECOGNITION`

Esta permissão é necessária para aceder aos dados relacionados com a atividade física do utilizador em versões compatíveis do Android.

Se a permissão for recusada, a aplicação não inicia novas leituras do sensor.

## Privacidade

A aplicação segue o princípio da permissão mínima, pedindo apenas a autorização necessária para a funcionalidade utilizada.

Os resultados são guardados localmente e não são enviados para servidores ou serviços externos.

## Requisitos

- Dispositivo Android compatível.
- Android 10 ou superior recomendado.
- Sensor de passos disponível no dispositivo.
- Permissão de atividade física concedida para efetuar novas leituras.

## Como testar

1. Instalar e abrir a aplicação num dispositivo Android.
2. Carregar no botão para iniciar a contagem.
3. Quando solicitado, aceitar ou recusar a permissão de atividade física.
4. Se a permissão for recusada, verificar que a contagem não é iniciada.
5. Conceder a permissão.
6. Caminhar alguns passos com o dispositivo.
7. Confirmar que o valor apresentado aumenta.
8. Guardar o resultado e verificar que a operação é concluída.

## Protocolo experimental

O protocolo experimental completo encontra-se na pasta:

`docs/protocolo_experimental.pdf`

Este documento contém o enquadramento teórico, objetivos, configuração utilizada, dados de entrada, procedimento, métricas e resultados esperados.

