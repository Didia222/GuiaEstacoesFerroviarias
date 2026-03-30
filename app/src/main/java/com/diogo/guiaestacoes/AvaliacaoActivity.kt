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

// Modelo de Dados para a Avaliação
data class Avaliacao(
    val estacaoId: String = "",
    val nomeAvaliador: String = "",
    val estrelas: Float = 0f,
    val timestamp: Long = 0L
)

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
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAvaliacao)

        btnGuardar.setOnClickListener {
            val estrelas = ratingBar.rating
            val nome = etNome.text.toString().trim()

            if (estrelas == 0f) {
                Toast.makeText(this, "Por favor, dá pelo menos 1 estrela.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nome.isEmpty()) {
                Toast.makeText(this, "Por favor, insere o teu nome.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarAvaliacao(nome, estrelas)
        }
    }

    private fun guardarAvaliacao(nome: String, estrelas: Float) {
        val avaliacao = Avaliacao(idEstacaoLimpo, nome, estrelas, System.currentTimeMillis())

        db.collection("avaliacoes").add(avaliacao)
            .addOnSuccessListener {
                Toast.makeText(this, "Avaliação guardada com sucesso!", Toast.LENGTH_LONG).show()
                finish() // Fecha o ecrã e volta para trás
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao guardar na base de dados.", Toast.LENGTH_SHORT).show()
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