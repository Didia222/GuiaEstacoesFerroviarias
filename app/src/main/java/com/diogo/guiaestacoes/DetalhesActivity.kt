package com.diogo.guiaestacoes

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class DetalhesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var historiaExpandida = false // Estado da seta (Aberto/Fechado)
    private var textoHistoriaAtual = "A história desta estação ainda não foi catalogada." // Guardar para partilhar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // 1. Ligar as variáveis aos elementos do ecrã
        val toolbar = findViewById<Toolbar>(R.id.toolbarDetalhes)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloDetalhe)
        val tvTipo = findViewById<TextView>(R.id.tvTipoDetalhe)
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        val btnExpandir = findViewById<ImageButton>(R.id.btnExpandirHistoria)
        val llHistoriaLabel = findViewById<View>(R.id.llHistoriaLabel)

        // Os nossos botões novos!
        val btnMapa = findViewById<MaterialButton>(R.id.btnMapaDetalhe)
        val btnPartilhar = findViewById<MaterialButton>(R.id.btnPartilharDetalhe)

        // 2. Configurar a Seta de Voltar atrás
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.navigationIcon?.setTint(Color.WHITE)

        // Engenharia Anti-Notch para empurrar a seta para baixo
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // 3. Receber dados da janela anterior
        val nome = intent.getStringExtra("NOME") ?: "Estação Desconhecida"
        val tipo = intent.getStringExtra("TIPO") ?: ""

        tvTitulo.text = nome
        tvTipo.text = tipo
        tvConteudo.text = "A viajar no tempo..."

        // 4. Ligar ao Firebase para ler a História
        val idDocumento = limparTexto(nome)

        db.collection("historias").document(idDocumento).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val resumo = document.getString("resumo") ?: ""
                    tvConteudo.text = resumo
                    textoHistoriaAtual = resumo // Guardamos para o botão de partilhar!
                } else {
                    tvConteudo.text = textoHistoriaAtual
                }
            }
            .addOnFailureListener {
                tvConteudo.text = "Erro de ligação. Verifica a internet."
            }

        // 5. O Botão da Seta Expansível
        llHistoriaLabel.setOnClickListener {
            historiaExpandida = !historiaExpandida

            if (historiaExpandida) {
                // Expandir o texto completamente
                tvConteudo.maxLines = Integer.MAX_VALUE
                btnExpandir.setImageResource(R.drawable.ic_expand_less)
            } else {
                // Cortar o texto de novo (tesourão a 4 linhas)
                tvConteudo.maxLines = 4
                btnExpandir.setImageResource(R.drawable.ic_expand_more)
            }
        }

        // 6. O Botão "Ver Mapa" (Volta ao Ecrã Principal)
        btnMapa.setOnClickListener {
            // Como a MainActivity do mapa já lá está atrás, basta fechar este ecrã de detalhes
            finish()
        }

        // 7. O Botão "Partilhar"
        btnPartilhar.setOnClickListener {
            val textoPartilha = "Olha que interessante a história da $nome:\n\n$textoHistoriaAtual\n\nPartilhado via App Guia Estações"

            val intentPartilha = Intent(Intent.ACTION_SEND)
            intentPartilha.type = "text/plain"
            intentPartilha.putExtra(Intent.EXTRA_TEXT, textoPartilha)

            // Abre a gaveta nativa do Android a perguntar onde queres partilhar (WhatsApp, SMS, etc)
            startActivity(Intent.createChooser(intentPartilha, "Partilhar história em..."))
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
}