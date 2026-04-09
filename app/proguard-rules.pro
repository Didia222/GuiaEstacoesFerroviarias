# Regras para o Firebase não se perder com as Classes de Dados
-keep class com.diogo.guiaestacoes.Comentario { *; }
-keep class com.diogo.guiaestacoes.Estacao { *; }
-keep class com.diogo.guiaestacoes.Comboio { *; }
-keep class com.diogo.guiaestacoes.FotoEstacao { *; }

# Mantém os nomes genéricos para o Firebase Firestore
-keepattributes Signature
-keepclassmembers class * {
  @com.google.firebase.firestore.PropertyName <fields>;
}