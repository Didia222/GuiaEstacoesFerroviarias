package com.diogo.guiaestacoes

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var etNumeroComboio: EditText
    private lateinit var btnPesquisar: Button
    private var nomeEstacaoGlobal: String = ""
    private var textoPesquisado: String = ""

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
        etNumeroComboio = findViewById(R.id.etNumeroComboio)
        btnPesquisar = findViewById(R.id.btnPesquisar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        if (nomeEstacaoGlobal.isNotEmpty()) {
            pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        }

        btnPesquisar.setOnClickListener {
            val texto = etNumeroComboio.text.toString().trim()
            textoPesquisado = texto
            if (texto.isNotEmpty()) pesquisarSmarter(texto)
            else pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }

    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        val nomeLimpoBusca = limparTexto(nomeEstacao)

        db.collection("Comboio").get().addOnSuccessListener { documents ->
            val listaFiltrada = mutableListOf<Comboio>()
            for (document in documents) {
                try {
                    val comboio = document.toObject(Comboio::class.java)
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
            exibirResultados(listaFiltrada)
        }.addOnFailureListener { e ->
            Log.e("HorariosActivity", "Erro Firebase", e)
            Toast.makeText(this, "Erro ao carregar dados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pesquisarSmarter(texto: String) {
        val textoLimpo = limparTexto(texto)
        val estacaoAtualLimpa = limparTexto(nomeEstacaoGlobal)

        db.collection("Comboio").get().addOnSuccessListener { documents ->
            val resultados = mutableListOf<Comboio>()

            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)
                val indexNossaEstacao = comboio.paragens.indexOfFirst {
                    val nomeLimpo = limparTexto(it.estacao)
                    nomeLimpo.contains(estacaoAtualLimpa) || estacaoAtualLimpa.contains(nomeLimpo)
                }

                if (indexNossaEstacao == -1) continue

                val matchNumero = comboio.numero.contains(textoLimpo, true)
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
        
        if (lista.isEmpty()) {
            Toast.makeText(this, "Nenhum comboio encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
