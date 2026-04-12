package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimelineAdapter(private val eventos: List<EventoHistorico>) :
    RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvAno: TextView = v.findViewById(R.id.tvAnoTimeline)
        val tvEvento: TextView = v.findViewById(R.id.tvEventoTimeline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = eventos[position]
        holder.tvAno.text = item.ano
        holder.tvEvento.text = item.acontecimento
    }

    override fun getItemCount() = eventos.size
}
