package com.diogo.guiaestacoes

import java.io.Serializable

// Não precisas de ": Any". Usamos "Serializable" para o caso de quereres
// enviar o comentário inteiro entre ecrãs.
data class Comentario(
    var id_comentario: String = "",
    var id_estacao: String = "",
    var autor: String = "Viajante Anónimo",
    var texto: String = "",
    var url_foto: String = "",
    var timestamp: Long = System.currentTimeMillis()
) : Serializable