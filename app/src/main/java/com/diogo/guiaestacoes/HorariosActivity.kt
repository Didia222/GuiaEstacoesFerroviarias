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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nomeEstacaoGlobal = intent.getStringExtra("ESTACAO_NOME") ?: ""
        supportActionBar?.title = "Horários: $nomeEstacaoGlobal"

        recyclerView = findViewById(R.id.recyclerViewHorarios)
        svPesquisaHorarios = findViewById(R.id.svPesquisaHorarios)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        if (nomeEstacaoGlobal.isNotEmpty()) carregarComboios(nomeEstacaoGlobal)

        svPesquisaHorarios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText ?: "")
                return true
            }
        })
    }

    private fun carregarComboios(nome: String) {
        db.collection("Comboio")
            .whereArrayContains("estacoes_servidas", limparTexto(nome))
            .get()
            .addOnSuccessListener {
                todosOsComboiosDaEstacao = it.toObjects(Comboio::class.java)
                exibirResultados(todosOsComboiosDaEstacao)
            }
    }

    private fun filtrar(texto: String) {
        val busca = limparTexto(texto)
        val filtrados = if (texto.isEmpty()) todosOsComboiosDaEstacao
        else todosOsComboiosDaEstacao.filter { c ->
            c.numero.contains(busca) || c.estacoes_servidas.any { it.contains(busca) }
        }
        exibirResultados(filtrados, texto)
    }

    private fun exibirResultados(lista: List<Comboio>, busca: String = "") {
        val finalItems = mutableListOf<Any>()
        val estacaoLimpa = limparTexto(nomeEstacaoGlobal)

        lista.groupBy { it.tipo }.forEach { (tipo, comboios) ->
            finalItems.add(getTipoExtenso(tipo))
            finalItems.addAll(comboios.sortedBy { c ->
                c.paragens.find { limparTexto(it.estacao) == estacaoLimpa }?.hora
            })
        }
        adapter.atualizarLista(finalItems, nomeEstacaoGlobal, busca)
    }

    private fun getTipoExtenso(t: String?): String = when(t) {
        "AP" -> "Alfa Pendular"; "IC" -> "Intercidades"; "R" -> "Regional"; "U" -> "Urbano"; else -> "Outros"
    }

    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}