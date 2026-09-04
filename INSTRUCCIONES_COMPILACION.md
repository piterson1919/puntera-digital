# Puntera Digital - Instrucciones de Compilación (Fase 4)

El proyecto Android ha sido inicializado con **Clean Architecture** y **MVVM** usando Jetpack Compose, Room, Hilt, CameraX y ML Kit, listo para compilar.

## Requisitos Previos
1. Tener **Android Studio (Giraffe o superior)** instalado.
2. Contar con el SDK de Android configurado (API 34).
3. Java JDK 17.

## Instrucciones para Generar el APK
Abre una terminal en la raíz del proyecto resultante (`PunteraDigital`) y ejecuta el siguiente comando oficial de Gradle Wrapper para ensamblar la variante Debug:

```bash
./gradlew assembleDebug
```
*Tip: Si estás en Windows (sin WSL o bash), ejecuta `gradlew.bat assembleDebug`.*

Una vez finalizado (y descargadas todas las dependencias de Compose, Room y ML Kit), encontrarás el archivo APK generado en la siguiente ruta:
`app/build/outputs/apk/debug/app-debug.apk`

Este archivo ya lo puedes instalar en tu dispositivo industrial de lectura o teléfono Android para probar el Dashboard y el escáner de la cámara.
