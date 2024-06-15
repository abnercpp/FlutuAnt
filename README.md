# FlutuAnt 🐜🧮

App para conversão de números decimais
para [`binary32` (ou `float32`)](https://en.wikipedia.org/wiki/Single-precision_floating-point_format) de acordo com o
padrão [IEEE-754](https://en.wikipedia.org/wiki/IEEE_754). (Apenas para Android.)

> [!NOTE]
> Este projeto faz parte do processo de avaliação da disciplina de Laboratório de Arquitetura e Organização de
> Computadores (ILP-500), ministrada pelo professor SERGIO LUIZ BANIN, do 2º semestre do curso de Análise e
> Desenvolvimento de Sistemas, no período matutino, da [Fatec São Paulo](https://www.fatecsp.br).

## Funcionalidades 📲

Atualmente o app possui as seguintes funcionalidades:

- [X] Conversão de [números normais](https://en.wikipedia.org/wiki/Normal_number_(computing)).
- [X] Conversão de zero
  e [números subnormais](https://en.wikipedia.org/wiki/Subnormal_number#:~:text=Any%20non%2Dzero%20number%20with,numbers%20(indicated%20in%20red).)
  com expoente fixo em -126.
- [X] Arredondamento de banqueiro ([_Banker's Rounding_](https://en.wikipedia.org/wiki/Rounding)) para números sem
  representação exata.
- [X] Conversão para [`NaN`](https://en.wikipedia.org/wiki/NaN) (_quiet_) em caso de números em formato inesperado.
- [X] Conversão para ±infinito em caso de números de magnitude excessiva.

## Testes 🔍

Este projeto possui testes unitários para validar o funcionamento da lógica de conversão. Eles podem ser encontrados na
pasta [`app/src/test`](./app/src/test).

## Pré-requisitos ⚒️

Para compilar e rodar este projeto localmente, as dependências necessárias são:

- [OpenJDK 17](https://learn.microsoft.com/pt-br/java/openjdk/download#openjdk-17)
- [Gradle](https://gradle.org/install/)
- [Android Studio](https://developer.android.com/studio)

## Recursos 🌐

O ícone utilizado pode ser encontrado no [Vexels](https://www.vexels.com/png-svg/preview/261532/purple-ant-character).

## Licença 📃

Este projeto utiliza a [licença do MIT](./LICENSE.txt).
