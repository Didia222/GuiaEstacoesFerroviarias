package com.diogo.guiaestacoes

import org.junit.Assert.assertEquals
import org.junit.Test

class MetadadosEstacaoTest {

    // Lógica que processa os metadados brutos e decide o estado da interface
    private fun determinarEstadoOperacional(anoEncerramento: String?): String {
        return if (anoEncerramento.isNullOrBlank() || anoEncerramento.lowercase() == "null") {
            "Operacional"
        } else {
            "Encerrada em $anoEncerramento"
        }
    }

    @Test
    fun `deve retornar Operacional quando nao ha data de encerramento`() {
        // Arrange
        val dataNula: String? = null
        val dataVazia = ""

        // Act
        val res1 = determinarEstadoOperacional(dataNula)
        val res2 = determinarEstadoOperacional(dataVazia)

        // Assert
        assertEquals("Operacional", res1)
        assertEquals("Operacional", res2)
    }

    @Test
    fun `deve retornar o ano de encerramento quando a estacao foi desativada`() {
        // Arrange
        val anoFim = "1990"

        // Act
        val resultado = determinarEstadoOperacional(anoFim)

        // Assert
        assertEquals("Encerrada em 1990", resultado)
    }
}