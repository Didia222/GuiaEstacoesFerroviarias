package com.diogo.guiaestacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

class ComboioAdapter(
    private var lista: List<Any>,
    private var estacaoSelecionada: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    // FUNÇÃO DE LIMPEZA: Como o firebase tem as letras das estações e paragens em letra minuscula e sem acentos os traços e espaços que os nomes das estações podem ter diferente da base do firebase.
    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val semAcentos = "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "")
        return semAcentos.replace("-", " ").replace("\\s+".toRegex(), " ").trim()
    }
    // "// Sinaleiro da lista: verifica se o dado nesta posição é apenas um texto (então desenha um Cabeçalho) ou se é um objeto com dados (então desenha a caixa do Comboio)."
    override fun getItemViewType(position: Int): Int = if (lista[position] is String) TYPE_HEADER else TYPE_ITEM
    // vai buscar o layout item_cabecalho_tipo.xml e item_comboio.xml para moldes visuais de forma a tornar a view prenchida com esas informação
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cabecalho_tipo, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comboio, parent, false)
            ComboioViewHolder(v)
        }
    }
    // Prenchimento do layout formulado pela função anterior com os dados obtidos pela base de dedos e aqui tem-se o uso da função limpearTexto para vasculhar as paragens de um comboio para a Hora de Partida correta no ecrã.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = lista[position]
        if (holder is HeaderViewHolder) {
            holder.tvTitulo.text = item as String
        } else if (holder is ComboioViewHolder) {
            val comboio = item as Comboio
            holder.txtNumero.text = comboio.numero
            holder.txtTipo.text = comboio.tipo
            holder.txtRota.text = comboio.destino

            // --- LÓGICA CORRIGIDA AQUI ---
            val estacaoMapaLimpa = limparTexto(estacaoSelecionada)

            val paragemAqui = comboio.paragens.find { paragem ->
                val nomeFirebaseLimpo = limparTexto(paragem.estacao)
                // Verifica se um nome contém o outro (ex: "Porto Sao Bento" vs "Sao Bento")
                nomeFirebaseLimpo.contains(estacaoMapaLimpa, true) ||
                        estacaoMapaLimpa.contains(nomeFirebaseLimpo, true)
            }

            holder.txtHoraPartida.text = paragemAqui?.hora ?: "--:--"
            // -----------------------------

            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, ItinerarioActivity::class.java)
                // IMPORTANTE: Garante que passas o objeto comboio
                intent.putExtra("COMBOIO_OBJ", comboio)
                intent.putExtra("ESTACAO_ATUAL", estacaoSelecionada)
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = lista.size

    fun atualizarLista(novaLista: List<Any>, estacao: String) {
        this.lista = novaLista
        this.estacaoSelecionada = estacao
        notifyDataSetChanged()
    }

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitulo: TextView = v.findViewById(R.id.tvTituloTipo)
    }

    class ComboioViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val txtNumero: TextView = v.findViewById(R.id.txtNumero)
        val txtTipo: TextView = v.findViewById(R.id.txtTipo)
        val txtRota: TextView = v.findViewById(R.id.txtRota)
        val txtHoraPartida: TextView = v.findViewById(R.id.txtHoraPartida)
    }
}