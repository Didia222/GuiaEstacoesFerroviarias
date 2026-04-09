package com.diogo.guiaestacoes

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
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

class GaleriaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var idEstacaoLimpo: String
    private lateinit var nomeEstacaoOficial: String

    private val listaFotos = mutableListOf<String>()
    private lateinit var adapterFotos: FotoAdapter

    private val listaComentarios = mutableListOf<Comentario>()
    private lateinit var adapterComentarios: ComentarioAdapter

    private val escolherImagemLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            fazerUploadDaImagem(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria)

        nomeEstacaoOficial = intent.getStringExtra("NOME") ?: "Estação"
        idEstacaoLimpo = limparTexto(nomeEstacaoOficial)

        val toolbar = findViewById<Toolbar>(R.id.toolbarGaleria)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Galeria de $nomeEstacaoOficial"
        toolbar.navigationIcon?.setTint(Color.BLACK)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }

        val rvFotos = findViewById<RecyclerView>(R.id.rvFotos)
        rvFotos.layoutManager = GridLayoutManager(this, 2)
        adapterFotos = FotoAdapter(listaFotos)
        rvFotos.adapter = adapterFotos

        val rvComentarios = findViewById<RecyclerView>(R.id.rvComentarios)
        rvComentarios.layoutManager = LinearLayoutManager(this)
        adapterComentarios = ComentarioAdapter(listaComentarios)
        rvComentarios.adapter = adapterComentarios

        // O NOVO BOTÃO DE AVALIAÇÃO
        findViewById<Button>(R.id.btnAbrirAvaliacao).setOnClickListener {
            val intent = Intent(this, AvaliacaoActivity::class.java).apply {
                putExtra("NOME", nomeEstacaoOficial)
            }
            startActivity(intent)
        }

        findViewById<FloatingActionButton>(R.id.fabAdicionarFoto).setOnClickListener {
            escolherImagemLauncher.launch("image/*")
        }

        carregarFotosDoFirebase()
        carregarComentariosDoFirebase()
    }

    private fun fazerUploadDaImagem(uri: Uri) {
        val nomeFicheiro = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("fotos_estacoes/${idEstacaoLimpo}/${nomeFicheiro}")

        Toast.makeText(this, "A fazer upload para a Galeria...", Toast.LENGTH_SHORT).show()

        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                guardarFotoNoFirestore(url.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro no upload", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarFotoNoFirestore(url: String) {
        val idFoto = db.collection("fotos_estacoes").document().id
        val fotoData = hashMapOf(
            "id_foto" to idFoto,
            "id_estacao" to idEstacaoLimpo,
            "caminho_ficheiro" to url,
            "ano" to java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        )

        db.collection("fotos_estacoes").document(idFoto).set(fotoData)
            .addOnSuccessListener {
                Toast.makeText(this, "Foto adicionada à galeria!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarFotosDoFirebase() {
        db.collection("fotos_estacoes")
            .whereEqualTo("id_estacao", idEstacaoLimpo)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                listaFotos.clear()
                for (doc in snapshots) {
                    val url = doc.getString("caminho_ficheiro") ?: ""
                    if (url.isNotEmpty()) listaFotos.add(url)
                }
                if (listaFotos.isEmpty()) {
                    listaFotos.add("https://images.unsplash.com/photo-1541427468627-a89a96e5ca1d?w=500")
                }
                adapterFotos.notifyDataSetChanged()
            }
    }

    private fun carregarComentariosDoFirebase() {
        db.collection("comentarios")
            .whereEqualTo("id_estacao", idEstacaoLimpo)
            // APAGÁMOS A LINHA DO ORDERBY AQUI!
            .addSnapshotListener { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                listaComentarios.clear()
                for (doc in snapshots) {
                    val c = doc.toObject(Comentario::class.java)
                    listaComentarios.add(c)
                }
                // ORDENAÇÃO LOCAL AQUI TAMBÉM:
                listaComentarios.sortByDescending { it.timestamp }

                adapterComentarios.notifyDataSetChanged()
            }
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex()
            .replace(normalizado, "")
            .replace("-", " ").replace("\\s+".toRegex(), " ").trim().uppercase()
    }
}