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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. O Modelo de Dados do Comentário
data class Comentario(
    val texto: String = "",
    val estacaoId: String = "",
    val timestamp: Long = 0L
)

class GaleriaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvComentarios: RecyclerView
    private lateinit var adapterComentarios: ComentarioAdapter
    private val listaComentarios = mutableListOf<Comentario>()
    private lateinit var idEstacaoLimpo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria)

        val nomeEstacao = intent.getStringExtra("NOME") ?: "Estação"
        idEstacaoLimpo = limparTexto(nomeEstacao)

        // Configurar a Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarGaleria)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Galeria de $nomeEstacao"
        toolbar.setTitleTextColor(Color.BLACK)
        toolbar.navigationIcon?.setTint(Color.BLACK)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // --- GRELHA DE FOTOS ---
        val rvFotos = findViewById<RecyclerView>(R.id.rvFotos)
        rvFotos.layoutManager = GridLayoutManager(this, 2) // 2 Colunas como no teu protótipo!

        // Imagens de Alta Qualidade (Mockup para a apresentação)
        val imagensExemplo = listOf(
            "https://images.unsplash.com/photo-1541427468627-a89a96e5ca1d?w=500",
            "https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=500",
            "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=500",
            "https://images.unsplash.com/photo-1532105956626-9569c03602f6?w=500"
        )
        rvFotos.adapter = FotoAdapter(imagensExemplo)

        // --- LISTA DE COMENTÁRIOS ---
        rvComentarios = findViewById(R.id.rvComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.adapter = adapterComentarios

        // --- ENVIAR COMENTÁRIO ---
        val etNovoComentario = findViewById<EditText>(R.id.etNovoComentario)
        val btnEnviar = findViewById<ImageButton>(R.id.btnEnviarComentario)

        btnEnviar.setOnClickListener {
            val texto = etNovoComentario.text.toString().trim()
            if (texto.isNotEmpty()) {
                salvarComentarioNoFirebase(texto)
                etNovoComentario.text.clear() // Limpa a caixa
            }
        }

        // Carregar comentários em tempo real!
        carregarComentariosDoFirebase()
    }

    private fun salvarComentarioNoFirebase(texto: String) {
        val novoComentario = Comentario(texto, idEstacaoLimpo, System.currentTimeMillis())
        db.collection("comentarios").add(novoComentario)
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar comentário.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarComentariosDoFirebase() {
        db.collection("comentarios")
            .whereEqualTo("estacaoId", idEstacaoLimpo)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                listaComentarios.clear()
                for (doc in snapshots) {
                    val com = doc.toObject(Comentario::class.java)
                    listaComentarios.add(com)
                }
                adapterComentarios.notifyDataSetChanged()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }

    // =========================================================
    // ADAPTERS (Classes internas para desenhar as listas)
    // =========================================================

    // 1. ADAPTER DAS FOTOS
    inner class FotoAdapter(private val fotosUrls: List<String>) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>() {
        inner class FotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imagem: ImageView = view.findViewById(android.R.id.icon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
            // Criamos a ImageView via código para ser mais rápido (sem precisar de outro XML)
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400).apply { setMargins(8, 8, 8, 8) }
                id = android.R.id.icon
            }
            return FotoViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
            // O Glide carrega a imagem do URL da internet, arredonda os cantos e coloca na ImageView!
            Glide.with(holder.itemView.context)
                .load(fotosUrls[position])
                .transform(CenterCrop(), RoundedCorners(24))
                .into(holder.imagem)
        }
        override fun getItemCount() = fotosUrls.size
    }

    // 2. ADAPTER DOS COMENTÁRIOS
    inner class ComentarioAdapter(private val comentarios: List<Comentario>) : RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {
        inner class ComentarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTexto: TextView = view.findViewById(android.R.id.text1)
            val tvData: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ComentarioViewHolder(view)
        }

        override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
            val comentario = comentarios[position]
            holder.tvTexto.text = comentario.texto
            holder.tvTexto.setTextColor(Color.DKGRAY)
            holder.tvTexto.textSize = 16f

            val dataFormatada = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("pt", "PT")).format(Date(comentario.timestamp))
            holder.tvData.text = dataFormatada
            holder.tvData.setTextColor(Color.GRAY)
        }
        override fun getItemCount() = comentarios.size
    }
}