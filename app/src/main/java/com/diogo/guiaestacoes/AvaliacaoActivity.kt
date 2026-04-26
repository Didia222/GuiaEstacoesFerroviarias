package com.diogo.guiaestacoes

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
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

    private val escolherImagemLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uriImagemSelecionada = it
            ivPreviewFoto.setImageURI(it)
            ivPreviewFoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avaliacao)

        // 1. Configurar Toolbar e Seta de Voltar
        val toolbar = findViewById<Toolbar>(R.id.toolbarAvaliacao)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // CORREÇÃO: Forçar o clique na seta de voltar
        toolbar.setNavigationOnClickListener { finish() }

        // CORREÇÃO: Empurrar a barra para baixo da câmara (Notch)
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        val nomeEstacao = intent.getStringExtra("NOME") ?: ""
        idEstacaoLimpo = limparTexto(nomeEstacao)

        findViewById<TextView>(R.id.tvTituloAvaliacao).text = "Avaliar $nomeEstacao"

        val etComentario = findViewById<EditText>(R.id.etComentarioAvaliacao)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAvaliacao)
        ivPreviewFoto = findViewById(R.id.ivPreviewFoto)

        findViewById<ImageButton>(R.id.btnAdicionarFotoAvaliacao).setOnClickListener {
            escolherImagemLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            val texto = etComentario.text.toString().trim()
            if (texto.length > 200) {
                Toast.makeText(this, "Máximo 200 caracteres!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnGuardar.isEnabled = false
            if (uriImagemSelecionada != null) fazerUpload(texto, btnGuardar)
            else guardarNoFirestore(texto, "", btnGuardar)
        }
    }

    private fun fazerUpload(texto: String, btn: Button) {
        val ref = storage.reference.child("fotos/$idEstacaoLimpo/${UUID.randomUUID()}.jpg")
        ref.putFile(uriImagemSelecionada!!).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { guardarNoFirestore(texto, it.toString(), btn) }
        }
    }

    private fun guardarNoFirestore(texto: String, url: String, btn: Button) {
        val ref = db.collection("comentarios").document()
        val nome = findViewById<EditText>(R.id.etNomeAvaliador).text.toString().ifBlank { "Anónimo" }

        val c = Comentario(ref.id, idEstacaoLimpo, nome, texto, findViewById<RatingBar>(R.id.ratingBar).rating, System.currentTimeMillis(), url)

        ref.set(c).addOnSuccessListener {
            Toast.makeText(this, "Publicado!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }
}