package com.diogo.guiaestacoes

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

// A antiga "data class Avaliacao" foi apagada daqui. Agora usamos o Comentario!

class AvaliacaoActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var idEstacaoLimpo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avaliacao)

        val nomeEstacao = intent.getStringExtra("NOME") ?: "Estação Desconhecida"
        idEstacaoLimpo = limparTexto(nomeEstacao)

        // UI Setup
        val toolbar = findViewById<Toolbar>(R.id.toolbarAvaliacao)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.navigationIcon?.setTint(Color.BLACK)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        val tvTitulo = findViewById<TextView>(R.id.tvTituloAvaliacao)
        tvTitulo.text = "Avaliar $nomeEstacao"

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val etNome = findViewById<EditText>(R.id.etNomeAvaliador)
        val etComentario = findViewById<EditText>(R.id.etComentarioAvaliacao) // O campo de texto que adicionámos
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAvaliacao)

        btnGuardar.setOnClickListener {
            val estrelas = ratingBar.rating
            var nome = etNome.text.toString().trim()
            val textoComentario = etComentario.text.toString().trim()

            if (estrelas == 0f) {
                Toast.makeText(this, "Por favor, dá pelo menos 1 estrela.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nome.isEmpty()) {
                nome = "Viajante Anónimo" // Opcional: Se não preencher nome
            }

            // Agora enviamos para os COMENTÁRIOS
            guardarAvaliacaoComoComentario(nome, textoComentario, estrelas)
        }
    }

    private fun guardarAvaliacaoComoComentario(nome: String, texto: String, estrelas: Float) {
        // Apontamos diretamente para a coleção 'comentarios'
        val novoDocumentoRef = db.collection("comentarios").document()

        val novoComentario = Comentario(
            id_comentario = novoDocumentoRef.id,
            id_estacao = idEstacaoLimpo,
            autor = nome,
            texto = texto,
            url_foto = "",
            timestamp = System.currentTimeMillis(),
            estrelas = estrelas // Salvamos as estrelas aqui!
        )

        novoDocumentoRef.set(novoComentario)
            .addOnSuccessListener {
                Toast.makeText(this, "Publicado com sucesso!", Toast.LENGTH_LONG).show()
                finish() // Volta para o mapa ou para os detalhes
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao publicar.", Toast.LENGTH_SHORT).show()
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
}