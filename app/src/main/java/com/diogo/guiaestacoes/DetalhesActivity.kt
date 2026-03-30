package com.diogo.guiaestacoes

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class DetalhesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var historiaExpandida = false // Estado do texto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // Ligar às Views
        val toolbar = findViewById<Toolbar>(R.id.toolbarDetalhes)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloDetalhe)
        val tvTipo = findViewById<TextView>(R.id.tvTipoDetalhe)
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        val btnExpandir = findViewById<ImageButton>(R.id.btnExpandirHistoria)
        val llHistoriaLabel = findViewById<View>(R.id.llHistoriaLabel)

        // Configurar a Toolbar (Seta de Voltar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.navigationIcon?.setTint(Color.WHITE)

        // --- ENGENHARIA ANTI-NOTCH (Correção do Voltar muito acima) ---
        // Vamos ler a altura da barra de estado e aplicar como padding na Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0) // Empurra para baixo automaticamente
            insets
        }
        // -------------------------------------------------------------

        // Receber dados da Intent
        val nome = intent.getStringExtra("NOME") ?: "Estação Desconhecida"
        val tipo = intent.getStringExtra("TIPO") ?: ""

        tvTitulo.text = nome
        tvTipo.text = tipo
        tvConteudo.text = "A viajar no tempo..."

        val idDocumento = limparTexto(nome)

        // Carregar História do Firebase
        db.collection("historias").document(idDocumento).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val resumo = document.getString("resumo")
                    tvConteudo.text = resumo
                } else {
                    tvConteudo.text = "A história desta estação ainda não foi catalogada."
                }
            }
            .addOnFailureListener {
                tvConteudo.text = "Erro de ligação. Verifica a internet."
            }

        // --- LÓGICA DO TEXTO EXPANSÍVEL (A Seta que puxa o texto) ---
        // Fazemos a linha inteira (título + seta) ser clicável para facilitar
        llHistoriaLabel.setOnClickListener {
            historiaExpandida = !historiaExpandida // Inverte o estado

            if (historiaExpandida) {
                // Expandir o texto completamente
                tvConteudo.maxLines = Integer.MAX_VALUE
                btnExpandir.setImageResource(R.drawable.ic_expand_less) // Seta para cima
            } else {
                // Cortar o texto de novo (tesourão)
                tvConteudo.maxLines = 4
                btnExpandir.setImageResource(R.drawable.ic_expand_more) // Seta para baixo
            }
        }
        // ------------------------------------------------------------
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
}