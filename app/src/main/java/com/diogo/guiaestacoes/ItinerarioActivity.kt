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

        // 1. Configurar Toolbar e Seta de Voltar
        val toolbar = findViewById<Toolbar>(R.id.toolbarItinerario)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // CORREÇÃO: Forçar o clique na seta de voltar
        toolbar.setNavigationOnClickListener { finish() }

        // CORREÇÃO: Empurrar a barra para baixo da câmara (Notch)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoOrigem = intent.getStringExtra("ESTACAO_ATUAL") ?: ""
        val destinoPesquisado = intent.getStringExtra("DESTINO_PESQUISADO") ?: ""

        if (comboio != null) {
            supportActionBar?.title = "Comboio ${comboio.numero}"
            val rv = findViewById<RecyclerView>(R.id.rvItinerario)
            rv.layoutManager = LinearLayoutManager(this)

            if (estacaoOrigem.isNotBlank()) {
                val origemLimpa = limparTexto(estacaoOrigem)

                // Corta o passado
                var paragens = comboio.paragens.dropWhile { limparTexto(it.estacao) != origemLimpa }

                // Corta o futuro extra se houver pesquisa de destino
                if (destinoPesquisado.isNotBlank()) {
                    val destLimpo = limparTexto(destinoPesquisado)
                    val idx = paragens.indexOfFirst { limparTexto(it.estacao).contains(destLimpo) }
                    if (idx != -1) paragens = paragens.take(idx + 1)
                }

                rv.adapter = ItinerarioAdapter(paragens)
            } else {
                rv.adapter = ItinerarioAdapter(comboio.paragens)
            }
        }
    }

    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }
}