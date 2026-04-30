package com.diogo.guiaestacoes

import org.junit.Assert.assertEquals
import org.junit.Test

class ItinerarioLogicaTest {

    // A tua função de lógica pura, extraída para ser testável sem precisar do ecrã (Activity)
    private fun filtrarParagens(
        paragensTotais: List<String>,
        estacaoOndeEstou: String,
        destinoPesquisado: String
    ): List<String> {
        val indexInicio = paragensTotais.indexOf(estacaoOndeEstou)
        val indexFim = paragensTotais.indexOf(destinoPesquisado)

        return if (indexInicio != -1 && indexFim != -1 && indexFim >= indexInicio) {
            paragensTotais.subList(indexInicio, indexFim + 1)
        } else if (indexInicio != -1) {
            paragensTotais.subList(indexInicio, paragensTotais.size)
        } else {
            paragensTotais
        }
    }

    @Test
    fun `deve cortar estacoes passadas e futuras alem do destino`() {
        // Arrange: Simulamos a Linha do Douro
        val linhaCompleta = listOf("Porto", "Ermesinde", "Paredes", "Penafiel", "Caíde", "Marco")

        val estacaoAtual = "Paredes"
        val destino = "Caíde"

        // O que esperamos: Não tem o Porto (já passou) nem o Marco (é depois de onde vou sair)
        val viagemEsperada = listOf("Paredes", "Penafiel", "Caíde")

        // Act
        val resultado = filtrarParagens(linhaCompleta, estacaoAtual, destino)

        // Assert
        assertEquals("A lógica de corte dinâmico falhou", viagemEsperada, resultado)
    }

    @Test
    fun `deve mostrar ate ao fim da linha se nao houver destino pesquisado`() {
        // Arrange
        val linhaCompleta = listOf("Lisboa", "Coimbra", "Aveiro", "Porto")
        val estacaoAtual = "Coimbra"
        val destinoInexistente = "" // Simulando que o utilizador não usou a barra de pesquisa

        val viagemEsperada = listOf("Coimbra", "Aveiro", "Porto")

        // Act
        val resultado = filtrarParagens(linhaCompleta, estacaoAtual, destinoInexistente)

        // Assert
        assertEquals("Devia mostrar até ao fim comercial da linha", viagemEsperada, resultado)
    }
}