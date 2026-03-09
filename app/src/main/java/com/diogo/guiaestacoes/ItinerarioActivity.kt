package com.diogo.guiaestacoes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItinerarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerario)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarItinerario)
        setSupportActionBar(toolbar)
        // Adiciona dentro do onCreate logo após o find da toolbar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBar = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // Ativa a seta de voltar na barra superior
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Receber os dados enviados pelo ComboioAdapter
        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoOrigem = intent.getStringExtra("ESTACAO_ORIGEM") ?: ""

        if (comboio != null) {
            supportActionBar?.title = "Comboio ${comboio.numero}"

            // FILTRAGEM: Mostra apenas a partir da estação selecionada
            val paragensFiltradas = comboio.paragens.dropWhile {
                !it.estacao.contains(estacaoOrigem, ignoreCase = true)
            }

            val rv = findViewById<RecyclerView>(R.id.rvItinerario)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = ItinerarioAdapter(paragensFiltradas)
        }
    }

    // MÉTODO CORRETO PARA A SETA FUNCIONAR
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Executa a ação de voltar
        return true
    }
}