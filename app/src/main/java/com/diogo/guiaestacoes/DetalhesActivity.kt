package com.diogo.guiaestacoes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.Normalizer
import java.util.UUID

class DetalhesActivity : AppCompatActivity() {

    private var isExpanded = false
    private var uriImagemSelecionada: Uri? = null

    // Variáveis do Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Variáveis da Estação
    private var nomeEstacao: String = ""
    private var idEstacaoLimpo: String = "" // A NOVA VARIÁVEL CRÍTICA

    // Variáveis da Lista de Comentários
    private lateinit var adapter: ComentarioAdapter
    private val listaComentarios = mutableListOf<Comentario>()

    // Lançador para abrir a Galeria
    private val selecionarImagemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            uriImagemSelecionada = data?.data
            if (uriImagemSelecionada != null) {
                Toast.makeText(this, "Foto selecionada! 📷", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Receber dados da Intent e Limpar o ID
        nomeEstacao = intent.getStringExtra("NOME") ?: ""
        idEstacaoLimpo = limparTexto(nomeEstacao) // LIMPEZA FEITA AQUI

        val tipo = intent.getStringExtra("TIPO") ?: ""
        val historia = intent.getStringExtra("HISTORIA") ?: ""
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lng = intent.getDoubleExtra("LONGITUDE", 0.0)

        // Configurar UI Básica
        val tvTitulo = findViewById<TextView>(R.id.tvTituloDetalhe)
        val tvTipo = findViewById<TextView>(R.id.tvTipoDetalhe)
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        val btnExpandir = findViewById<ImageButton>(R.id.btnExpandirHistoria)
        val btnVerNoMapa = findViewById<MaterialButton>(R.id.btnMapaDetalhe)

        tvTitulo.text = nomeEstacao
        tvTipo.text = tipo
        tvConteudo.text = historia

        // Configurar a Lista (RecyclerView) de Comentários
        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        adapter = ComentarioAdapter(listaComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        rvComentarios.adapter = adapter

        // Começar a ouvir os comentários do Firebase
        ouvirComentarios()

        // --- CLIQUES BÁSICOS ---
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

        // --- CLIQUES COMENTÁRIOS E FOTOS ---
        val btnTirarFoto = findViewById<ImageButton>(R.id.btnTirarFoto)
        val btnEnviarComentario = findViewById<ImageButton>(R.id.btnEnviarComentario)
        val etNovoComentario = findViewById<EditText>(R.id.etNovoComentario)

        btnTirarFoto.setOnClickListener { abrirGaleria() }

        btnEnviarComentario.setOnClickListener {
            val textoComentario = etNovoComentario.text.toString().trim()
            if (textoComentario.isEmpty() && uriImagemSelecionada == null) {
                Toast.makeText(this, "Escreve algo ou escolhe uma foto!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEnviarComentario.isEnabled = false
            Toast.makeText(this, "A enviar... ⏳", Toast.LENGTH_SHORT).show()

            if (uriImagemSelecionada != null) {
                fazerUploadImagemEGuardarComentario(textoComentario, etNovoComentario, btnEnviarComentario)
            } else {
                guardarComentarioNoFirestore(textoComentario, "", etNovoComentario, btnEnviarComentario)
            }
        }
    }

    private fun ouvirComentarios() {
        // Usa idEstacaoLimpo!
        db.collection("comentarios")
            .whereEqualTo("id_estacao", idEstacaoLimpo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    listaComentarios.clear()
                    for (doc in snapshots) {
                        val comentario = doc.toObject(Comentario::class.java)
                        listaComentarios.add(comentario)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selecionarImagemLauncher.launch(intent)
    }

    private fun fazerUploadImagemEGuardarComentario(texto: String, inputField: EditText, btnEnviar: ImageButton) {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        // Usa idEstacaoLimpo na pasta!
        val refStorage = storage.reference.child("fotos_estacoes/$idEstacaoLimpo/$fileName")

        refStorage.putFile(uriImagemSelecionada!!)
            .addOnSuccessListener {
                refStorage.downloadUrl.addOnSuccessListener { uri ->
                    guardarComentarioNoFirestore(texto, uri.toString(), inputField, btnEnviar)
                }
            }
            .addOnFailureListener {
                btnEnviar.isEnabled = true
            }
    }

    private fun guardarComentarioNoFirestore(texto: String, urlFoto: String, inputField: EditText, btnEnviar: ImageButton) {
        val novoDocumentoRef = db.collection("comentarios").document()
        val idGerado = novoDocumentoRef.id

        val novoComentario = Comentario(
            id_comentario = idGerado,
            id_estacao = idEstacaoLimpo, // Usa idEstacaoLimpo!
            autor = "Viajante Anónimo",
            texto = texto,
            url_foto = urlFoto,
            timestamp = System.currentTimeMillis()
        )

        novoDocumentoRef.set(novoComentario)
            .addOnSuccessListener {
                inputField.text.clear()
                uriImagemSelecionada = null
                btnEnviar.isEnabled = true
                Toast.makeText(this, "Publicado! 🎉", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                btnEnviar.isEnabled = true
            }
    }

    // A FUNÇÃO MÁGICA DE LIMPAR TEXTO
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }
}