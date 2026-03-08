package com.diogo.guiaestacoes
import java.io.Serializable
// data class:  são utilizadas para que a app consegue organizar os dados da base de dados para conseguir interpretar a informação do firebase

data class Paragem(
    val estacao: String = "", // Define o nome da estação ou apeadeiro
    val hora: String = "" // Define a hora de chegada ou partida
) : Serializable

data class Comboio(
    val numero: String = "", // Define o número do comboio
    val tipo: String = "", // define o tipo de comboio
    val origem: String = "", //define a origem de partida do comboio
    val destino: String = "",// define o destino do comboio
    val paragens: List<Paragem> = listOf() // define a lista de objetos para que um único comboio contenha dezenas de paragens

) : Serializable
