package com.diogo.guiaestacoes

data class FotoEstacao(
    var id_foto: String = "",
    var id_estacao: String = "",
    var caminho_ficheiro: String = "",
    var ano: Long = 2024L,
    var legenda: String = "Adicionada via aplicação"
)