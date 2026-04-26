package com.diogo.guiaestacoes

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class HorariosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComboioAdapter
    private lateinit var svPesquisaHorarios: SearchView

    private var nomeEstacaoGlobal: String = ""
    private var todosOsComboiosDaEstacao = listOf<Comboio>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        // Configuração da Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nomeEstacaoGlobal = intent.getStringExtra("ESTACAO_NOME") ?: ""
        supportActionBar?.title = "Horários: $nomeEstacaoGlobal"

        // Inicializar Views
        recyclerView = findViewById(R.id.recyclerViewHorarios)
        svPesquisaHorarios = findViewById(R.id.svPesquisaHorarios)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        if (nomeEstacaoGlobal.isNotEmpty()) {
            carregarComboiosOtimizado(nomeEstacaoGlobal)
        }

        configurarPesquisa()
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "").uppercase().trim()
    }

    private fun carregarComboiosOtimizado(nomeEstacao: String) {
        val nomeBusca = limparTexto(nomeEstacao)

        // RF-5: Pesquisa direta no campo estacoes_servidas (muito mais rápido)
        db.collection("Comboio")
            .whereArrayContains("estacoes_servidas", nomeBusca)
            .get()
            .addOnSuccessListener { documents ->
                val lista = documents.toObjects(Comboio::class.java)
                todosOsComboiosDaEstacao = lista
                exibirResultados(todosOsComboiosDaEstacao)
            }
            .addOnFailureListener { e ->
                Log.e("HorariosActivity", "Erro Firebase", e)
                Toast.makeText(this, "Erro ao carregar horários", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarPesquisa() {
        svPesquisaHorarios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarNaMemoria(newText ?: "")
                return true
            }
        })
    }

    private fun filtrarNaMemoria(texto: String) {
        if (texto.isEmpty()) {
            exibirResultados(todosOsComboiosDaEstacao)
            return
        }

        val busca = limparTexto(texto)
        val filtrados = todosOsComboiosDaEstacao.filter { comboio ->
            comboio.numero.contains(busca) || comboio.destino.contains(busca, ignoreCase = true)
        }
        exibirResultados(filtrados)
    }

    private fun exibirResultados(lista: List<Comboio>) {
        val finalItems = mutableListOf<Any>()
        val estacaoLimpa = limparTexto(nomeEstacaoGlobal)

        // Agrupar por tipo e ordenar por hora de passagem
        lista.groupBy { it.tipo }.forEach { (tipo, comboios) ->
            finalItems.add(getTipoExtenso(tipo))
            finalItems.addAll(comboios.sortedBy { c ->
                c.paragens.find { limparTexto(it.estacao) == estacaoLimpa }?.hora
            })
        }
        adapter.atualizarLista(finalItems, nomeEstacaoGlobal)
    }

    private fun getTipoExtenso(tipo: String?): String = when(tipo) {
        "AP" -> "Alfa Pendular"
        "IC" -> "Intercidades"
        "R" -> "Regional"
        "U" -> "Urbano"
        else -> "Outros Serviços"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}