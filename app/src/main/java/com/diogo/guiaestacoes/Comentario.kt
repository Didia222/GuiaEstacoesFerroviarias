package com.diogo.guiaestacoes

import java.io.Serializable

data class Comentario(
    var id_comentario: String = "",
    var id_estacao: String = "",
    var autor: String = "Viajante Anónimo",
    var texto: String = "",
    var url_foto: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var estrelas: Float = 0f
) : Serializable