package com.diogo.guiaestacoes
import java.io.Serializable

// Adiciona "Serializable" aqui e nas Paragens
data class Comboio(
    val numero: String = "",
    val tipo: String = "",
    val origem: String = "",
    val destino: String = "",
    val paragens: List<Paragem> = emptyList(),
    val estacoes_servidas: List<String> = emptyList()
) : Serializable

data class Paragem(
    val estacao: String = "",
    val hora: String = ""
) : Serializable