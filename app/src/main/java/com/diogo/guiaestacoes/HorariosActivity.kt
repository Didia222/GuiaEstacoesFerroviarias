package com.diogo.guiaestacoes

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private var textoPesquisado: String = ""

    // Vamos guardar os comboios originais aqui para não termos de pedir à net cada vez que escreves uma letra!
    private var todosOsComboiosDaEstacao = listOf<Comboio>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(android.graphics.Color.WHITE)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        nomeEstacaoGlobal = intent.getStringExtra("ESTACAO_NOME") ?: ""
        supportActionBar?.title = "Horários: $nomeEstacaoGlobal"

        recyclerView = findViewById(R.id.recyclerViewHorarios)
        svPesquisaHorarios = findViewById(R.id.svPesquisaHorarios)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        // 1. Primeiro vamos buscar TODOS os comboios que passam nesta estação
        if (nomeEstacaoGlobal.isNotEmpty()) {
            carregarComboiosIniciais(nomeEstacaoGlobal)
        }

        // 2. Configurar a barra de pesquisa para filtrar em tempo real
        svPesquisaHorarios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Quando o utilizador clica no "Enter" no teclado
                svPesquisaHorarios.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Quando o utilizador escreve ou apaga qualquer letra!
                val texto = newText?.trim() ?: ""
                textoPesquisado = texto
                filtrarListaNaMemoria(texto)
                return true
            }
        })
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }

    private fun carregarComboiosIniciais(nomeEstacao: String) {
        val nomeLimpoBusca = limparTexto(nomeEstacao)

        db.collection("Comboio").get().addOnSuccessListener { documents ->
            val listaFiltrada = mutableListOf<Comboio>()
            for (document in documents) {
                try {
                    val comboio = document.toObject(Comboio::class.java)
                    // Verifica se o comboio para nesta estação
                    val paraNestaEstacao = comboio.paragens.any {
                        limparTexto(it.estacao).contains(nomeLimpoBusca) || nomeLimpoBusca.contains(limparTexto(it.estacao))
                    }

                    if (paraNestaEstacao) {
                        listaFiltrada.add(comboio)
                    }
                } catch (e: Exception) {
                    Log.e("HorariosActivity", "Erro ao converter: ${e.message}")
                }
            }

            // Guardamos a lista completa para podermos filtrar super rápido
            todosOsComboiosDaEstacao = listaFiltrada
            exibirResultados(todosOsComboiosDaEstacao)

        }.addOnFailureListener { e ->
            Log.e("HorariosActivity", "Erro Firebase", e)
            Toast.makeText(this, "Erro ao carregar dados", Toast.LENGTH_SHORT).show()
        }
    }

    // Esta função é nova! É muito mais rápida porque pesquisa diretamente na lista que já sacámos do Firebase.
    private fun filtrarListaNaMemoria(texto: String) {
        if (texto.isEmpty()) {
            exibirResultados(todosOsComboiosDaEstacao)
            return
        }

        val textoLimpo = limparTexto(texto)
        val estacaoAtualLimpa = limparTexto(nomeEstacaoGlobal)
        val resultados = mutableListOf<Comboio>()

        for (comboio in todosOsComboiosDaEstacao) {
            // Em que número da lista de paragens está a nossa estação?
            val indexNossaEstacao = comboio.paragens.indexOfFirst {
                val nomeLimpo = limparTexto(it.estacao)
                nomeLimpo.contains(estacaoAtualLimpa) || estacaoAtualLimpa.contains(nomeLimpo)
            }

            if (indexNossaEstacao == -1) continue

            // Verifica se o que a pessoa escreveu é o NÚMERO do comboio
            val matchNumero = comboio.numero.contains(textoLimpo, true)

            // Verifica se o que a pessoa escreveu é o DESTINO (uma paragem que venha DEPOIS da nossa)
            val sentidoCorreto = comboio.paragens.withIndex().any { (index, paragem) ->
                val nomeParagemLimpo = limparTexto(paragem.estacao)
                val correspondePesquisa = nomeParagemLimpo.contains(textoLimpo) || textoLimpo.contains(nomeParagemLimpo)
                correspondePesquisa && index > indexNossaEstacao
            }

            if (matchNumero || sentidoCorreto) {
                resultados.add(comboio)
            }
        }

        exibirResultados(resultados)
    }

    private fun exibirResultados(lista: List<Comboio>) {
        val listaExibicao = mutableListOf<Any>()
        val grupos = lista.groupBy { it.tipo }
        val estacaoGlobalLimpa = limparTexto(nomeEstacaoGlobal)

        grupos.forEach { (tipo, comboios) ->
            listaExibicao.add(tipo ?: "Comboio")
            listaExibicao.addAll(comboios.sortedBy { c ->
                c.paragens.find {
                    val nomeParagem = limparTexto(it.estacao)
                    nomeParagem.contains(estacaoGlobalLimpa) || estacaoGlobalLimpa.contains(nomeParagem)
                }?.hora
            })
        }

        adapter.atualizarLista(listaExibicao, nomeEstacaoGlobal, textoPesquisado)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}