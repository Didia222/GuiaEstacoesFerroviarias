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
    //para evitar fazer multiplos pedidos ao servidor quando o utilizador
    //escreve na barra de pesquisa (RNF-2)
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
                // filtração de texto inserido na  barra de pesquisa como a presença de assentos
                //para detetar o nome da base de dados que se encontra sem sinais de pontuação
                filtrar(newText ?: "")
                return true
            }
        })
    }

    private fun carregarComboios(nome: String) {
        // Utilização de array-contains do Firestore no campo 'estacoes_servidas'.
        //Em vez de o programa percorrer todos os comboios presentes na base de dados,
        //ele devolve apenas os comnoiox que passam na estação selecionada do mapa.
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
            // Capacidade de o utilizador pesquisar um determinado comboio
            //por o seu numero de identificação
            if (c.numero.contains(busca)) return@filter true

            //1.Descobrimos a posição da estação atual no array de paragens
            val indexAtual = c.paragens.indexOfFirst { limparTexto(it.estacao) == estacaoAtualLimpa }
            //2. Isola-se as paregens futuras  usando o sublist
            //Isso corta os comboios que passaram pelas estações anteriores
            //á selecionada nao aparerem no resultado da pesquisa
            val paragensFuturas = if (indexAtual != -1) {
                c.paragens.subList(indexAtual + 1, c.paragens.size)
            } else {
                emptyList()
            }

            //3. Verificação que na pesquisa existe apenas estações que ainda váo acontecer
            //como destino do comboio
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
        "AP" -> "Alfa Pendular"; "IC" -> "Intercidades"; "R" -> "Regional"; "U" -> "Urbano"; else -> "Outros"
    }

    // Normalização vital: remove acentos e mete tudo em maiúsculas (ex: "SÃO bento" -> "SAO BENTO")
    // para evitar erros de case-sensitivity ou utilizadores que não põem acentos.
    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}