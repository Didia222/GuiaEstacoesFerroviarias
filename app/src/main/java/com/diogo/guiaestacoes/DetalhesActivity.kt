package com.diogo.guiaestacoes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    // NOVA: Lista para a Timeline
    private lateinit var adapterTimeline: TimelineAdapter
    private val listaEventos = mutableListOf<EventoHistorico>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // 1. Dados da Intent
        nomeEstacao = intent.getStringExtra("NOME") ?: ""
        val tipo = intent.getStringExtra("TIPO") ?: ""
        val historia = intent.getStringExtra("HISTORIA") ?: ""
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lng = intent.getDoubleExtra("LONGITUDE", 0.0)

        idEstacaoLimpo = limparTexto(nomeEstacao)
        db = FirebaseFirestore.getInstance()

        // 2. Toolbar / Seta Voltar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarDetalhes)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.navigationIcon?.setTint(android.graphics.Color.WHITE)

        toolbar.setNavigationOnClickListener {
            val intentVoltar = Intent(this, MainActivity::class.java).apply {
                putExtra("LAT_RETORNO", lat)
                putExtra("LNG_RETORNO", lng)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intentVoltar)
            finish()
        }

        // 3. UI Principal
        findViewById<TextView>(R.id.tvTituloDetalhe).text = nomeEstacao
        findViewById<TextView>(R.id.tvTipoDetalhe).text = tipo
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        tvConteudo.text = historia

        // 4. CONFIGURAR TIMELINE (LINHA CRONOLÓGICA)
        val rvTimeline = findViewById<RecyclerView>(R.id.rvTimeline)
        adapterTimeline = TimelineAdapter(listaEventos)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvTimeline.adapter = adapterTimeline

        carregarCronologia() // Função para buscar os marcos históricos

        // 5. Configurar Comentários
        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        rvComentarios.adapter = adapterComentarios

        ouvirComentarios()

        // 6. Botões (Mapa, Expandir, Avaliar)
        findViewById<MaterialButton>(R.id.btnMapaDetalhe).setOnClickListener {
            val toggleModo = findViewById<MaterialButtonToggleGroup>(R.id.toggleModoTransporte)
            val modo = if (toggleModo.checkedButtonId == R.id.btnPe) "w" else "d"
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng&mode=$modo")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) startActivity(mapIntent)
            else toolbar.performClick()
        }

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

        findViewById<Button>(R.id.btnAbrirAvaliacaoDetalhes).setOnClickListener {
            startActivity(Intent(this, AvaliacaoActivity::class.java).apply { putExtra("NOME", nomeEstacao) })
        }
    }

    private fun carregarCronologia() {
        db.collection("Estacao").document(idEstacaoLimpo).collection("cronologia")
            .orderBy("ano")
            .get()
            .addOnSuccessListener { snapshots ->
                // Se não houver história, escondemos a secção inteira
                if (snapshots.isEmpty) {
                    findViewById<android.widget.LinearLayout>(R.id.llHistoriaLabel).visibility = android.view.View.GONE
                    findViewById<TextView>(R.id.tvConteudoDetalhe).visibility = android.view.View.GONE
                    findViewById<RecyclerView>(R.id.rvTimeline).visibility = android.view.View.GONE
                    findViewById<android.view.View>(R.id.linhaSeparadora).visibility = android.view.View.GONE
                } else {
                    listaEventos.clear()
                    for (doc in snapshots) {
                        val evento = doc.toObject(EventoHistorico::class.java)
                        listaEventos.add(evento)
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
                for (doc in snapshots) listaComentarios.add(doc.toObject(Comentario::class.java))
                listaComentarios.sortByDescending { it.timestamp }
                adapterComentarios.notifyDataSetChanged()
            }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }
}