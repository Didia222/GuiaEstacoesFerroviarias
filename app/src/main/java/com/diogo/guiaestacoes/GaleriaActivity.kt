package com.diogo.guiaestacoes

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.Normalizer
import java.util.UUID

// --- 1. MODELOS DE DADOS (Estrutura Exata da Base de Dados) ---

data class Comentario(
    val conteudo: String = "",
    val data_hora: Long = 0L,
    val id_comentario: String = "",
    val id_estacao: String = "",
    val nome_autor: String = ""
)

data class FotoEstacao(
    val ano: Long = 2024L, // O "L" garante que o Firebase guarda como int64
    val caminho_ficheiro: String = "",
    val estacao_id: String = "",
    val id_foto: String = "",
    val legenda: String = ""
)

// --- 2. ACTIVITY PRINCIPAL ---

class GaleriaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var idEstacaoLimpo: String

    private val listaFotos = mutableListOf<String>()
    private lateinit var adapterFotos: FotoAdapter // Agora usa o ficheiro separado!

    private val listaComentarios = mutableListOf<Comentario>()
    private lateinit var adapterComentarios: ComentarioAdapter // Agora usa o ficheiro separado!

    // Lançador para escolher a imagem da galeria do telemóvel
    private val escolherImagemLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            fazerUploadDaImagem(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria)

        val nomeEstacao = intent.getStringExtra("NOME") ?: "Estação"
        idEstacaoLimpo = limparTexto(nomeEstacao)

        // Configurar UI da Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarGaleria)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Galeria de $nomeEstacao"
        toolbar.navigationIcon?.setTint(Color.BLACK)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        // Configurar Grelha de Fotos (RecyclerView)
        val rvFotos = findViewById<RecyclerView>(R.id.rvFotos)
        rvFotos.layoutManager = GridLayoutManager(this, 2)
        adapterFotos = FotoAdapter(listaFotos)
        rvFotos.adapter = adapterFotos

        // Configurar Lista de Comentários (RecyclerView)
        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.adapter = adapterComentarios

        // Botão Enviar Comentário
        val etNovoComentario = findViewById<EditText>(R.id.etNovoComentario)
        findViewById<ImageButton>(R.id.btnEnviarComentario).setOnClickListener {
            val texto = etNovoComentario.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarComentarioParaFirebase(texto)
                etNovoComentario.text.clear()
            }
        }

        // Botão Adicionar Foto (Abre a galeria)
        findViewById<FloatingActionButton>(R.id.fabAdicionarFoto).setOnClickListener {
            escolherImagemLauncher.launch("image/*")
        }

        // Iniciar escuta de dados em tempo real
        carregarFotosDoFirebase()
        carregarComentariosDoFirebase()
    }

    // --- FUNÇÕES DE FOTOS E STORAGE ---

    private fun fazerUploadDaImagem(uri: Uri) {
        Toast.makeText(this, "A fazer upload da foto...", Toast.LENGTH_SHORT).show()

        val nomeFicheiro = "${UUID.randomUUID()}.jpg"
        val referenciaStorage = storage.reference.child("fotos_estacoes/${idEstacaoLimpo}/${nomeFicheiro}")

        // 1. Envia a foto para o Firebase Storage
        referenciaStorage.putFile(uri)
            .addOnSuccessListener {
                // 2. Obtém o URL público da imagem
                referenciaStorage.downloadUrl.addOnSuccessListener { urlDownload ->
                    guardarFotoNoFirestore(urlDownload.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar a imagem.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarFotoNoFirestore(url: String) {
        // Gera o ID único da foto
        val novoIdFoto = db.collection("fotos_estacoes").document().id

        // Preenche com o modelo exato que pediste
        val novaFoto = FotoEstacao(
            ano = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toLong(),
            caminho_ficheiro = url,
            estacao_id = idEstacaoLimpo,
            id_foto = novoIdFoto,
            legenda = "Adicionada via aplicação"
        )

        db.collection("fotos_estacoes").document(novoIdFoto).set(novaFoto)
            .addOnSuccessListener {
                Toast.makeText(this, "Foto adicionada à galeria!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao guardar foto.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarFotosDoFirebase() {
        db.collection("fotos_estacoes")
            .whereEqualTo("estacao_id", idEstacaoLimpo)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                listaFotos.clear()
                for (doc in snapshots) {
                    val foto = doc.toObject(FotoEstacao::class.java)
                    if (foto.caminho_ficheiro.isNotEmpty()) {
                        listaFotos.add(foto.caminho_ficheiro)
                    }
                }

                // Imagem de placeholder se a estação ainda não tiver fotos
                if (listaFotos.isEmpty()) {
                    listaFotos.add("https://images.unsplash.com/photo-1541427468627-a89a96e5ca1d?w=500")
                }
                adapterFotos.notifyDataSetChanged()
            }
    }

    // --- FUNÇÕES DE COMENTÁRIOS ---

    private fun carregarComentariosDoFirebase() {
        db.collection("comentarios")
            .whereEqualTo("id_estacao", idEstacaoLimpo)
            .orderBy("data_hora", Query.Direction.DESCENDING)
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
        val novoIdComentario = db.collection("comentarios").document().id

        val novoComentario = Comentario(
            conteudo = texto,
            data_hora = System.currentTimeMillis(),
            id_comentario = novoIdComentario,
            id_estacao = idEstacaoLimpo,
            nome_autor = "Viajante" // Nome por defeito
        )

        db.collection("comentarios").document(novoIdComentario).set(novoComentario)
            .addOnSuccessListener {
                Toast.makeText(this, "Comentário enviado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar comentário.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- UTILITÁRIOS ---

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
}