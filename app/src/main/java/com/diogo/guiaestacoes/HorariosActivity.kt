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

        // Inicializa o adaptador com a lista vazia e o nome da estação
        adapter = ComboioAdapter(emptyList(), nomeEstacaoGlobal)
        recyclerView.adapter = adapter

        if (nomeEstacaoGlobal.isNotEmpty()) {
            etNumeroComboio.hint = "Procurar em: $nomeEstacaoGlobal"
            pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        }

        btnPesquisar.setOnClickListener {
            val textoPesquisa = etNumeroComboio.text.toString().trim()
            if (textoPesquisa.isNotEmpty()) {
                // Nova função de pesquisa inteligente
                executarPesquisaSmarter(textoPesquisa)
            } else {
                // Se estiver vazio, volta a mostrar todos os comboios da estação
                pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    /**
     * Procura todos os comboios que param na estação selecionada.
     */
    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        db.collection("comboios")
            .get()
            .addOnSuccessListener { documents ->
                val listaOriginal = mutableListOf<Comboio>()

                for (document in documents) {
                    val comboio = document.toObject(Comboio::class.java)
                    // Verifica se o comboio passa nesta estação
                    val passaNestaEstacao = comboio.paragens.any { paragem ->
                        paragem.estacao.contains(nomeEstacao, ignoreCase = true)
                    }
                    if (passaNestaEstacao) {
                        listaOriginal.add(comboio)
                    }
                }

                if (listaOriginal.isNotEmpty()) {
                    processarEExibirLista(listaOriginal)
                } else {
                    Toast.makeText(this, "Sem comboios para $nomeEstacao", Toast.LENGTH_LONG).show()
                    adapter.atualizarLista(emptyList(), nomeEstacao)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro de ligação ao Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Pesquisa avançada que filtra por Número ou paragens FUTURAS,
     * garantindo que o comboio serve para o destino pretendido a partir de onde o utilizador está.
     */
    private fun executarPesquisaSmarter(texto: String) {
        db.collection("comboios")
            .get()
            .addOnSuccessListener { documents ->
                val resultados = mutableListOf<Comboio>()

                for (document in documents) {
                    val comboio = document.toObject(Comboio::class.java)

                    // 1. Encontrar a posição da estação atual na rota do comboio
                    val indexAtual = comboio.paragens.indexOfFirst {
                        it.estacao.contains(nomeEstacaoGlobal, ignoreCase = true)
                    }

                    // Se o comboio passa na estação selecionada...
                    if (indexAtual != -1) {
                        // 2. Verifica se o texto coincide com o Número
                        val matchesNumero = comboio.numero.contains(texto)

                        // 3. Verifica se o texto coincide com o Destino Final
                        val matchesDestinoFinal = comboio.destino.contains(texto, ignoreCase = true)

                        // 4. A LÓGICA DE DESTINO INTERMÉDIO: Verifica paragens APÓS a estação atual
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
                    Toast.makeText(this, "Nenhum comboio para '$texto' nesta linha", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Agrupa os comboios por tipo e ordena-os pela hora local.
     */
    private fun processarEExibirLista(listaComboios: List<Comboio>) {
        val listaMista = mutableListOf<Any>()
        val grupos = listaComboios.groupBy { it.tipo }

        grupos.forEach { (tipo, comboios) ->
            listaMista.add(tipo ?: "Outros")

            // Ordenação cronológica baseada na hora da paragem na estação local
            val ordenados = comboios.sortedBy { c ->
                c.paragens.find { it.estacao.contains(nomeEstacaoGlobal, true) }?.hora
            }
            listaMista.addAll(ordenados)
        }
        adapter.atualizarLista(listaMista, nomeEstacaoGlobal)
    }
}