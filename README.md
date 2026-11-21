# Passador Replay

Projeto Android para transferir replays entre as instalações Normal e MAX do Free Fire.

## Build local

```bash
./gradlew clean assembleDebug
```

O APK será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build no GitHub

O workflow em `.github/workflows/build.yml` instala o JDK 17, a plataforma Android 34 e o Build Tools 34.0.0. Execute manualmente em **Actions > Build APK > Run workflow** ou faça push na branch `main`.

## Permissões

O aplicativo solicita notificações no Android 13+ e acesso amplo a arquivos quando necessário. O pareamento de depuração sem fio deve ser feito manualmente pelo usuário nas configurações do Android.
