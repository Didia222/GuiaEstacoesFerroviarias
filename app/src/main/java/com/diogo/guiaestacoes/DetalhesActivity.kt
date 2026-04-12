package com.diogo.guiaestacoes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class DetalhesActivity : AppCompatActivity() {

    private var isExpanded = false
    private lateinit var db: FirebaseFirestore

    private var nomeEstacao: String = ""
    private var idEstacaoLimpo: String = ""

    private lateinit var adapterComentarios: ComentarioAdapter
    private val listaComentarios = mutableListOf<Comentario>()

    private lateinit var adapterTimeline: TimelineAdapter
    private val listaEventos = mutableListOf<EventoHistorico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // 1. Recuperar dados da Intent (Mapa -> Detalhes)
        nomeEstacao = intent.getStringExtra("NOME") ?: ""
        val tipo = intent.getStringExtra("TIPO") ?: ""
        val historia = intent.getStringExtra("HISTORIA") ?: ""
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lng = intent.getDoubleExtra("LONGITUDE", 0.0)

        idEstacaoLimpo = limparTexto(nomeEstacao)
        db = FirebaseFirestore.getInstance()

        // 2. Configurar Toolbar (Seta de Voltar)
        val toolbar = findViewById<Toolbar>(R.id.toolbarDetalhes)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationOnClickListener {
            // Volta para o mapa focando na estação de onde viemos
            val intentVoltar = Intent(this, MainActivity::class.java).apply {
                putExtra("LAT_RETORNO", lat)
                putExtra("LNG_RETORNO", lng)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intentVoltar)
            finish()
        }

        // 3. Preencher UI Básica
        val tvTitulo = findViewById<TextView>(R.id.tvTituloDetalhe)
        val tvTipo = findViewById<TextView>(R.id.tvTipoDetalhe)
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)

        tvTitulo.text = nomeEstacao
        tvTipo.text = tipo
        tvConteudo.text = historia

        // 4. Configurar RecyclerView da Timeline (Linha Cronológica)
        val rvTimeline = findViewById<RecyclerView>(R.id.rvTimeline)
        adapterTimeline = TimelineAdapter(listaEventos)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvTimeline.adapter = adapterTimeline

        // 5. Configurar RecyclerView de Comentários
        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        rvComentarios.adapter = adapterComentarios

        // 6. Carregar Dados do Firebase
        carregarCronologia()
        ouvirComentarios()

        // 7. Configurar Botão Ver no Mapa (Navegação GPS)
        findViewById<MaterialButton>(R.id.btnMapaDetalhe).setOnClickListener {
            val toggleModo = findViewById<MaterialButtonToggleGroup>(R.id.toggleModoTransporte)
            // Se o botão "A pé" estiver selecionado usa modo "w" (walking), senão "d" (driving)
            val modo = if (toggleModo.checkedButtonId == R.id.btnPe) "w" else "d"

            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng&mode=$modo")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
        }

        // 8. Botão Expandir História
        findViewById<ImageButton>(R.id.btnExpandirHistoria).setOnClickListener {
            if (isExpanded) {
                tvConteudo.maxLines = 4
                (it as ImageButton).setImageResource(R.drawable.ic_expand_more)
            } else {
                tvConteudo.maxLines = Integer.MAX_VALUE
                (it as ImageButton).setImageResource(R.drawable.ic_expand_less)
            }
            isExpanded = !isExpanded
        }

        // 9. Botão Partilhar
        findViewById<MaterialButton>(R.id.btnPartilharDetalhe).setOnClickListener {
            val textoPartilha = "Estou a ver a estação $nomeEstacao no Guia de Estações Ferroviárias! 🚂"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textoPartilha)
            }
            startActivity(Intent.createChooser(shareIntent, "Partilhar via"))
        }

        // 10. Botão Abrir Avaliação
        findViewById<Button>(R.id.btnAbrirAvaliacaoDetalhes).setOnClickListener {
            startActivity(Intent(this, AvaliacaoActivity::class.java).apply {
                putExtra("NOME", nomeEstacao)
            })
        }
    }

    private fun carregarCronologia() {
        db.collection("Estacao").document(idEstacaoLimpo).collection("cronologia")
            .orderBy("ano")
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    // Esconde a secção se não houver dados históricos
                    findViewById<LinearLayout>(R.id.llHistoriaLabel).visibility = View.GONE
                    findViewById<TextView>(R.id.tvConteudoDetalhe).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rvTimeline).visibility = View.GONE
                    findViewById<View>(R.id.linhaSeparadora).visibility = View.GONE
                } else {
                    listaEventos.clear()
                    for (doc in snapshots) {
                        listaEventos.add(doc.toObject(EventoHistorico::class.java))
                    }
                    adapterTimeline.notifyDataSetChanged()
                }
            }
    }

    private fun ouvirComentarios() {
        db.collection("comentarios")
            .whereEqualTo("id_estacao", idEstacaoLimpo)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                listaComentarios.clear()
                for (doc in snapshots) {
                    listaComentarios.add(doc.toObject(Comentario::class.java))
                }
                listaComentarios.sortByDescending { it.timestamp }
                adapterComentarios.notifyDataSetChanged()

                // Se não houver comentários, podemos opcionalmente mudar o texto do label
                if (listaComentarios.isEmpty()) {
                    findViewById<TextView>(R.id.tvLabelComentarios).text = "💬  Ainda sem comentários"
                } else {
                    findViewById<TextView>(R.id.tvLabelComentarios).text = "💬  Comentários e Fotos"
                }
            }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }
}