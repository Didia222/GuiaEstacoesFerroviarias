package com.diogo.guiaestacoes

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HorariosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComboioAdapter
    private lateinit var etNumeroComboio: EditText
    private lateinit var btnPesquisar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)

// Ativa a seta de voltar atrás
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

// Faz a seta funcionar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Horários Próximos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Horários de Comboios"
        recyclerView = findViewById(R.id.recyclerViewHorarios)
        etNumeroComboio = findViewById(R.id.etNumeroComboio)
        btnPesquisar = findViewById(R.id.btnPesquisar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComboioAdapter(emptyList())
        recyclerView.adapter = adapter

        // 1. A MAGIA ACONTECE AQUI: Ler a estação que clicaste no Mapa!
        val nomeEstacaoDoMapa = intent.getStringExtra("ESTACAO_NOME")

        if (nomeEstacaoDoMapa != null) {
            // Muda o texto da caixa de pesquisa para saberes onde estás
            etNumeroComboio.hint = "Horários para: $nomeEstacaoDoMapa"

            // Vai logo ao Firebase procurar os comboios desta estação
            pesquisarComboiosDaEstacao(nomeEstacaoDoMapa)
        }

        // 2. O botão de procurar continua a funcionar se quiseres usar um Número
        btnPesquisar.setOnClickListener {
            val numero = etNumeroComboio.text.toString().trim()
            if (numero.isNotEmpty()) {
                pesquisarComboioPorNumero(numero)
            } else {
                Toast.makeText(this, "Escreve um número primeiro!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- FUNÇÃO NOVA: Procura todos os comboios que param na Estação ---
    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        Toast.makeText(this, "A procurar comboios para $nomeEstacao...", Toast.LENGTH_SHORT).show()

        db.collection("comboios")
            .get() // Pede TODOS os comboios ao Firebase
            .addOnSuccessListener { documents ->
                val listaDaEstacao = mutableListOf<Comboio>()

                for (document in documents) {
                    val comboio = document.toObject(Comboio::class.java)

                    // O Kotlin vai ver se a tua estação está dentro da lista de paragens deste comboio
                    val passaNestaEstacao = comboio.paragens.any { paragem ->
                        paragem.estacao.contains(nomeEstacao, ignoreCase = true)
                    }

                    if (passaNestaEstacao) {
                        listaDaEstacao.add(comboio)
                    }
                }

                if (listaDaEstacao.isNotEmpty()) {
                    adapter.atualizarLista(listaDaEstacao)
                } else {
                    Toast.makeText(this, "Ainda não há comboios para $nomeEstacao", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro de ligação ao Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- FUNÇÃO ANTIGA: Procura apenas por um Número exato ---
    private fun pesquisarComboioPorNumero(numeroInserido: String) {
        db.collection("comboios").document(numeroInserido).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val comboio = document.toObject(Comboio::class.java)
                    if (comboio != null) {
                        adapter.atualizarLista(listOf(comboio))
                    }
                } else {
                    Toast.makeText(this, "Comboio não encontrado", Toast.LENGTH_SHORT).show()
                    adapter.atualizarLista(emptyList())
                }
            }
    }
}