# proyectopresionarterial
<<<<<<< HEAD
Proyecto final Programacion 2, Aplicacion de control de presion arterial
=======

Aplicación Android (Java + MVVM + Room + Retrofit + Material 3) para registro y análisis básico de presión arterial con recomendaciones dinámicas caché local.

## Estado actual
Se han corregido errores de compilación y recursos:
- `NetworkUtils` reparado (archivo corrupto).
- `RetrofitProvider` simplificado (eliminado logging obligatorio y error de Gson). 
- Ajustado `compileOptions` a Java 17 (requerido por AGP 8.x).
- IDs y strings verificados en layouts (`include_last_measure`, `fragment_summary`).
- Se añadió test unitario: `ClassificationHelperTest`.

## Requisitos
- Android Studio Giraffe / Iguana o superior (AGP 8.12.3 declarado en catálogo).
- JDK 17 (Android Studio ya lo incluye; si usas CLI, asegúrate de `JAVA_HOME`).
- Dispositivo/emulador con API >= 24.

## Configuración de API (segura, recomendada)
Las llamadas a API usan `BuildConfig.API_BASE_URL`, `BuildConfig.API_KEY` y `BuildConfig.N8N_WEBHOOK_URL` definidos en `app/build.gradle.kts` a partir de propiedades de Gradle.

Para no poner claves en el repositorio sigue una de estas opciones (recomendado):

1) Añadir las propiedades en tu archivo local de Gradle (no versionado):
   - Windows (instalación local por desarrollador): edita o crea `C:\Users\<tu_usuario>\.gradle\gradle.properties` y añade:

```
API_BASE_URL=https://api.example.com/
API_KEY=TU_CLAVE_AQUI
N8N_WEBHOOK_URL=https://tu-n8n-webhook.example.com/xxxxx
```

2) Alternativa - archivo `local.properties` en la raíz del proyecto (no subirlo):
   - Crea `local.properties` y añade las mismas propiedades (NO subir este archivo):

```
API_BASE_URL=https://api.example.com/
API_KEY=TU_CLAVE_AQUI
N8N_WEBHOOK_URL=https://tu-n8n-webhook.example.com/xxxxx
```

3) Comprobar que `app/build.gradle.kts` usará estas propiedades y generará `BuildConfig.API_KEY` vacío si no existen; el proyecto compilará sin claves, pero las llamadas a la API devolverán 401/errores hasta configurar una clave válida.

Archivos y lugares donde NUNCA debes dejar claves en el repo:
- `app/src/main/java/**` (constantes hardcodeadas) — busca `API_KEY`, `OPENAI_API_KEY`, `sk-`.
- `gradle.properties` en el repositorio (si contiene claves) — usa tu `~/.gradle/gradle.properties` o `local.properties` en su lugar.
- `app/src/main/res/values/strings.xml` — evita URLs/keys reales; usa placeholders.
- `google-services.json`, `secrets.properties`, `.env` — no subir estos ficheros.

Si ya subiste una clave por error y la historia contiene la clave, purga el historial (ejemplo con BFG o git filter-repo). Atención: esto reescribe la historia del repo y requiere fuerza push y coordinación con colaboradores.

Ejemplo rápido (básico) para purgar un archivo sensible del historial con BFG (Linux/WSL/macOS, no ejecutar si no estás seguro):

```
# Instalar BFG y ejecutar (ejemplo):
bfg --delete-files google-services.json
git reflog expire --expire=now --all
git gc --prune=now --aggressive
git push --force
```

Si prefieres, deja el repo tal cual y crea un branch limpio donde eliminas los secretos antes de publicar.

## Compilación
Windows CMD:
```cmd
cd proyectopresionarterial
gradlew.bat clean assembleDebug
```
APK resultante (debug) estará en:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Ejecución de tests unitarios
```cmd
gradlew.bat testDebugUnitTest
```
Archivo de prueba añadido: `app/src/test/java/com/example/proyectopresionarterial/ClassificationHelperTest.java`.

## Estructura destacada
- Persistencia: Room (`AppDatabase`, `BloodPressureRecord`, `UserProfile`, `RecommendationCache`).
- ViewModels: manejo de LiveData para registros, perfil y recomendaciones.
- Red: Retrofit (`RetrofitProvider`, `BloodPressureApiService`), inyección simple a través de método estático.
- Caché de recomendaciones: tabla singleton `recommendation_cache` con TTL (`REC_CACHE_TTL_MS`).
- UI: Activities y Fragments con Material Components (no se usa DataBinding, solo findViewById).

## Personalización de TTL de caché
En `defaultConfig` (`app/build.gradle.kts`):
```
buildConfigField("long", "REC_CACHE_TTL_MS", "7200000L") // 2 horas
```
Cambia el valor (en milisegundos) si deseas otra caducidad.

## Mejores prácticas / Próximos pasos sugeridos
1. Reemplazar acceso estático de Retrofit por un patrón DI (Hilt/Dagger o manual) si crece el proyecto.
2. Añadir tests instrumentados para flujos CRUD en Room.
3. Implementar manejo de errores HTTP más granular (mapeando códigos a mensajes UI).
4. Internacionalización completa (al menos `en/` y `es/`).
5. Validación adicional en perfil (fechas futuras, género opcional). 
6. Migrar a ViewBinding para mayor seguridad en layouts.

## Seguridad
No dejes claves reales en el repositorio. Usa variables locales (`gradle.properties` local, no versionado) o un sistema de inyección seguro.

## Problemas conocidos
- Si no configuras API válida, las recomendaciones mostrarán mensajes de error/fallback.
- Advertencias de Lint (no críticas) quedan sobre concatenaciones previas residuales en algunas clases (puedes ejecutar `Analyze > Inspect Code`).

## Soporte
Si fallan las dependencias, fuerza refresco del wrapper y caché:
```cmd
gradlew.bat --refresh-dependencies clean assembleDebug
```

---
Hecho: el código se ha adaptado para usar `BuildConfig` para claves; revisa más abajo los archivos que modifiqué y siguientes pasos.
>>>>>>> 458329e (mi primer commit)
