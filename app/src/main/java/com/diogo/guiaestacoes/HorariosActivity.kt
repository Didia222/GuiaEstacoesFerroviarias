package com.diogo.guiaestacoes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
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

    // Guardar a lista original com os dados dos horarios dos comboios
    // para evitar fazer multiplos pedidos ao servidor quando o utilizador
    // escreve na barra de pesquisa (RNF-2)
    private var todosOsComboiosDaEstacao = listOf<Comboio>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        // Configuração da Toolbar Verde com Seta Branca
        val toolbar = findViewById<Toolbar>(R.id.toolbarHorarios)
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
                // Filtração de texto inserido na barra de pesquisa
                filtrar(newText ?: "")
                return true
            }
        })
    }

    private fun carregarComboios(nome: String) {
        // Utilização de array-contains do Firestore no campo 'estacoes_servidas'.
        db.collection("Comboio")
            .whereArrayContains("estacoes_servidas", limparTexto(nome))
            .get()
            .addOnSuccessListener {
                todosOsComboiosDaEstacao = it.toObjects(Comboio::class.java)
                exibirResultados(todosOsComboiosDaEstacao)
            }
    }

    // Filtra a lista para mostrar apenas comboios que ainda vão passar no destino pesquisado
    private fun filtrar(texto: String) {
        val busca = limparTexto(texto)
        val estacaoAtualLimpa = limparTexto(nomeEstacaoGlobal)

        val filtrados = if (texto.isEmpty()) todosOsComboiosDaEstacao
        else todosOsComboiosDaEstacao.filter { c ->
            // Capacidade de o utilizador pesquisar um determinado comboio pelo número
            if (c.numero.contains(busca)) return@filter true

            // 1. Descobrimos a posição da estação atual no array de paragens
            val indexAtual = c.paragens.indexOfFirst { limparTexto(it.estacao) == estacaoAtualLimpa }

            // 2. Isola-se as paragens futuras usando o sublist
            val paragensFuturas = if (indexAtual != -1) {
                c.paragens.subList(indexAtual + 1, c.paragens.size)
            } else {
                emptyList()
            }

            // 3. Verificação que na pesquisa existe apenas estações que ainda vão acontecer
            paragensFuturas.any { limparTexto(it.estacao).contains(busca) }
        }

        exibirResultados(filtrados, texto)
    }

    private fun exibirResultados(lista: List<Comboio>, busca: String = "") {
        val finalItems = mutableListOf<Any>()
        val estacaoLimpa = limparTexto(nomeEstacaoGlobal)

        lista.groupBy { it.tipo }.forEach { (tipo, comboios) ->
            finalItems.add(getTipoExtenso(tipo))
            // Ordenação local pela hora em que o comboio passa pela estação
            finalItems.addAll(comboios.sortedBy { c ->
                c.paragens.find { limparTexto(it.estacao) == estacaoLimpa }?.hora
            })
        }
        adapter.atualizarLista(finalItems, nomeEstacaoGlobal, busca)
    }

    private fun getTipoExtenso(t: String?): String = when(t) {
        "AP" -> "Alfa Pendular"
        "IC" -> "Intercidades"
        "R" -> "Regional"
        "U" -> "Urbano"
        else -> "Outros"
    }

    // Normalização vital: remove acentos, mete tudo em maiúsculas e corrige inconsistências
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ")
            .replace("/", " ")
            .replace("'", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .uppercase()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}