package com.diogo.guiaestacoes

import android.os.Bundle
import android.util.Log
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

    private var nomeEstacaoGlobal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        nomeEstacaoGlobal = intent.getStringExtra("ESTACAO_NOME") ?: ""
        supportActionBar?.title = if (nomeEstacaoGlobal.isNotEmpty()) "Horários: $nomeEstacaoGlobal" else "Horários de Comboios"

        recyclerView = findViewById(R.id.recyclerViewHorarios)
        etNumeroComboio = findViewById(R.id.etNumeroComboio)
        btnPesquisar = findViewById(R.id.btnPesquisar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        if (nomeEstacaoGlobal.isNotEmpty()) {
            etNumeroComboio.hint = "Procurar em: $nomeEstacaoGlobal"
            pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        } else {
            Toast.makeText(this, "Nome da estação não recebido", Toast.LENGTH_SHORT).show()
        }

        btnPesquisar.setOnClickListener {
            val textoPesquisa = etNumeroComboio.text.toString().trim()
            if (textoPesquisa.isNotEmpty()) {
                executarPesquisaSmarter(textoPesquisa)
            } else {
                pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
            }
        }
    }

    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        db.collection("comboios").get().addOnSuccessListener { documents ->
            val listaOriginal = mutableListOf<Comboio>()
            for (document in documents) {
                try {
                    val comboio = document.toObject(Comboio::class.java)
                    val passaNestaEstacao = comboio.paragens.any {
                        it.estacao.contains(nomeEstacao, ignoreCase = true)
                    }
                    if (passaNestaEstacao) {
                        listaOriginal.add(comboio)
                    }
                } catch (e: Exception) {
                    Log.e("FIREBASE", "Erro ao converter comboio: ${e.message}")
                }
            }

            if (listaOriginal.isNotEmpty()) {
                processarEExibirLista(listaOriginal)
            } else {
                Toast.makeText(this, "Nenhum comboio encontrado para esta estação", Toast.LENGTH_LONG).show()
                // Limpa a lista se não houver resultados
                adapter.atualizarLista(emptyList(), nomeEstacaoGlobal)
            }
        }.addOnFailureListener { exception ->
            Log.e("FIREBASE", "Erro ao ler do Firestore", exception)
            Toast.makeText(this, "Erro de rede: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun executarPesquisaSmarter(texto: String) {
        db.collection("comboios").get().addOnSuccessListener { documents ->
            val resultados = mutableListOf<Comboio>()
            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)
                val indexAtual = comboio.paragens.indexOfFirst {
                    it.estacao.contains(nomeEstacaoGlobal, ignoreCase = true)
                }

                if (indexAtual != -1) {
                    val matchesNumero = comboio.numero.contains(texto)
                    val matchesDestinoFinal = comboio.destino.contains(texto, ignoreCase = true)
                    
                    val paragensFuturas = comboio.paragens.subList(indexAtual + 1, comboio.paragens.size)
                    val matchesParagemFutura = paragensFuturas.any {
                        it.estacao.contains(texto, ignoreCase = true)
                    }

                    if (matchesNumero || matchesDestinoFinal || matchesParagemFutura) {
                        resultados.add(comboio)
                    }
                }
            }

            if (resultados.isNotEmpty()) {
                processarEExibirLista(resultados)
            } else {
                Toast.makeText(this, "Nenhum resultado para '$texto'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processarEExibirLista(listaComboios: List<Comboio>) {
        val listaMista = mutableListOf<Any>()
        val grupos = listaComboios.groupBy { it.tipo }

        grupos.forEach { (tipo, comboios) ->
            listaMista.add(tipo ?: "Outros")
            val ordenados = comboios.sortedBy { c ->
                c.paragens.find { it.estacao.contains(nomeEstacaoGlobal, true) }?.hora
            }
            listaMista.addAll(ordenados)
        }
        adapter.atualizarLista(listaMista, nomeEstacaoGlobal)
    }
}
