package com.diogo.guiaestacoes

import androidx.annotation.Keep // <-- ADICIONAR ISTO
import java.io.Serializable

@Keep // <-- E ISTO
data class Estacao(
    var nome: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var Discricao_hist: String = ""
    // ... (deixa os restantes campos como já os tens)
) : Serializable