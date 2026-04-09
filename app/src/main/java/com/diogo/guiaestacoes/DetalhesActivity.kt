package com.diogo.guiaestacoes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.Normalizer

class DetalhesActivity : AppCompatActivity() {

    private var isExpanded = false
    private lateinit var db: FirebaseFirestore

    private var nomeEstacao: String = ""
    private var idEstacaoLimpo: String = ""

    private lateinit var adapter: ComentarioAdapter
    private val listaComentarios = mutableListOf<Comentario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        db = FirebaseFirestore.getInstance()

        nomeEstacao = intent.getStringExtra("NOME") ?: ""
        idEstacaoLimpo = limparTexto(nomeEstacao)

        val tipo = intent.getStringExtra("TIPO") ?: ""
        val historia = intent.getStringExtra("HISTORIA") ?: ""
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lng = intent.getDoubleExtra("LONGITUDE", 0.0)

        val tvTitulo = findViewById<TextView>(R.id.tvTituloDetalhe)
        val tvTipo = findViewById<TextView>(R.id.tvTipoDetalhe)
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        val btnExpandir = findViewById<ImageButton>(R.id.btnExpandirHistoria)
        val btnVerNoMapa = findViewById<MaterialButton>(R.id.btnMapaDetalhe)

        tvTitulo.text = nomeEstacao
        tvTipo.text = tipo
        tvConteudo.text = historia

        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        adapter = ComentarioAdapter(listaComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        rvComentarios.adapter = adapter

        ouvirComentarios()

        btnVerNoMapa.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("LAT_RETORNO", lat)
                putExtra("LNG_RETORNO", lng)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }

        btnExpandir.setOnClickListener {
            if (isExpanded) {
                tvConteudo.maxLines = 4
                btnExpandir.setImageResource(R.drawable.ic_expand_more)
            } else {
                tvConteudo.maxLines = Integer.MAX_VALUE
                btnExpandir.setImageResource(R.drawable.ic_expand_less)
            }
            isExpanded = !isExpanded
        }

        // O NOVO BOTÃO DE AVALIAR (Que não choca com os layouts antigos)
        val btnAvaliar = findViewById<Button>(R.id.btnAbrirAvaliacaoDetalhes)
        btnAvaliar.setOnClickListener {
            val intent = Intent(this, AvaliacaoActivity::class.java).apply {
                putExtra("NOME", nomeEstacao)
            }
            startActivity(intent)
        }
    }

    private fun ouvirComentarios() {
        db.collection("comentarios")
            .whereEqualTo("id_estacao", idEstacaoLimpo)
            // APAGÁMOS A LINHA DO ORDERBY AQUI!
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    listaComentarios.clear()
                    for (doc in snapshots) {
                        val comentario = doc.toObject(Comentario::class.java)
                        listaComentarios.add(comentario)
                    }
                    // A MAGIA ACONTECE AQUI: Ordenamos no telemóvel em vez de ser no Firebase
                    listaComentarios.sortByDescending { it.timestamp }

                    adapter.notifyDataSetChanged()
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