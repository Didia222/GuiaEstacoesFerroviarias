package com.diogo.guiaestacoes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

class GeolocalizacaoTest {

    // Simulação da lógica matemática (Fórmula de Haversine) usada para medir distâncias por GPS
    private fun estaDentroDoRaio(
        latUtilizador: Double, lonUtilizador: Double,
        latEstacao: Double, lonEstacao: Double,
        raioMaximoKm: Double
    ): Boolean {
        val raioTerra = 6371.0 // Raio da Terra em km
        val dLat = Math.toRadians(latEstacao - latUtilizador)
        val dLon = Math.toRadians(lonEstacao - lonUtilizador)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(latUtilizador)) * cos(Math.toRadians(latEstacao)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distancia = raioTerra * c

        return distancia <= raioMaximoKm
    }

    @Test
    fun `deve retornar true se a estacao estiver a menos de 10km`() {
        // Arrange
        val latPenafiel = 41.2064
        val lonPenafiel = -8.2831
        val latParedes = 41.2016 // Paredes fica a cerca de 6km de Penafiel
        val lonParedes = -8.3333

        // Act
        val resultado = estaDentroDoRaio(latPenafiel, lonPenafiel, latParedes, lonParedes, 10.0)

        // Assert
        assertTrue("Paredes devia estar no raio de 10km de Penafiel", resultado)
    }

    @Test
    fun `deve retornar false se a estacao estiver a mais de 10km`() {
        // Arrange
        val latPenafiel = 41.2064
        val lonPenafiel = -8.2831
        val latPorto = 41.1496 // Porto fica a mais de 30km
        val lonPorto = -8.6109

        // Act
        val resultado = estaDentroDoRaio(latPenafiel, lonPenafiel, latPorto, lonPorto, 10.0)

        // Assert
        assertFalse("Porto NÃO devia estar no raio de 10km de Penafiel", resultado)
    }
}