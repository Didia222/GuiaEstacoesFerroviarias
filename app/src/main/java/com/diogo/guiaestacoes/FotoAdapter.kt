package com.diogo.guiaestacoes

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners


class FotoAdapter(private val urls: List<String>) : RecyclerView.Adapter<FotoAdapter.FotoViewHolder>(){
    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val imageView: ImageView = itemView as ImageView

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
     val imageView = ImageView(parent.context).apply {
         layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450).apply {
             setMargins(8, 8, 8, 8)

         }
         scaleType = ImageView.ScaleType.CENTER_CROP


     }
        return FotoViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        Glide.with(holder.itemView).load(urls[position]).transform(CenterCrop(), RoundedCorners(32)).into(holder.imageView)

    }

    override fun getItemCount(): Int = urls.size


}


