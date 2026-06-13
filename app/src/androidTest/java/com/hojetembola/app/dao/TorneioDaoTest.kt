package com.hojetembola.app.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hojetembola.app.data.local.AppDatabase
import com.hojetembola.app.data.local.entity.TorneioEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TorneioDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun torneio(id: String = "1", synced: Boolean = true) = TorneioEntity(
        id = id,
        nome = "Liga Verão",
        modalidade = "fut7",
        formato = "liga",
        maxEquipas = 8,
        maxJogadoresPorEquipa = 12,
        dataInicioInscricoes = "2026-01-01",
        dataFimInscricoes = "2026-01-10",
        dataInicio = "2026-01-15",
        dataFimPrevista = "2026-03-01",
        localizacaoNome = "Campo Principal",
        estado = "Criado",
        organizadorId = "org1",
        isSynced = synced
    )

    @Test
    fun inserir_e_buscarPorId_retornaTorneioCorreto() = runTest {
        val t = torneio()
        db.torneioDao().insert(t)

        val resultado = db.torneioDao().getById("1")
        assertNotNull(resultado)
        assertEquals("Liga Verão", resultado?.nome)
        assertEquals("fut7", resultado?.modalidade)
    }

    @Test
    fun getPendentesSync_retornaApenasNaoSincronizados() = runTest {
        db.torneioDao().insert(torneio(id = "1", synced = true))
        db.torneioDao().insert(torneio(id = "2", synced = false))
        db.torneioDao().insert(torneio(id = "3", synced = false))

        val pendentes = db.torneioDao().getPendentesSync()
        assertEquals(2, pendentes.size)
        assertTrue(pendentes.none { it.isSynced })
    }

    @Test
    fun markAsSynced_atualizaEstadoNaBase() = runTest {
        db.torneioDao().insert(torneio(id = "1", synced = false))
        db.torneioDao().markAsSynced("1")

        val t = db.torneioDao().getById("1")
        assertTrue(t?.isSynced == true)
    }

    @Test
    fun deleteById_removeTorneio() = runTest {
        db.torneioDao().insert(torneio(id = "1"))
        db.torneioDao().deleteById("1")

        assertNull(db.torneioDao().getById("1"))
    }

    @Test
    fun getAll_retornaListaOrdenadaPorDataDesc() = runTest {
        db.torneioDao().insert(torneio(id = "1").copy(dataInicio = "2026-01-01"))
        db.torneioDao().insert(torneio(id = "2").copy(dataInicio = "2026-06-01"))

        val todos = db.torneioDao().getAll().first()
        assertEquals("2", todos.first().id)
    }
}
