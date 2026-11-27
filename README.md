# Windows Pad

Launcher Android inspirado na experiência de desktop do Windows 11, desenvolvido para tablets como o Redmi Pad Pro.

## Recursos da primeira versão
- Tela inicial com lista de aplicativos instalados.
- Pesquisa por nome de aplicativo.
- Barra inferior com acesso rápido à tela inicial e relógio.
- Cores que acompanham o tema claro/escuro do Android.
- Registro como aplicativo de início (launcher).

## Requisitos
- Android 8.0 ou superior (API 26).
- Projeto configurado para compilar com Android SDK 36 e Java 17.

## Gerar APK
O repositório possui um workflow do GitHub Actions em `.github/workflows/android.yml`.
Cada envio para a branch `main` inicia uma compilação de teste. Quando ela termina com sucesso, o APK de depuração fica disponível como artefato **WindowsPad-debug** na execução do workflow, na aba Actions.

## Estado do projeto
Esta é uma primeira versão funcional em desenvolvimento. Ainda não é uma versão final de produção; precisamos instalar e testar no Redmi Pad Pro antes de considerar o launcher pronto.
