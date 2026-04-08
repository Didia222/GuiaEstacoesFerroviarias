package com.diogo.guiaestacoes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class DetalhesActivity : AppCompatActivity() {

    private var isExpanded = false
    private var uriImagemSelecionada: Uri? = null

    // Variáveis do Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Variáveis da Estação
    private var nomeEstacao: String = ""

    // Lançador para abrir a Galeria
    private val selecionarImagemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            uriImagemSelecionada = data?.data
            if (uriImagemSelecionada != null) {
                Toast.makeText(this, "Foto selecionada com sucesso! 📷", Toast.LENGTH_SHORT).show()
                // Aqui poderíamos mudar a cor do botão ou mostrar um mini-preview
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Receber dados da MainActivity
        nomeEstacao = intent.getStringExtra("NOME") ?: ""
        val tipo = intent.getStringExtra("TIPO") ?: ""
        val historia = intent.getStringExtra("HISTORIA") ?: ""
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lng = intent.getDoubleExtra("LONGITUDE", 0.0)

        // Configurar Interface da História
        val tvTitulo = findViewById<TextView>(R.id.tvTituloDetalhe)
        val tvTipo = findViewById<TextView>(R.id.tvTipoDetalhe)
        val tvConteudo = findViewById<TextView>(R.id.tvConteudoDetalhe)
        val btnExpandir = findViewById<ImageButton>(R.id.btnExpandirHistoria)
        val btnVerNoMapa = findViewById<MaterialButton>(R.id.btnMapaDetalhe)

        tvTitulo.text = nomeEstacao
        tvTipo.text = tipo
        tvConteudo.text = historia

        // Botão Ver no Mapa
        btnVerNoMapa.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("LAT_RETORNO", lat)
                putExtra("LNG_RETORNO", lng)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }

        // Botão Expandir História
        btnExpandir.setOnClickListener {
            if (isExpanded) {
                tvConteudo.maxLines = 4
                btnExpandir.setImageResource(R.drawable.ic_expand_more)
            } else {
                tvConteudo.maxLines = Integer.MAX_VALUE
                btnExpandir.setImageResource(R.drawable.ic_expand_less) // Opcional: cria um ícone ic_expand_less
            }
            isExpanded = !isExpanded
        }

        // --- LÓGICA DOS COMENTÁRIOS E FOTOS ---

        val btnTirarFoto = findViewById<ImageButton>(R.id.btnTirarFoto)
        val btnEnviarComentario = findViewById<ImageButton>(R.id.btnEnviarComentario)
        val etNovoComentario = findViewById<EditText>(R.id.etNovoComentario)

        // 1. Abrir Galeria
        btnTirarFoto.setOnClickListener {
            abrirGaleria()
        }

        // 2. Enviar Comentário
        btnEnviarComentario.setOnClickListener {
            val textoComentario = etNovoComentario.text.toString().trim()

            if (textoComentario.isEmpty() && uriImagemSelecionada == null) {
                Toast.makeText(this, "Escreve algo ou escolhe uma foto!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Desativar botão para não enviar 2 vezes
            btnEnviarComentario.isEnabled = false
            Toast.makeText(this, "A enviar... ⏳", Toast.LENGTH_SHORT).show()

            if (uriImagemSelecionada != null) {
                // Tem foto: Fazer upload primeiro
                fazerUploadImagemEGuardarComentario(textoComentario, etNovoComentario, btnEnviarComentario)
            } else {
                // Não tem foto: Guardar só o texto diretamente
                guardarComentarioNoFirestore(textoComentario, "", etNovoComentario, btnEnviarComentario)
            }
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selecionarImagemLauncher.launch(intent)
    }

    private fun fazerUploadImagemEGuardarComentario(texto: String, inputField: EditText, btnEnviar: ImageButton) {
        val fileName = UUID.randomUUID().toString() + ".jpg" // Nome único para a foto
        val refStorage = storage.reference.child("fotos_estacoes/$fileName")

        refStorage.putFile(uriImagemSelecionada!!)
            .addOnSuccessListener {
                // Upload com sucesso! Agora vamos pedir o link público
                refStorage.downloadUrl.addOnSuccessListener { uri ->
                    val urlDaFoto = uri.toString()
                    guardarComentarioNoFirestore(texto, urlDaFoto, inputField, btnEnviar)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao enviar foto: ${e.message}", Toast.LENGTH_LONG).show()
                btnEnviar.isEnabled = true
            }
    }

    private fun guardarComentarioNoFirestore(texto: String, urlFoto: String, inputField: EditText, btnEnviar: ImageButton) {
        // Criar o objeto usando o nosso "molde"
        val idComentarioGerado = db.collection("Comentarios").document().id
        val novoComentario = Comentario(
            id_comentario = idComentarioGerado,
            id_estacao = nomeEstacao,
            autor = "Viajante Anónimo",
            texto = texto,
            url_foto = urlFoto
        )

        db.collection("Comentarios").document(idComentarioGerado)
            .set(novoComentario)
            .addOnSuccessListener {
                Toast.makeText(this, "Comentário publicado! 🎉", Toast.LENGTH_SHORT).show()
                // Limpar o campo e a foto
                inputField.text.clear()
                uriImagemSelecionada = null
                btnEnviar.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao publicar comentário.", Toast.LENGTH_SHORT).show()
                btnEnviar.isEnabled = true
            }
    }
}