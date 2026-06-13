package com.hojetembola.app.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hojetembola.app.data.local.AppDatabase
import com.hojetembola.app.data.local.entity.EventoJogoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventoJogoDaoTest {

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

    private fun evento(id: String, jogoId: String = "10", tipo: String = "golo", synced: Boolean = true) =
        EventoJogoEntity(
            id = id,
            jogoId = jogoId,
            tipo = tipo,
            minuto = 23,
            jogadorId = "jogador1",
            equipaId = "equipa1",
            isSynced = synced
        )

    @Test
    fun inserir_e_getPendentesSync_retornaApenasOffline() = runTest {
        db.eventoJogoDao().insert(evento("e1", synced = true))
        db.eventoJogoDao().insert(evento("e2", synced = false))
        db.eventoJogoDao().insert(evento("e3", synced = false))

        val pendentes = db.eventoJogoDao().getPendentesSync()
        assertEquals(2, pendentes.size)
        assertTrue(pendentes.all { !it.isSynced })
    }

    @Test
    fun getPendentesSync_semOffline_retornaListaVazia() = runTest {
        db.eventoJogoDao().insert(evento("e1", synced = true))
        db.eventoJogoDao().insert(evento("e2", synced = true))

        val pendentes = db.eventoJogoDao().getPendentesSync()
        assertTrue(pendentes.isEmpty())
    }

    @Test
    fun deleteById_removeEventoCorreto() = runTest {
        db.eventoJogoDao().insert(evento("e1", synced = false))
        db.eventoJogoDao().insert(evento("e2", synced = false))
        db.eventoJogoDao().deleteById("e1")

        val pendentes = db.eventoJogoDao().getPendentesSync()
        assertEquals(1, pendentes.size)
        assertEquals("e2", pendentes.first().id)
    }

    @Test
    fun inserir_multiplosTipos_guardaTipoCorreto() = runTest {
        db.eventoJogoDao().insert(evento("e1", tipo = "golo"))
        db.eventoJogoDao().insert(evento("e2", tipo = "amarelo"))
        db.eventoJogoDao().insert(evento("e3", tipo = "vermelho"))

        val todos = db.eventoJogoDao().getByJogo("10").first()
        assertEquals(3, todos.size)
        assertTrue(todos.any { it.tipo == "golo" })
        assertTrue(todos.any { it.tipo == "amarelo" })
        assertTrue(todos.any { it.tipo == "vermelho" })
    }
}
