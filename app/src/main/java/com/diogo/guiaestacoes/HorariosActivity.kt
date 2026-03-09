package com.diogo.guiaestacoes

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer // Importante para remover acentos

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

        // Ajuste para a câmara (notch) não tapar a barra
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
            if (texto.isNotEmpty()) pesquisarSmarter(texto)
            else pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        }
    }

    // Função Mágica que tira os acentos (ex: "Santarém" vira "Santarem")
    private fun removerAcentos(str: String): String {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
    }

    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        val estacaoProcuradaLimpa = removerAcentos(nomeEstacao)

        db.collection("comboios").get().addOnSuccessListener { documents ->
            val lista = mutableListOf<Comboio>()
            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)

                // Verifica se a estação (sem acentos) existe na lista do Firebase (sem acentos)
                val passaNestaEstacao = comboio.paragens.any { paragem ->
                    removerAcentos(paragem.estacao).contains(estacaoProcuradaLimpa, ignoreCase = true)
                }

                if (passaNestaEstacao) {
                    lista.add(comboio)
                }
            }
            if (lista.isEmpty()) Toast.makeText(this, "Sem comboios para esta estação", Toast.LENGTH_SHORT).show()
            exibirResultados(lista)
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao ligar ao Firebase: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun pesquisarSmarter(texto: String) {
        val textoLimpo = removerAcentos(texto)

        db.collection("comboios").get().addOnSuccessListener { documents ->
            val resultados = mutableListOf<Comboio>()
            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)

                val matchNumero = comboio.numero.contains(textoLimpo, true)
                val matchDestino = removerAcentos(comboio.destino).contains(textoLimpo, true)
                val matchParagem = comboio.paragens.any { removerAcentos(it.estacao).contains(textoLimpo, true) }

                if (matchNumero || matchDestino || matchParagem) {
                    resultados.add(comboio)
                }
            }
            exibirResultados(resultados)
        }
    }

    private fun exibirResultados(lista: List<Comboio>) {
        val listaExibicao = mutableListOf<Any>()
        val grupos = lista.groupBy { it.tipo }
        val estacaoGlobalLimpa = removerAcentos(nomeEstacaoGlobal)

        grupos.forEach { (tipo, comboios) ->
            listaExibicao.add(tipo ?: "Comboio")
            listaExibicao.addAll(comboios.sortedBy { c ->
                c.paragens.find { removerAcentos(it.estacao).contains(estacaoGlobalLimpa, true) }?.hora
            })
        }
        adapter.atualizarLista(listaExibicao, nomeEstacaoGlobal)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}