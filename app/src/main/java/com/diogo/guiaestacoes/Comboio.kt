package com.diogo.guiaestacoes

import java.io.Serializable

data class Paragem(
    val estacao: String = "",
    val hora: String = ""
) : Serializable

data class Comboio(
    val id_comboio: String = "",
    val numero: String = "",
    val tipo: String = "",
    val origem: String = "",
    val destino: String = "",
    val paragens: List<Paragem> = listOf()
) : Serializable