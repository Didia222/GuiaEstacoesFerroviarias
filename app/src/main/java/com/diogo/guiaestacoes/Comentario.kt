package com.diogo.guiaestacoes // Confirma se o teu package se chama assim!

import java.io.Serializable

data class Comentario(
    var id_comentario: String = "",
    var id_estacao: String = "",
    var autor: String = "Viajante Anónimo",
    var texto: String = "",
    var url_foto: String = "",
    var timestamp: Long = System.currentTimeMillis()
) : Serializable