package com.diogo.guiaestacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

class ComboioAdapter(
    private var items: List<Any>,
    private var nomeEstacaoAtual: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    private var textoPesquisa: String = ""

    fun atualizarLista(novaLista: List<Any>, estacao: String, pesquisa: String = "") {
        this.items = novaLista
        this.nomeEstacaoAtual = estacao
        this.textoPesquisa = pesquisa
        notifyDataSetChanged()
    }

    private fun limparTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalizado, "").uppercase().trim()
    }

    override fun getItemViewType(position: Int): Int = if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cabecalho_tipo, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comboio, parent, false)
            ComboioViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvTitulo.text = items[position] as String
        } else if (holder is ComboioViewHolder) {
            val comboio = items[position] as Comboio
            val estacaoLimpa = limparTexto(nomeEstacaoAtual)
            val buscaLimpa = limparTexto(textoPesquisa)

            val paragemAtual = comboio.paragens.find { limparTexto(it.estacao) == estacaoLimpa }
            holder.tvHora.text = paragemAtual?.hora ?: "--:--"

            var infoExtra = ""
            if (textoPesquisa.isNotBlank()) {
                val indexAtual = comboio.paragens.indexOfFirst { limparTexto(it.estacao) == estacaoLimpa }
                val paragensFuturas = if (indexAtual != -1) comboio.paragens.subList(indexAtual + 1, comboio.paragens.size) else emptyList()
                val pDestino = paragensFuturas.find { limparTexto(it.estacao).contains(buscaLimpa) }

                if (pDestino != null) {
                    infoExtra = if (limparTexto(pDestino.estacao) == limparTexto(comboio.destino)) " (Destino às ${pDestino.hora})" else " (Por ${pDestino.estacao} às ${pDestino.hora})"
                }
            }

            holder.tvNumero.text = "Nº ${comboio.numero}"
            holder.tvRota.text = "$nomeEstacaoAtual ➔ ${comboio.destino}$infoExtra"
            holder.tvTipo.text = when(comboio.tipo) {
                "AP" -> "ALFA PENDULAR"; "IC" -> "INTERCIDADES"; "R" -> "REGIONAL"; "U" -> "URBANO"; else -> comboio.tipo?.uppercase()
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, ItinerarioActivity::class.java).apply {
                    putExtra("COMBOIO_OBJ", comboio)
                    putExtra("ESTACAO_ATUAL", nomeEstacaoAtual)
                    putExtra("DESTINO_PESQUISADO", textoPesquisa)
                }
                it.context.startActivity(intent)
            }
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloCabecalho)
    }

    class ComboioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHora: TextView = view.findViewById(R.id.tvHoraComboio)
        val tvNumero: TextView = view.findViewById(R.id.tvNumeroComboio)
        val tvRota: TextView = view.findViewById(R.id.tvRotaComboio)
        val tvTipo: TextView = view.findViewById(R.id.tvTipoServico)
    }
}