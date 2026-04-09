package com.diogo.guiaestacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComentarioAdapter(private val listaComentarios: List<Comentario>) :
    RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder>() {

    class ComentarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAutor: TextView = view.findViewById(R.id.tvAutorItem)
        val tvTexto: TextView = view.findViewById(R.id.tvTextoItem)
        val ivFoto: ImageView = view.findViewById(R.id.ivFotoItem)

        // As duas variáveis novas!
        val rbEstrelas: RatingBar = view.findViewById(R.id.rbEstrelasItem)
        val tvDataHora: TextView = view.findViewById(R.id.tvDataHoraItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comentario, parent, false)
        return ComentarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComentarioViewHolder, position: Int) {
        val comentario = listaComentarios[position]

        // 1. Nome do Autor
        holder.tvAutor.text = comentario.autor

        // 2. Data e Hora formatadas
        val formatador = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        val dataFormatada = formatador.format(Date(comentario.timestamp))
        holder.tvDataHora.text = dataFormatada

        // 3. Texto do Comentário (Esconder se estiver vazio)
        if (comentario.texto.trim().isNotEmpty()) {
            holder.tvTexto.visibility = View.VISIBLE
            holder.tvTexto.text = comentario.texto
        } else {
            holder.tvTexto.visibility = View.GONE
        }

        // 4. Lógica das Estrelas
        if (comentario.estrelas > 0f) {
            holder.rbEstrelas.visibility = View.VISIBLE
            holder.rbEstrelas.rating = comentario.estrelas
        } else {
            // Se foi um comentário feito sem ser pela Avaliação, não mostra estrelas vazias
            holder.rbEstrelas.visibility = View.GONE
        }

        // 5. Lógica da Foto com Glide
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