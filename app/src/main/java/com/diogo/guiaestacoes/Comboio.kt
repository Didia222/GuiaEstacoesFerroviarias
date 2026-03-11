package com.diogo.guiaestacoes

import java.io.Serializable

data class Paragem(
    val estacao: String = "",
    val hora: String = ""
) : Serializable // como o telemóvel é muito rigoroso e nao deixa passar objetos grandes esta palavra faz o programa embrulhar o ecra dos comboios para o itenerário para nao haver perda de informação pelo caminho.

data class Comboio(
    val numero: String = "",
    val tipo: String = "",
    val origem: String = "",
    val destino: String = "",
    val paragens: List<Paragem> = listOf()
) : Serializable