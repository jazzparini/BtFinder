# BT Finder

App Android (Kotlin + Jetpack Compose) para localizar un audífono Bluetooth
vinculado mediante escaneo BLE, intensidad de señal (RSSI) e indicadores de
proximidad. Generada a partir de la especificación en
`bt_phone_instructions.md` (carpeta `bt_phones` del proyecto).

## Cómo obtener el APK para instalar en el teléfono

El entorno donde se generó este proyecto no tiene acceso a los repositorios
de Google/Android (dl.google.com, maven.google.com), así que el APK no pudo
compilarse ahí. Hay dos formas de conseguirlo:

### Opción A — Compilar en Android Studio (recomendada, sin GitHub)

1. Abrir esta carpeta (`BtFinder/`) con Android Studio (Ladybug o superior).
   Ya incluye el wrapper de Gradle (`gradlew`), así que Android Studio
   descargará Gradle 8.9 automáticamente en la primera sincronización.
2. Esperar a que termine "Gradle sync" (puede tardar varios minutos la
   primera vez, porque descarga el SDK/las dependencias).
3. Para instalar directo en tu teléfono: conectarlo por USB con la
   "depuración USB" activada y presionar ▶ Run.
4. Para generar el archivo `.apk` instalable manualmente: menú
   **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. Cuando termine,
   aparece un enlace "locate" que lleva a
   `app/build/outputs/apk/debug/app-debug.apk`. Ese archivo se copia al
   teléfono y se instala tocándolo (activando antes "Instalar apps de
   origen desconocido" para el instalador que uses).

### Opción B — Compilar en la nube con GitHub Actions (sin instalar nada)

El proyecto ya incluye `.github/workflows/build-apk.yml`.

1. Crear un repositorio en GitHub y subir el contenido de esta carpeta
   (`git init`, `git add .`, `git commit`, `git push` a un repo nuevo).
2. En GitHub, ir a la pestaña **Actions** del repo → el workflow
   "Compilar APK de BT Finder" corre solo al hacer push a `main`, o se
   puede lanzar a mano con el botón "Run workflow".
3. Cuando el workflow termina (unos minutos), entrar a esa ejecución y
   descargar el artefacto `bt-finder-debug-apk` (es un .zip que contiene
   el `app-debug.apk`).
4. Pasar el APK al teléfono (por cable, Drive, etc.) e instalarlo.

Ambas opciones producen un APK de **depuración** (sin firmar para Play
Store), que es lo normal para instalar manualmente en un teléfono propio.

## Qué implementa el MVP (sección 2 y 20 del documento)

- Selección de dispositivos Bluetooth vinculados.
- Escaneo BLE con promedio móvil de RSSI (`RssiFilter`).
- Clasificación de proximidad con 5 estados, incluido "No detectado" tras
  8 s sin señal (`Proximity`).
- Vibración corta cuando la proximidad mejora (`VibratorHelper`).
- Botón de "Probar sonido" que emite un pitido desde el teléfono
  (`BeepPlayer`); el documento aclara que no todos los audífonos reciben
  ese audio, y la UI no lo garantiza.
- Persistencia del último dispositivo seleccionado con DataStore
  (`PreferencesRepository`), sin tokens ni datos personales.
- Servicio en primer plano `connectedDevice` con notificación visible
  mientras el escaneo está activo (`BluetoothScanService`), iniciado
  únicamente por una acción explícita del usuario en primer plano, según
  exige Android.
- Solicitud de permisos según versión de Android (`Permissions`):
  `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` en Android 12+, o
  `ACCESS_FINE_LOCATION` en versiones anteriores.
- Pruebas unitarias de `RssiFilter` y `Proximity` (JUnit).

## Diferencias respecto a los archivos originales de la carpeta

- `bt.js` y `layout.xml`/`androidmanifest.xml` sueltos eran un borrador
  previo en Java + Views (con varios errores de compilación: import roto
  `android.bluetooth BLEScanner`, métodos inexistentes como
  `BluetoothAdapter.isScanning()`/`getConnectedDevice()`, permiso mal
  referenciado `BluetoothScan`, falta de `PackageManager` importado). El
  propio `bt_phone_instructions.md` reemplaza ese borrador por la
  arquitectura Kotlin + Compose descrita en sus secciones 4 a 15, así que
  el programa se construyó sobre esa versión, no sobre el borrador Java.
- Se corrigió `BluetoothDevice.displayName` (no existe en la API pública)
  por `BluetoothDevice.name`, con `address` como respaldo.
- Se completaron los archivos que la sección 5 menciona en la estructura
  de carpetas pero no traía con código: `PreferencesRepository.kt`,
  `Permissions.kt`, `BtFinderApplication.kt`, y se agregó `VibratorHelper.kt`
  para la vibración prevista en el alcance del MVP.

## Fuera de alcance (sección 2 y 18)

Mapas, geolocalización precisa, protocolos privados de fabricantes
(AirPods, Galaxy Buds, etc.), rastreo con el audífono apagado o dentro del
estuche, y rastreo mediante la red de otros teléfonos. Ver limitaciones de
diseño completas en `bt_phone_instructions.md`, sección 18.
