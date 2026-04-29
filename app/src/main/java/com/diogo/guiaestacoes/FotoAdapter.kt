package com.diogo.guiaestacoes

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

// [Funcionalidade: RF-7 - Valorização do Património (Galeria)]
// [Perfomance: RNF-2 - Gestão de Memória e UI Fluida]
// O FotoAdapter é responsável por renderizar o arquivo histórico de imagens da estação.
class FotoAdapter(private val urls: List<String>) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>(){
    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        // [PADRÃO VIEWHOLDER]
        // Faz cache das referências visuais para evitar chamar o 'findViewById' repetidamente
        val imageView: ImageView = itemView as ImageView

    }

    // Como este item da lista é composto apenas por Uma ImageView,
    // decidi gerá-la programaticamente.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
     val imageView = ImageView(parent.context).apply {
         // Definição das dimensões (Altura fixa de 450px) e margens (8px em cada lado)
         layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450).apply {
             setMargins(8, 8, 8, 8)

         }
         // CenterCrop: Garante que a imagem preenche o espaço todo sem ficar distorcida (esticada),
         // cortando os excessos nas bordas de forma simétrica.
         scaleType = ImageView.ScaleType.CENTER_CROP


     }
        return FotoViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        // [LÓGICA CORE - RENDERIZAÇÃO ASSÍNCRONA E CACHE]
        // Utilização da biblioteca GLIDE para processar as imagens da Cloud (Firebase Storage ou URLs externas).
        Glide.with(holder.itemView)
        Glide.with(holder.itemView).load(urls[position]).transform(CenterCrop(), RoundedCorners(32)).into(holder.imageView)

    }

    override fun getItemCount(): Int = urls.size


}


