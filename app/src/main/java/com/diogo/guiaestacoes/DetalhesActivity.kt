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

        // 1. Recuperar dados passados pela Intent
        nomeEstacao = intent.getStringExtra("NOME") ?: ""

        val historia = intent.getStringExtra("HISTORIA") ?: ""
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lng = intent.getDoubleExtra("LONGITUDE", 0.0)

        val tipoCompleto = intent.getStringExtra("TIPO") ?: ""

        val tipoLimpo = tipoCompleto.split(" • ")[0]

        idEstacaoLimpo = limparTexto(nomeEstacao)
        db = FirebaseFirestore.getInstance()

        // 2. Configurar Toolbar e Seta de Voltar
        val toolbar = findViewById<Toolbar>(R.id.toolbarDetalhes)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationOnClickListener {
            val intentVoltar = Intent(this, MainActivity::class.java).apply {
                putExtra("LAT_RETORNO", lat)
                putExtra("LNG_RETORNO", lng)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intentVoltar)
            finish()
        }

        findViewById<TextView>(R.id.tvTituloDetalhe).text = nomeEstacao
        findViewById<TextView>(R.id.tvTipoDetalhe).text = tipoLimpo // Usamos o tipo já cortado!
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        tvConteudo.text = historia



        // 4. Configurar Recycler da Cronologia
        val rvTimeline = findViewById<RecyclerView>(R.id.rvTimeline)
        adapterTimeline = TimelineAdapter(listaEventos)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvTimeline.adapter = adapterTimeline

        // 5. Configurar Recycler de Comentários
        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        rvComentarios.adapter = adapterComentarios

        // 6. Carregar Dados do Firestore
        carregarCronologia()
        ouvirComentarios()

        // 7. Botão GPS (Google Maps Navigation)
        findViewById<MaterialButton>(R.id.btnMapaDetalhe).setOnClickListener {
            val toggleModo = findViewById<MaterialButtonToggleGroup>(R.id.toggleModoTransporte)
            val modo = if (toggleModo.checkedButtonId == R.id.btnPe) "w" else "d"

            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng&mode=$modo")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
        }

        // 8. Expandir / Recolher História
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
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Estou a ver a estação $nomeEstacao na App Guia Ferroviário! 🚂")
            }
            startActivity(Intent.createChooser(shareIntent, "Partilhar através de"))
        }

        // 10. Botão Abrir Avaliação
        findViewById<Button>(R.id.btnAbrirAvaliacaoDetalhes).setOnClickListener {
            val intentAvaliacao = Intent(this, AvaliacaoActivity::class.java)
            intentAvaliacao.putExtra("NOME", nomeEstacao)
            startActivity(intentAvaliacao)
        }

        val cardFundo = findViewById<androidx.cardview.widget.CardView>(R.id.cardDetalhes)

        val tipoEstacao = intent.getStringExtra("TIPO") ?: ""
        when {
            tipoEstacao.contains("Estação", ignoreCase = true) -> {
                cardFundo.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.fundo_estacao
                    )
                )
            }

            tipoEstacao.contains("Apeadeiro", ignoreCase = true) -> {
                cardFundo.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.fundo_apeadeiro
                    )
                )
            }

            else -> {
                cardFundo.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.fundo_padrao
                    )
                )
            }

        }
    }

    private fun carregarCronologia() {
        db.collection("Estacao").document(idEstacaoLimpo).collection("cronologia")
            .orderBy("ano")
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    // Esconde apenas os elementos da timeline, mantendo o texto da história visível
                    findViewById<RecyclerView>(R.id.rvTimeline).visibility = View.GONE
                    findViewById<View>(R.id.linhaSeparadora).visibility = View.GONE
                } else {
                    findViewById<RecyclerView>(R.id.rvTimeline).visibility = View.VISIBLE
                    findViewById<View>(R.id.linhaSeparadora).visibility = View.VISIBLE

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

                // Feedback visual se não houver comentários
                val tvLabel = findViewById<TextView>(R.id.tvLabelComentarios)
                if (listaComentarios.isEmpty()) {
                    tvLabel.text = "💬  Ainda sem comentários"
                } else {
                    tvLabel.text = "💬  Comentários e Fotos"
                }
            }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ")
            .replace("\\s+".toRegex(), " ")
            .trim().uppercase()
    }
}