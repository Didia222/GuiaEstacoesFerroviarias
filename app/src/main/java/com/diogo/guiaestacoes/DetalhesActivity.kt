package com.diogo.guiaestacoes
// Esta activity funciona como um ecra de leitura. Ela não gera dados novos; ela apenas recebe e exibe o que lhe foi enviado pelo mapa.
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalhesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        //onCreate: É o ponto de entrada do ecrã. Mal a atividade é "criada", ela executa o setContentView para carregar o desenho que fiz no ficheiro xml activity_detalhes.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes)
//Intent.getStringExtra No android, os ecrãs não partilham memória diretamente. Para passares o nome de uma estação para um mapa
        val nome = intent.getStringExtra("NOME")
        val tipo = intent.getStringExtra("TIPO")
        //intent.getStringExtra():O código vai ao "envelope" (Intent) e retira o texto que está guardado com a etiqueta "NOME". Faz o mesmo para o "TIPO" e a "DESCRICAO".

        val descricao = intent.getStringExtra("DESCRICAO")

        findViewById<TextView>(R.id.tvTituloDetalhe).text = nome
        findViewById<TextView>(R.id.tvTipoDetalhe).text = tipo
        findViewById<TextView>(R.id.tvConteudoDetalhe).text = descricao ?: "Sem descrição."
    }
}