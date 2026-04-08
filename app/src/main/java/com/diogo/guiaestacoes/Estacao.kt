package com.diogo.guiaestacoes

import java.io.Serializable

data class Estacao(
    val id_estacao: String = "",
    val nome: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val Discricao_hist: String = "",
    val Id_linha: Int = 1
) : Serializable