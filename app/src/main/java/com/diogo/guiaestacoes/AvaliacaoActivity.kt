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

    // Inicialização dos serviços Cloud: Firestore (Base de dados de texto) e Storage (Armazenamento de ficheiros)
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var idEstacaoLimpo: String
    private var uriImagemSelecionada: Uri? = null
    private lateinit var ivPreviewFoto: ImageView

    // [RF-7: Upload de Fotografia]
    // Utilização do ActivityResultLauncher para obter a URI da imagem selecionada do dispositivo

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

        // Configuração da interface e navegação
        val toolbar = findViewById<Toolbar>(R.id.toolbarAvaliacao)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        //Forçar o clique na seta de voltar (Garante compatibilidade em diferentes versões do Android)
        toolbar.setNavigationOnClickListener { finish() }

        // [RNF-1: Usabilidade] Edge-to-Edge e Insets
        // Este código deteta a altura da barra de estado
        // e empurra a Toolbar para baixo, impedindo que a seta de voltar fique escondida.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        // Obtém o nome da estação que veio do ecrã anterior e cria um ID limpo para a base de dados

        val nomeEstacao = intent.getStringExtra("NOME") ?: ""
        idEstacaoLimpo = limparTexto(nomeEstacao)

        findViewById<TextView>(R.id.tvTituloAvaliacao).text = "Avaliar $nomeEstacao"

        val etComentario = findViewById<EditText>(R.id.etComentarioAvaliacao)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarAvaliacao)
        ivPreviewFoto = findViewById(R.id.ivPreviewFoto)

        // Clique no botão para abrir a galeria
        findViewById<ImageButton>(R.id.btnAdicionarFotoAvaliacao).setOnClickListener {
            escolherImagemLauncher.launch("image/*")
        }

        // [RF-6: Comentários até 200 caracteres]
        // Validação estrita: O utilizador é impedido de submeter a avaliação se exceder o limite.
        btnGuardar.setOnClickListener {
            val texto = etComentario.text.toString().trim()
            if (texto.length > 200) {
                Toast.makeText(this, "Máximo 200 caracteres!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Desativa o botão temporariamente para evitar cliques duplos durante o upload
            btnGuardar.isEnabled = false
            // // Ramificação: Se houver foto, faz upload primeiro. Se não houver, guarda apenas o texto direto.
            if (uriImagemSelecionada != null) fazerUpload(texto, btnGuardar)
            else guardarNoFirestore(texto, "", btnGuardar)
        }
    }

    // [RF-7: Upload de fotografia para a Cloud]
    // Pega no ficheiro físico do telemóvel e envia-o para o Firebase Storage.
    // Usa um UUID (Identificador Universal Único) para que duas fotos não tenham o mesmo nome.
    private fun fazerUpload(texto: String, btn: Button) {
        val ref = storage.reference.child("fotos/$idEstacaoLimpo/${UUID.randomUUID()}.jpg")
        ref.putFile(uriImagemSelecionada!!).addOnSuccessListener {
            // Após o upload ter sucesso, pedimos o link público da foto para associar ao comentário de texto
            ref.downloadUrl.addOnSuccessListener { guardarNoFirestore(texto, it.toString(), btn) }
        }
    }

    // [RF-6: Registo de Comentário, Data, Hora e Utilizador]
    // Esta função centraliza a gravação do documento no Firestore.
    private fun guardarNoFirestore(texto: String, url: String, btn: Button) {
        val ref = db.collection("comentarios").document()
        // Define "Anónimo" caso o utilizador não tenha preenchido o nome
        val nome = findViewById<EditText>(R.id.etNomeAvaliador).text.toString().ifBlank { "Anónimo" }

        // Cria o modelo do comentário
        // O System.currentTimeMillis() regista automaticamente a data e a hora exata da publicação.

        val c = Comentario(ref.id, idEstacaoLimpo, nome, texto, findViewById<RatingBar>(R.id.ratingBar).rating, System.currentTimeMillis(), url)

        ref.set(c).addOnSuccessListener {
            Toast.makeText(this, "Publicado!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Função auxiliar que cria uma chave estrangeira "segura"
    // Remove acentos, espaços e caracteres especiais (ex: "São Bento" vira "SAOBENTO")
    // para podermos filtrar corretamente os comentários de cada estação.
    private fun limparTexto(t: String): String {
        val n = Normalizer.normalize(t, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(n, "").uppercase().trim()
    }
}