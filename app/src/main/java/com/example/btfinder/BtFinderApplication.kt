package com.example.btfinder

import android.app.Application

/**
 * Punto de entrada de la aplicación (referenciado en AndroidManifest.xml,
 * sección 7). No mantiene estado propio en el MVP; existe como lugar natural
 * para inicializaciones globales futuras (por ejemplo, un contenedor de
 * dependencias) sin tocar MainActivity.
 */
class BtFinderApplication : Application()
