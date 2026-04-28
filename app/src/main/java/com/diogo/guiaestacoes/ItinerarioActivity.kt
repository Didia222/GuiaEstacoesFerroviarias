package com.diogo.guiaestacoes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

class ItinerarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerario)

        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoAtual = intent.getStringExtra("ESTACAO_ATUAL") ?: ""
        val destinoPesquisado = intent.getStringExtra("DESTINO_PESQUISADO") ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbarItinerario)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.title = "Comboio Nº ${comboio?.numero}"

        // Ajuste para a câmara frontal
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        // Filtra as paragens desde a atual ATÉ ao destino que o user pesquisou
        val paragensUteis = filtrarAteDestino(comboio, estacaoAtual, destinoPesquisado)

        val rv = findViewById<RecyclerView>(R.id.rvItinerario)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ItinerarioAdapter(paragensUteis, estacaoAtual)
    }

    private fun filtrarAteDestino(comboio: Comboio?, atual: String, pesquisado: String): List<Paragem> {
        if (comboio == null) return emptyList()
        val lista = comboio.paragens
        val atualLimpa = limparTexto(atual)

        // Se houver pesquisa, o fim da lista é a paragem pesquisada.
        // Se não houver, é o destino final oficial do comboio.
        val destinoAlvo = if (pesquisado.isNotBlank()) limparTexto(pesquisado) else limparTexto(comboio.destino)

        val indexInicio = lista.indexOfFirst { limparTexto(it.estacao) == atualLimpa }
        val indexFim = lista.indexOfFirst { limparTexto(it.estacao).contains(destinoAlvo) }

        return when {
            indexInicio != -1 && indexFim != -1 && indexFim >= indexInicio -> {
                lista.subList(indexInicio, indexFim + 1)
            }
            indexInicio != -1 -> {
                lista.subList(indexInicio, lista.size)
            }
            else -> lista
        }
    }

    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }
}