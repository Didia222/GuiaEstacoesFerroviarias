package com.diogo.guiaestacoes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

class ItinerarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerario)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarItinerario)
        setSupportActionBar(toolbar)

        // Empurra a barra para baixo para não bater na câmara (notch) do telemóvel
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBar = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // Ativa a seta de voltar na barra superior
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 1. Receber as três "bagagens" que vêm do ecrã anterior
        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoOrigem = intent.getStringExtra("ESTACAO_ATUAL") ?: ""
        val destinoPesquisado = intent.getStringExtra("DESTINO_PESQUISADO") ?: ""

        if (comboio != null) {
            supportActionBar?.title = "Comboio ${comboio.numero}"

            val rv = findViewById<RecyclerView>(R.id.rvItinerario)
            rv.layoutManager = LinearLayoutManager(this)

            // Lógica de UX: Mostrar a viagem apenas daqui para a frente (e cortar o futuro desnecessário)
            if (estacaoOrigem.isNotEmpty()) {
                val estacaoOrigemLimpa = limparTexto(estacaoOrigem)

                // 2. Corta o Passado (Deita fora as estações antes de onde estamos)
                var paragensFiltradas = comboio.paragens.dropWhile {
                    val estacaoLimpa = limparTexto(it.estacao)
                    !estacaoLimpa.contains(estacaoOrigemLimpa) && !estacaoOrigemLimpa.contains(estacaoLimpa)
                }

                // 3. Corta o Futuro Extra (Se o utilizador pesquisou "Porto", não mostra as estações a seguir ao Porto)
                if (destinoPesquisado.isNotEmpty()) {
                    val destinoLimpo = limparTexto(destinoPesquisado)

                    // Encontra em que número da lista ficou a estação de destino
                    val indexDestino = paragensFiltradas.indexOfFirst {
                        val estacaoLimpa = limparTexto(it.estacao)
                        estacaoLimpa.contains(destinoLimpo) || destinoLimpo.contains(estacaoLimpa)
                    }

                    // Se a encontrou, usa a função "take" para agarrar só as estações do início até a esse número
                    if (indexDestino != -1) {
                        paragensFiltradas = paragensFiltradas.take(indexDestino + 1)
                    }
                }

                // Envia a lista final "cirúrgica" para o ecrã!
                rv.adapter = ItinerarioAdapter(paragensFiltradas)
            } else {
                rv.adapter = ItinerarioAdapter(comboio.paragens)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Executa a ação de voltar à página anterior
        return true
    }

    // A nossa função tratora de segurança
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.uppercase().replace("-", " ").replace("\\s+".toRegex(), " ").trim()
    }
}