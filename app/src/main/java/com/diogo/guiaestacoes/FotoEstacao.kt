package com.diogo.guiaestacoes

import androidx.annotation.Keep // <-- ADICIONAR ISTO

@Keep // <-- E ISTO
data class FotoEstacao(
    var id_foto: String = "",
    var id_estacao: String = "",
    var caminho_ficheiro: String = "",
    var ano: Long = 2024L,
    var legenda: String = "Adicionada via aplicação"
)