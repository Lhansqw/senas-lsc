# Reglas de R8 para la versión de producción (fase 12).
#
# MediaPipe, LiteRT y LiteRT-LM tienen código nativo (C++) que busca clases, campos y métodos de
# Java por su nombre mediante JNI. R8 no ve esas llamadas: si los renombra o los elimina, la app
# compila pero se cierra al cargar el modelo (SIGABRT). Se conservan enteros; ocupan poco frente a
# sus bibliotecas nativas. Ver https://github.com/google-ai-edge/mediapipe/pull/6247.
#
# Si la versión de producción falla, se puede compilar sin R8 para comprobar si es la causa:
#   gradlew.bat assembleRelease -Psenas.r8=false

# --- MediaPipe (tasks-vision) ---
-keep class com.google.mediapipe.** { *; }
-keep interface com.google.mediapipe.** { *; }
# Callbacks que el grafo nativo invoca, también en clases generadas para lambdas.
-keep class * implements com.google.mediapipe.framework.PacketCallback { *; }
-keep class * implements com.google.mediapipe.framework.PacketListCallback { *; }
-keep class * implements com.google.mediapipe.framework.PacketWithHeaderCallback { *; }
-dontwarn com.google.mediapipe.**

# MediaPipe lee sus opciones como protobuf ligero, que usa reflexión sobre los campos.
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.protobuf.**

# --- LiteRT (reconocedor de señas) ---
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

# --- LiteRT-LM (modelo de lenguaje local) ---
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# Anotaciones de compilación que algunas bibliotecas referencian pero no incluyen.
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**
-dontwarn org.checkerframework.**

# Conservar los números de línea en los informes de error (sin el nombre real del archivo).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
