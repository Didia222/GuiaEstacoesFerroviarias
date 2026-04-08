package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ComentarioAdapter(private val listaComentarios: List<Comentario>) :
    RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

    // 1. O ViewHolder é quem segura os componentes do XML
    class ComentarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAutor: TextView = view.findViewById(R.id.tvAutorItem)
        val tvTexto: TextView = view.findViewById(R.id.tvTextoItem)
        val ivFoto: ImageView = view.findViewById(R.id.ivFotoItem)
    }

    // 2. Infla o layout item_comentario que criámos
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(view)
    }

    // 3. Liga os dados do objeto Comentario aos componentes visuais
    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = listaComentarios[position]

        holder.tvAutor.text = comentario.autor
        holder.tvTexto.text = comentario.texto

        // Lógica da Foto com Glide
        if (comentario.url_foto.isNotEmpty()) {
            holder.ivFoto.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(comentario.url_foto)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.ivFoto)
        } else {
            holder.ivFoto.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listaComentarios.size
}