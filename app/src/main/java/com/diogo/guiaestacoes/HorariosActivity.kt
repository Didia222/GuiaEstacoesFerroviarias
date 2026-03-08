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

    // Variável para guardar o nome da estação selecionada globalmente
    private var nomeEstacaoGlobal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)

        // Configuração da seta de voltar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Recupera o nome da estação vindo do mapa
        nomeEstacaoGlobal = intent.getStringExtra("ESTACAO_NOME") ?: ""
        supportActionBar?.title = if (nomeEstacaoGlobal.isNotEmpty()) "Horários: $nomeEstacaoGlobal" else "Horários de Comboios"

        recyclerView = findViewById(R.id.recyclerViewHorarios)
        etNumeroComboio = findViewById(R.id.etNumeroComboio)
        btnPesquisar = findViewById(R.id.btnPesquisar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // CORREÇÃO 1: Inicializa o adaptador com a lista vazia e o nome da estação
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        if (nomeEstacaoGlobal.isNotEmpty()) {
            etNumeroComboio.hint = "Horários para: $nomeEstacaoGlobal"
            pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        }

        btnPesquisar.setOnClickListener {
            val numero = etNumeroComboio.text.toString().trim()
            if (numero.isNotEmpty()) {
                pesquisarComboioPorNumero(numero)
            } else {
                Toast.makeText(this, "Escreve um número primeiro!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        db.collection("comboios")
            .get()
            .addOnSuccessListener { documents ->
                val listaOriginal = mutableListOf<Comboio>()

                for (document in documents) {
                    val comboio = document.toObject(Comboio::class.java)
                    val passaNestaEstacao = comboio.paragens.any { paragem ->
                        paragem.estacao.contains(nomeEstacao, ignoreCase = true)
                    }
                    if (passaNestaEstacao) {
                        listaOriginal.add(comboio)
                    }
                }

                if (listaOriginal.isNotEmpty()) {
                    // CORREÇÃO 2: Lógica de Agrupamento por Tipo e Ordenação por Hora Local
                    val listaMista = mutableListOf<Any>()

                    // Agrupamos os comboios pelo campo "tipo" (Ex: Urbano, Regional)
                    val grupos = listaOriginal.groupBy { it.tipo }

                    grupos.forEach { (tipo, comboios) ->
                        // Adicionamos o nome do tipo como um cabeçalho (String)
                        listaMista.add(tipo ?: "Outros")

                        // Ordenamos os comboios pela hora específica em que passam nesta estação
                        val ordenados = comboios.sortedBy { c ->
                            c.paragens.find { it.estacao.contains(nomeEstacao, true) }?.hora
                        }
                        listaMista.addAll(ordenados)
                    }

                    // CORREÇÃO 3: Atualiza o adaptador com a nova lista mista e estação
                    adapter.atualizarLista(listaMista, nomeEstacao)
                } else {
                    Toast.makeText(this, "Ainda não há comboios para $nomeEstacao", Toast.LENGTH_LONG).show()
                    adapter.atualizarLista(emptyList(), nomeEstacao)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro de ligação ao Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pesquisarComboioPorNumero(numeroInserido: String) {
        db.collection("comboios").document(numeroInserido).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val comboio = document.toObject(Comboio::class.java)
                    if (comboio != null) {
                        // Passamos o tipo como título e depois o objeto do comboio
                        val listaSimples = listOf(comboio.tipo ?: "Comboio", comboio)
                        adapter.atualizarLista(listaSimples, nomeEstacaoGlobal)
                    }
                } else {
                    Toast.makeText(this, "Comboio não encontrado", Toast.LENGTH_SHORT).show()
                    adapter.atualizarLista(emptyList(), nomeEstacaoGlobal)
                }
            }
    }
}