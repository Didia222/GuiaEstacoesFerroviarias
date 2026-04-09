package com.diogo.guiaestacoes

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.Normalizer
import java.util.UUID

class AvaliacaoActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var idEstacaoLimpo: String

    private var uriImagemSelecionada: Uri? = null
    private lateinit var ivPreviewFoto: ImageView

    private val escolherImagemLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uriImagemSelecionada = uri
            ivPreviewFoto.setImageURI(uri)
            ivPreviewFoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avaliacao)

        val nomeEstacao = intent.getStringExtra("NOME") ?: "Estação Desconhecida"
        idEstacaoLimpo = limparTexto(nomeEstacao)

        // Configuração da Toolbar e da SETA de voltar atrás
        val toolbar = findViewById<Toolbar>(R.id.toolbarAvaliacao)
        setSupportActionBar(toolbar)

        // Ativa a seta visualmente
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Diz à seta para fechar esta janela e voltar para a anterior
        toolbar.setNavigationOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        val tvTitulo = findViewById<TextView>(R.id.tvTituloAvaliacao)
        tvTitulo.text = "Avaliar $nomeEstacao"

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val etNome = findViewById<EditText>(R.id.etNomeAvaliador)
        val etComentario = findViewById<EditText>(R.id.etComentarioAvaliacao)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAvaliacao)

        val btnAdicionarFoto = findViewById<ImageButton>(R.id.btnAdicionarFotoAvaliacao)
        ivPreviewFoto = findViewById(R.id.ivPreviewFoto)

        btnAdicionarFoto.setOnClickListener {
            escolherImagemLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            val estrelas = ratingBar.rating
            var nome = etNome.text.toString().trim()
            val textoComentario = etComentario.text.toString().trim()

            if (estrelas == 0f) {
                Toast.makeText(this, "Por favor, dá pelo menos 1 estrela.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nome.isEmpty()) {
                nome = "Viajante Anónimo"
            }

            btnGuardar.isEnabled = false

            if (uriImagemSelecionada != null) {
                Toast.makeText(this, "A enviar foto e avaliação... ⏳", Toast.LENGTH_SHORT).show()
                fazerUploadDaFotoEGuardar(nome, textoComentario, estrelas, btnGuardar)
            } else {
                Toast.makeText(this, "A enviar avaliação... ⏳", Toast.LENGTH_SHORT).show()
                guardarAvaliacaoComoComentario(nome, textoComentario, estrelas, "", btnGuardar)
            }
        }
    }

    private fun fazerUploadDaFotoEGuardar(nome: String, texto: String, estrelas: Float, btn: Button) {
        val nomeFicheiro = "${UUID.randomUUID()}.jpg"
        val refStorage = storage.reference.child("fotos_estacoes/$idEstacaoLimpo/$nomeFicheiro")

        refStorage.putFile(uriImagemSelecionada!!)
            .addOnSuccessListener {
                refStorage.downloadUrl.addOnSuccessListener { uriDownload ->
                    guardarAvaliacaoComoComentario(nome, texto, estrelas, uriDownload.toString(), btn)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar foto.", Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
            }
    }

    private fun guardarAvaliacaoComoComentario(nome: String, texto: String, estrelas: Float, urlFoto: String, btn: Button) {
        val novoDocumentoRef = db.collection("comentarios").document()

        val novoComentario = Comentario(
            id_comentario = novoDocumentoRef.id,
            id_estacao = idEstacaoLimpo,
            autor = nome,
            texto = texto,
            url_foto = urlFoto,
            timestamp = System.currentTimeMillis(),
            estrelas = estrelas
        )

        novoDocumentoRef.set(novoComentario)
            .addOnSuccessListener {
                Toast.makeText(this, "Avaliação publicada com sucesso! 🎉", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao publicar.", Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
            }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }
}