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
import java.text.Normalizer

class HorariosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ComboioAdapter
    private lateinit var etNumeroComboio: EditText
    private lateinit var btnPesquisar: Button
    private var nomeEstacaoGlobal: String = ""
    private var textoPesquisado: String = "" // AQUI: Variável para guardar o destino pesquisado!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHorarios)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(android.graphics.Color.WHITE)

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
            textoPesquisado = texto // AQUI: Guarda o destino sempre que o utilizador clica em pesquisar
            if (texto.isNotEmpty()) pesquisarSmarter(texto)
            else pesquisarComboiosDaEstacao(nomeEstacaoGlobal)
        }
    }

    // FUNÇÃO MELHORADA: Remove acentos, hífens e espaços extra (ex: "Porto - São Bento" vira "Porto Sao Bento")
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.replace("-", " ").replace("\\s+".toRegex(), " ").trim()
    }

    // Vai ao firebase recolher informação apenas dos comboios que fazem paragem na estação selecionada
    private fun pesquisarComboiosDaEstacao(nomeEstacao: String) {
        val estacaoProcuradaLimpa = limparTexto(nomeEstacao)
        textoPesquisado = "" // AQUI: Se não há pesquisa, limpa a variável de destino

        db.collection("comboios").get().addOnSuccessListener { documents ->
            val lista = mutableListOf<Comboio>()
            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)

                // Verifica cruzada: o mapa tem o nome do Firebase OU o Firebase tem o nome do mapa
                val passaNestaEstacao = comboio.paragens.any { paragem ->
                    val nomeParagemLimpo = limparTexto(paragem.estacao)
                    nomeParagemLimpo.contains(estacaoProcuradaLimpa, ignoreCase = true) ||
                            estacaoProcuradaLimpa.contains(nomeParagemLimpo, ignoreCase = true)
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

    // Verificador flexivel que identifica os comboios da paragem nao só pelo nome mas sim pelo seu numero, destino ou se é de umas paragens pelo meio
    private fun pesquisarSmarter(texto: String) {
        val textoLimpo = limparTexto(texto)
        val estacaoAtualLimpa = limparTexto(nomeEstacaoGlobal)

        db.collection("comboios").get().addOnSuccessListener { documents ->
            val resultados = mutableListOf<Comboio>()

            for (document in documents) {
                val comboio = document.toObject(Comboio::class.java)

                // 1. Descobrir a POSIÇÃO (índice) em que o comboio passa na nossa estação (ex: Penafiel)
                val indexNossaEstacao = comboio.paragens.indexOfFirst {
                    val nomeLimpo = limparTexto(it.estacao)
                    nomeLimpo.contains(estacaoAtualLimpa, true) || estacaoAtualLimpa.contains(nomeLimpo, true)
                }

                // Se não passa na nossa estação (devolve -1), ignoramos logo este comboio
                if (indexNossaEstacao == -1) continue

                // 2. Verifica se o texto pesquisado é o NÚMERO do comboio
                val matchNumero = comboio.numero.contains(textoLimpo, true)

                // 3. A NOVA REGRA DE OURO (O Sentido da Viagem):
                // Procura se a estação pesquisada (ex: Porto) existe e se fica DEPOIS da nossa estação
                val sentidoCorreto = comboio.paragens.withIndex().any { (index, paragem) ->
                    val nomeParagemLimpo = limparTexto(paragem.estacao)
                    val correspondePesquisa = nomeParagemLimpo.contains(textoLimpo, true) || textoLimpo.contains(nomeParagemLimpo, true)

                    // Só é válido se corresponder ao texto pesquisado E a paragem for mais à frente (index > indexNossaEstacao)
                    correspondePesquisa && index > indexNossaEstacao
                }

                // Se o utilizador pesquisou pelo número ou se a direção está correta, mostramos o comboio!
                if (matchNumero || sentidoCorreto) {
                    resultados.add(comboio)
                }
            }
            exibirResultados(resultados)
        }
    }

    //O "Arrumador": Pega nos comboios desorganizados que vêm do Firebase, agrupa-os por categoria (ex: Urbanos, AP) e ordena-os pela hora mais cedo.
    private fun exibirResultados(lista: List<Comboio>) {
        val listaExibicao = mutableListOf<Any>()
        val grupos = lista.groupBy { it.tipo }
        val estacaoGlobalLimpa = limparTexto(nomeEstacaoGlobal)

        grupos.forEach { (tipo, comboios) ->
            listaExibicao.add(tipo ?: "Comboio")
            listaExibicao.addAll(comboios.sortedBy { c ->
                c.paragens.find {
                    val nomeParagem = limparTexto(it.estacao)
                    nomeParagem.contains(estacaoGlobalLimpa, true) || estacaoGlobalLimpa.contains(nomeParagem, true)
                }?.hora
            })
        }

        // AQUI ESTAVA O SEGREDO: Envias agora o textoPesquisado para o adaptador!
        adapter.atualizarLista(listaExibicao, nomeEstacaoGlobal, textoPesquisado)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}