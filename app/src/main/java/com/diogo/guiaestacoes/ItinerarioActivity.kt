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

        // Receber o pacote com os dados do comboio e a estação onde o utilizador clicou
        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoOrigem = intent.getStringExtra("ESTACAO_ATUAL") ?: ""

        if (comboio != null) {
            supportActionBar?.title = "Comboio ${comboio.numero}"

            val rv = findViewById<RecyclerView>(R.id.rvItinerario)
            rv.layoutManager = LinearLayoutManager(this)

            // Lógica de UX: Mostrar a viagem apenas daqui para a frente!
            if (estacaoOrigem.isNotEmpty()) {
                val estacaoOrigemLimpa = limparTexto(estacaoOrigem)

                // O dropWhile deita fora as estações anteriores até encontrar a nossa estação atual
                val paragensFiltradas = comboio.paragens.dropWhile {
                    val estacaoLimpa = limparTexto(it.estacao)
                    !estacaoLimpa.contains(estacaoOrigemLimpa) && !estacaoOrigemLimpa.contains(estacaoLimpa)
                }

                // Envia a lista já cortada para o ecrã
                rv.adapter = ItinerarioAdapter(paragensFiltradas)
            } else {
                // Se por algum motivo não soubermos a origem, mostra a viagem toda
                rv.adapter = ItinerarioAdapter(comboio.paragens)
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Executa a ação de voltar à página anterior
        return true
    }

    // Função de segurança que remove acentos e espaços extra para evitar erros de comparação
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.uppercase().replace("-", " ").replace("\\s+".toRegex(), " ").trim()
    }
}