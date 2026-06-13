package com.hojetembola.app.entity

import com.hojetembola.app.data.local.entity.TorneioEntity
import com.hojetembola.app.data.local.entity.minJogadoresPorEquipa
import org.junit.Assert.assertEquals
import org.junit.Test

class TorneioEntityTest {

    private fun torneio(modalidade: String, personalizado: Int? = null) = TorneioEntity(
        id = "1",
        nome = "Torneio Teste",
        modalidade = modalidade,
        numJogadoresPersonalizado = personalizado,
        formato = "liga",
        maxEquipas = 8,
        maxJogadoresPorEquipa = 15,
        dataInicioInscricoes = "2026-01-01",
        dataFimInscricoes = "2026-01-10",
        dataInicio = "2026-01-15",
        dataFimPrevista = "2026-03-01",
        localizacaoNome = "Campo Teste",
        estado = "Criado",
        organizadorId = "org1"
    )

    @Test
    fun minJogadoresPorEquipa_fut5_returns5() {
        assertEquals(5, torneio("fut5").minJogadoresPorEquipa())
    }

    @Test
    fun minJogadoresPorEquipa_fut7_returns7() {
        assertEquals(7, torneio("fut7").minJogadoresPorEquipa())
    }

    @Test
    fun minJogadoresPorEquipa_fut11_returns11() {
        assertEquals(11, torneio("fut11").minJogadoresPorEquipa())
    }

    @Test
    fun minJogadoresPorEquipa_personalizado_returnsCustomValue() {
        assertEquals(6, torneio("personalizado", personalizado = 6).minJogadoresPorEquipa())
    }

    @Test
    fun minJogadoresPorEquipa_personalizadoSemValor_returns1() {
        assertEquals(1, torneio("personalizado", personalizado = null).minJogadoresPorEquipa())
    }
}
