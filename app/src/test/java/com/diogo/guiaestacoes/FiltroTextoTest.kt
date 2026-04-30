package com.diogo.guiaestacoes

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Normalizer

class FiltroTextoTest {

    // Isolamos a função exatamente como a tens no teu código principal
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .uppercase()
    }

    @Test
    fun `deve remover acentos e converter para maiusculas`() {
        // Arrange (Preparar os dados de entrada e o resultado esperado)
        val input = "São Bento"
        val esperado = "SAO BENTO"

        // Act (Executar a função)
        val resultado = limparTexto(input)

        // Assert (Verificar se bate certo)
        assertEquals("O texto com acentos não foi limpo corretamente", esperado, resultado)
    }

    @Test
    fun `deve substituir hifens por espacos`() {
        // Arrange
        val input = "Marco-de-Canaveses"
        val esperado = "MARCO DE CANAVESES"

        // Act
        val resultado = limparTexto(input)

        // Assert
        assertEquals("Os hífenes não foram substituídos", esperado, resultado)
    }

    @Test
    fun `deve remover espacos extra no inicio e no fim`() {
        // Arrange
        val input = "   Porto Campanhã   "
        val esperado = "PORTO CAMPANHA"

        // Act
        val resultado = limparTexto(input)

        // Assert
        assertEquals("Os espaços em branco extra não foram removidos", esperado, resultado)
    }
}