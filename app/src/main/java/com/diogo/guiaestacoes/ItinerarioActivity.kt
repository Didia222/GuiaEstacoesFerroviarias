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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 1. Receber os dados enviados pelo ComboioAdapter
        val comboio = intent.getSerializableExtra("COMBOIO_OBJ") as? Comboio
        val estacaoOrigem = intent.getStringExtra("ESTACAO_ORIGEM") ?: ""

        if (comboio != null) {
            supportActionBar?.title = "Comboio ${comboio.numero}"

            // 2. FILTRAGEM: Mostra apenas a partir da estação selecionada
            val paragensFiltradas = comboio.paragens.dropWhile {
                !it.estacao.contains(estacaoOrigem, ignoreCase = true)
            }

            // 3. Configurar a RecyclerView
            val rv = findViewById<RecyclerView>(R.id.rvItinerario)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = ItinerarioAdapter(paragensFiltradas)
        }
    }
}