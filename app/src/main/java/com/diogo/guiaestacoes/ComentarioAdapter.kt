package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComentarioAdapter(private val comentarios: List<Comentario>) : RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

    // Usa um layout nativo do Android que já tem duas linhas de texto (simple_list_item_2)
    inner class ComentarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtConteudo: TextView = itemView.findViewById(android.R.id.text1)
        val txtData: TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = comentarios[position]

        // Formata o Nome e o Conteúdo
        holder.txtConteudo.text = "${comentario.nome_autor} disse:\n${comentario.conteudo}"

        // Formata o timestamp (Long) para uma data legível
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(comentario.data_hora))
        holder.txtData.text = dataFormatada
    }

    override fun getItemCount(): Int = comentarios.size
}