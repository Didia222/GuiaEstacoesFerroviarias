package com.diogo.guiaestacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//Define o comportamento da lista de comboios na intergace
class ComboioAdapter(
    private var lista: List<Any>,
    private var estacaoSelecionada: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
// constantes para distinguir os tipos de elementos na lista
    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
// Identifica se o dado na posição x é um titulo de categoria ou um comboio
    override fun getItemViewType(position: Int): Int = if (lista[position] is String) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cabecalho_tipo, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comboio, parent, false)
            ComboioViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = lista[position]
        if (holder is HeaderViewHolder) {
            holder.tvTitulo.text = item as String
        } else if (holder is ComboioViewHolder) {
            val comboio = item as Comboio
            holder.txtNumero.text = comboio.numero
            holder.txtTipo.text = comboio.tipo
            holder.txtRota.text = comboio.destino

            // Procura a hora exata nesta estação específica
            val paragemAqui = comboio.paragens.find { it.estacao.contains(estacaoSelecionada, true) }
            holder.txtHoraPartida.text = paragemAqui?.hora ?: "--:--"

            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, ItinerarioActivity::class.java)
                intent.putExtra("COMBOIO_OBJ", comboio)
                intent.putExtra("ESTACAO_ORIGEM", estacaoSelecionada)
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
