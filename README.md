# Windows Pad

Launcher Android para tablets, com duas experiências: **modo Tablet** e **modo DeX**. O projeto é desenvolvido em Kotlin + Jetpack Compose e tem como alvo inicial o Redmi Pad Pro com Android 16.

## Estado atual

Versão de desenvolvimento: **0.3.0** (build de CI; ainda precisa de validação física no Redmi Pad Pro).

### Implementado
- Registro como aplicativo de tela inicial do Android.
- Interface adaptável entre retrato e paisagem.
- Tema claro/escuro seguindo o tema do sistema.
- Catálogo de aplicativos instalados e pesquisa no menu de aplicativos.
- Atualização do catálogo quando o launcher volta ao primeiro plano.
- Barra inferior com atalhos para aplicativos recentes.
- Modo DeX experimental com área de trabalho, menu Iniciar, pesquisa local e painel de controles.
- Assistente local experimental com comandos em português e encaminhamento para o reconhecedor de voz do Android.
- Preferência de modo Tablet/DeX e lista de aplicativos recentes persistidas localmente.

### Limitações conhecidas
- O assistente ainda não é um modelo generativo: executa comandos locais e pesquisa aplicativos.
- O painel de controles abre as configurações do Android; não substitui os painéis de sistema protegidos.
- O modo DeX organiza atalhos e controles dentro do launcher, mas não cria janelas independentes para outros aplicativos. O gerenciamento de janelas depende do Android/HyperOS.
- Widgets de terceiros ainda não estão hospedados. A API oficial `AppWidgetHost` permite implementar isso, mas exige fluxo de autorização, configuração, persistência de IDs e gerenciamento do ciclo de vida.
- O launcher ainda precisa de testes de interação, rotação, multitarefa e consumo de memória no hardware real.

## Arquitetura atual

- `MainActivity.kt`: atividade HOME, descoberta de aplicativos, preferências e telas Compose.
- `app/src/main/AndroidManifest.xml`: declaração da atividade de launcher e visibilidade de pacotes.
- `app/build.gradle.kts`: SDK, Kotlin e dependências Compose.
- `.github/workflows/android.yml`: compilação automatizada e publicação do APK de depuração como artefato.

## Próximas etapas técnicas

1. Separar modelo de dados, persistência, catálogo de apps e componentes visuais em arquivos próprios.
2. Evoluir o DeX com atalhos favoritos configuráveis, indicadores de execução e comportamento responsivo.
3. Implementar widgets Android reais com `AppWidgetHost` e fluxo de autorização do usuário.
4. Adicionar menu de contexto por aplicativo e suporte a atalhos publicados pelos próprios apps.
5. Adicionar testes automatizados e validação de build release otimizado.
6. Avaliar integração de IA generativa opcional, com consentimento explícito e sem envio de dados por padrão.

## Requisitos e compilação

- Android mínimo: API 26 (Android 8.0).
- Compile SDK: Android 36.
- Java: 17.
- Gradle: 8.11.1.

O workflow do GitHub Actions compila `assembleDebug` a cada push na branch `main`. O APK de teste fica no artefato **WindowsPad-debug** da execução correspondente.

> Um build bem-sucedido confirma que o código compila; não substitui testes de instalação e uso no Redmi Pad Pro.
