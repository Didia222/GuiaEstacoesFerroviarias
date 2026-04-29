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

        // Recebe os dados serializado da activity Horários
        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoAtual = intent.getStringExtra("ESTACAO_ATUAL") ?: ""
        val destinoPesquisado = intent.getStringExtra("DESTINO_PESQUISADO") ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbarItinerario)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.title = "Comboio Nº ${comboio?.numero}"

        // RNF de Adaptabilidade de Hardware. Garante que em telemóveis modernos
        // a câmara frontal (notch) não sobrepõe a barra e a seta de voltar atrás.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        // Filtra as paragens desde a atual ATÉ ao destino que o utilizador pesquisou
        val paragensUteis = filtrarAteDestino(comboio, estacaoAtual, destinoPesquisado)

        val rv = findViewById<RecyclerView>(R.id.rvItinerario)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ItinerarioAdapter(paragensUteis, estacaoAtual)
    }

    // [LÓGICA CORE - CORTE DINÂMICO]
    // Esta função corta as extremidades inúteis do array original do comboio.
    private fun filtrarAteDestino(comboio: Comboio?, atual: String, pesquisado: String): List<Paragem> {
        if (comboio == null) return emptyList()
        val lista = comboio.paragens
        val atualLimpa = limparTexto(atual)

        // Se houver pesquisa, o fim da lista é a paragem pesquisada.
        // Se não houver, é o destino final oficial do comboio.
        val destinoAlvo = if (pesquisado.isNotBlank()) limparTexto(pesquisado) else limparTexto(comboio.destino)
        // 1. Descobre o índice da estação onde o utilizador vai entrar (Ínicio)
        val indexInicio = lista.indexOfFirst { limparTexto(it.estacao) == atualLimpa }
        // 2. Descobre o índice da estação onde o utilizador vai sair (Fim)
        val indexFim = lista.indexOfFirst { limparTexto(it.estacao).contains(destinoAlvo) }

        return when {
            // Caso ideal: Encontrou o ínicio e o fim e a ordem é logica. Retorna só esse bocado.
            indexInicio != -1 && indexFim != -1 && indexFim >= indexInicio -> {
                lista.subList(indexInicio, indexFim + 1)
            }
            // Fallback: Não encontrou o fim (ex: utilizador pesquisou algo que não existe ali), mostra do inicio até ao fim da linha.

            indexInicio != -1 -> {
                lista.subList(indexInicio, lista.size)
            }
            // Retorna o original apenas se tudo falhar (segurança contra crashes)
            else -> lista
        }
    }

    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }
}