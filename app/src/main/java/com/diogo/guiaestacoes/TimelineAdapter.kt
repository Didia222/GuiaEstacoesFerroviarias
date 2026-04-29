package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// [FUNCIONALIDADE: RF-7 - Valorização do Património (Cronologia)]
// [PERFORMANCE: RNF-2 e RNF-9 - Escalabilidade e Manutenibilidade]
// O TimelineAdapter tem a responsabilidade de desenhar a linha do tempo histórica da estação.
class TimelineAdapter(private val eventos: List<EventoHistorico>) :
    RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {
    // [PADRÃO VIEWHOLDER]
    // A cache visual. Guarda as referências do Ano e do Evento para que o telemóvel não
    // tenha de pesquisar a árvore de layouts (findViewById) repetidamente durante o scroll.
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvAno: TextView = v.findViewById(R.id.tvAnoTimeline)
        val tvEvento: TextView = v.findViewById(R.id.tvEventoTimeline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflação do layout. Aqui, o Android lê o ficheiro XML 'item_timeline' e transforma-o
        // num objeto visual na memória (View) que representa um único nó da nossa linha do tempo.
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(v)
    }
    // [DATA BINDING]
    // Liga a Lógica de Negócio (O objeto EventoHistorico) à Interface Gráfica (TextViews).
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = eventos[position]
        // Atribuição direta. Assumimos que a Activity que chama este adaptador
        // já entregou a lista de eventos ordenada cronologicamente.
        holder.tvAno.text = item.ano
        holder.tvEvento.text = item.acontecimento
    }

    override fun getItemCount() = eventos.size
}
