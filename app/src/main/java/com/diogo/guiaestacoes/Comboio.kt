package com.diogo.guiaestacoes



data class Comboio(
    val numero: String = "",
    val tipo: String = "",
    val origem: String = "",
    val destino: String = "",
    val paragens: List<Paragem> = emptyList(),
    val estacoes_servidas: List<String> = emptyList()
)

data class Paragem(
    val estacao: String = "",
    val hora: String = ""
)