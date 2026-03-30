package com.diogo.guiaestacoes

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelo para os Comentários
data class Comentario(
    val texto: String = "",
    val estacaoId: String = "",
    val timestamp: Long = 0L
)

// Modelo para as Fotos
data class FotoEstacao(
    val url: String = "",
    val estacaoId: String = ""
)

class GaleriaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var idEstacaoLimpo: String

    private val listaFotos = mutableListOf<String>()
    private lateinit var adapterFotos: FotoAdapter

    private val listaComentarios = mutableListOf<Comentario>()
    private lateinit var adapterComentarios: ComentarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria)

        val nomeEstacao = intent.getStringExtra("NOME") ?: "Estação"
        idEstacaoLimpo = limparTexto(nomeEstacao)

        // Configurar UI
        val toolbar = findViewById<Toolbar>(R.id.toolbarGaleria)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Galeria de $nomeEstacao"

        // Ajuste para o Notch
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        // 1. Configurar Grelha de Fotos (RecyclerView Superior)
        val rvFotos = findViewById<RecyclerView>(R.id.rvFotos)
        rvFotos.layoutManager = GridLayoutManager(this, 2) // 2 colunas como no protótipo
        adapterFotos = FotoAdapter(listaFotos)
        rvFotos.adapter = adapterFotos

        // 2. Configurar Lista de Comentários (RecyclerView Inferior)
        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.adapter = adapterComentarios

        // 3. Lógica do Botão Enviar Comentário
        val etNovoComentario = findViewById<EditText>(R.id.etNovoComentario)
        findViewById<ImageButton>(R.id.btnEnviarComentario).setOnClickListener {
            val texto = etNovoComentario.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarComentarioParaFirebase(texto)
                etNovoComentario.text.clear()
            }
        }

        // 4. Lógica do Botão Adicionar Foto (FAB)
        findViewById<FloatingActionButton>(R.id.fabAdicionarFoto).setOnClickListener {
            Toast.makeText(this, "Funcionalidade de Upload em breve!", Toast.LENGTH_LONG).show()
        }

        // Iniciar escuta de dados em tempo real
        carregarFotosDoFirebase()
        carregarComentariosDoFirebase()
    }

    private fun carregarFotosDoFirebase() {
        db.collection("fotos_estacoes")
            .whereEqualTo("estacaoId", idEstacaoLimpo)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                listaFotos.clear()
                for (doc in snapshots) {
                    val foto = doc.toObject(FotoEstacao::class.java)
                    if (foto.url.isNotEmpty()) listaFotos.add(foto.url)
                }
                // Foto padrão se estiver vazio para não ficar feio
                if (listaFotos.isEmpty()) {
                    listaFotos.add("https://images.unsplash.com/photo-1541427468627-a89a96e5ca1d?w=500")
                }
                adapterFotos.notifyDataSetChanged()
            }
    }

    private fun carregarComentariosDoFirebase() {
        db.collection("comentarios")
            .whereEqualTo("estacaoId", idEstacaoLimpo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                listaComentarios.clear()
                for (doc in snapshots) {
                    listaComentarios.add(doc.toObject(Comentario::class.java))
                }
                adapterComentarios.notifyDataSetChanged()
            }
    }

    private fun enviarComentarioParaFirebase(texto: String) {
        val novo = Comentario(texto, idEstacaoLimpo, System.currentTimeMillis())
        db.collection("comentarios").add(novo)
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao comentar.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // --- ADAPTERS INTERNOS ---

    inner class FotoAdapter(private val urls: List<String>) : RecyclerView.Adapter<FotoAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) { val img: ImageView = v as ImageView }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ImageView(p.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450).apply { setMargins(8, 8, 8, 8) }
            scaleType = ImageView.ScaleType.CENTER_CROP
        })
        override fun onBindViewHolder(h: VH, p: Int) {
            Glide.with(h.itemView).load(urls[p]).transform(CenterCrop(), RoundedCorners(32)).into(h.img)
        }
        override fun getItemCount() = urls.size
    }

    inner class ComentarioAdapter(private val list: List<Comentario>) : RecyclerView.Adapter<ComentarioAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val txt: TextView = v.findViewById(android.R.id.text1)
            val sub: TextView = v.findViewById(android.R.id.text2)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_2, p, false))
        override fun onBindViewHolder(h: VH, p: Int) {
            h.txt.text = list[p].texto
            val data = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(list[p].timestamp))
            h.sub.text = data
        }
        override fun getItemCount() = list.size
    }
}