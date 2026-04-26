package com.diogo.guiaestacoes
import java.io.Serializable

data class Comentario(
    val id_comentario: String = "",
    val id_estacao: String = "",
    val autor: String = "Viajante Anónimo",
    val texto: String = "",
    val estrelas: Float = 0f,
    val timestamp: Long = 0,
    val url_foto: String? = null
) : Serializable