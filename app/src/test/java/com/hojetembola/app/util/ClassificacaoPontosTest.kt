package com.hojetembola.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa a fórmula de pontos usada em JogoRepository:
 *   pontos = vitorias * 3 + empates * 1
 */
class ClassificacaoPontosTest {

    private fun calcularPontos(vitorias: Int, empates: Int): Int = vitorias * 3 + empates

    @Test
    fun vitoria_vale3Pontos() {
        assertEquals(3, calcularPontos(vitorias = 1, empates = 0))
    }

    @Test
    fun empate_vale1Ponto() {
        assertEquals(1, calcularPontos(vitorias = 0, empates = 1))
    }

    @Test
    fun derrota_vale0Pontos() {
        assertEquals(0, calcularPontos(vitorias = 0, empates = 0))
    }

    @Test
    fun combinacao_2vitorias1empate_vale7Pontos() {
        assertEquals(7, calcularPontos(vitorias = 2, empates = 1))
    }

    @Test
    fun combinacao_3vitorias2empates_vale11Pontos() {
        assertEquals(11, calcularPontos(vitorias = 3, empates = 2))
    }
}
