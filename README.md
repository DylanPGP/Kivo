# Kivo Pass (beta)

Gestor de contraseñas para Android (Kotlin + Jetpack Compose). Todo se guarda cifrado en el dispositivo; no usa red ni permisos de internet.

## Seguridad
| Elemento | Implementación |
|---|---|
| Derivación de claves | Argon2id (32 MiB, 3 iteraciones, 2 hilos) |
| Cifrado de datos | AES-256-GCM |
| Salt | 16 bytes aleatorios |
| Nonce AES-GCM | 12 bytes aleatorios, nuevo en cada cifrado |
| Autenticación | Tag de 16 bytes |
| Aleatoriedad | `SecureRandom` |
| Claves auxiliares | Android Keystore (envoltura de la clave y clave biométrica) |

Flujo: PIN/contraseña → Argon2id → KEK → descifra la clave de la bóveda (VK, 256 bits) → que a su vez está envuelta por una clave del Keystore. La huella guarda otra copia de VK bajo una clave del Keystore que exige biometría.

Extras: `FLAG_SECURE` (sin capturas), bloqueo automático, portapapeles sensible que se borra a los 30 s, bloqueo progresivo tras 5 intentos fallidos, `allowBackup=false`.

## Funciones
- Inicio vacío, botón **+** abajo a la derecha.
- Tipos: **Cuenta** (enlace opcional, usuario/correo, contraseña, TOTP Base32 opcional), **Tarjeta** y **Frase semilla**.
- Mantén presionado (o toca) un elemento para ver el detalle con copiar / mostrar / TOTP en vivo.
- Ajustes (arriba a la derecha): español / inglés, huella, cambiar PIN/contraseña, bloqueo automático.
- Acceso con PIN de 5 dígitos o contraseña, y huella dactilar opcional.

## Compilar
GitHub Actions (`.github/workflows/build.yml`) compila el APK en cada push y lo sube como artefacto (`KivoPass-debug` y `KivoPass-release`).
Local: Android Studio, o `gradle assembleDebug` con Gradle 8.7 y JDK 17.

> Beta: si olvidas el PIN/contraseña no hay recuperación. Aún no hay exportar/importar.
