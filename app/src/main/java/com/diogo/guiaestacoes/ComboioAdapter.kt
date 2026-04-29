package com.diogo.guiaestacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Normalizer

// Responsável por desenhar a forma como a base de dados fica distribuida no ecrã.
// Pega num objeto da lista de resultados e transforma-o num cartão visual.
class ComboioAdapter(
    private var items: List<Any>,
    private var nomeEstacaoAtual: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Utilização de dois tipos de View para criar uma lista organizada (Secções):
    // TYPE_HEADER  para oss titulos ("Alfa Pendular", "Intercidades", etc)
    // TYPE_ITEM para o cartão do comboio
    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    private var textoPesquisa: String = ""

    // Pesquisa em Tempo real (RNF-2)
    // Esta função é sempre chamada quando o utilizador escreve uma letra na HotatiosActivity,
    //Para atualizar a interface com a lista das estações pesquisadas sem recriar o adaptador do zero.
    fun atualizarLista(novaLista: List<Any>, estacao: String, pesquisa: String = "") {
        this.items = novaLista
        this.nomeEstacaoAtual = estacao
        this.textoPesquisa = pesquisa
        notifyDataSetChanged() // Informa o android que a lista mudou e deve redesenhar o ecrã.
    }

    // Normalização vital: remove acentos e mete tudo em maiúsculas (ex: "SÃO bento" -> "SAO BENTO")
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

    // [LÒGICA CORE - RENDERIZAÇÂO DO CARTÃO]
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvTitulo.text = items[position] as String
        } else if (holder is ComboioViewHolder) {
            val comboio = items[position] as Comboio
            val estacaoLimpa = limparTexto(nomeEstacaoAtual)
            val buscaLimpa = limparTexto(textoPesquisa)

            // 1. Descobre a hora exata que o comboio passa na estação selecionada
            val paragemAtual = comboio.paragens.find { limparTexto(it.estacao) == estacaoLimpa }
            val horaExata = paragemAtual?.hora ?: "--:--"

            // O código compara a estação atual com o destino oficial do comboio.
            // Se a estação se encontra na ultima estação do trajeto selecionada então só diz a chegar.
            if (estacaoLimpa == limparTexto(comboio.destino)) {
                holder.tvHora.text = "Chegada: $horaExata\n(Terminal)"
            } else {
                holder.tvHora.text = "Partida: $horaExata"
            }

            // [LÓGICA DE PESQUISA E AVISOS CONTEXTUAIS]
            var infoExtra = ""
            if (textoPesquisa.isNotBlank()) {
                // // Se o utilizador pesquisou um destino, as paragens seguintes para o destino são isoladas
                val indexAtual = comboio.paragens.indexOfFirst { limparTexto(it.estacao) == estacaoLimpa }
                val paragensFuturas = if (indexAtual != -1) {
                    comboio.paragens.subList(indexAtual + 1, comboio.paragens.size)
                } else {
                    emptyList()
                }

                // Procura-se o destino apenas nas paragens futuras
                val pDestino = paragensFuturas.find { limparTexto(it.estacao).contains(buscaLimpa) }

                if (pDestino != null) {
                    // Prevenção de Redundância Visual (UX):
                    // Se o utilizador pesquisar a última estação, em vez de dizer "Passa por X",
                    // diz elegantemente "Chegada ao destino"
                    if (limparTexto(pDestino.estacao) == limparTexto(comboio.destino)) {
                        infoExtra = "\n(Chegada ao destino às ${pDestino.hora})"
                    } else {
                        infoExtra = "\n(Passa por ${pDestino.estacao} às ${pDestino.hora})"
                    }
                }
            }

            holder.tvNumero.text = "Nº ${comboio.numero}"
            // Rota ajustada para a Experiencia do Utilizador:
            // Mostra de onde ele parte agora (Estação atual) e para onde vai (Destino).

            holder.tvRota.text = "$nomeEstacaoAtual ➔ ${comboio.destino}$infoExtra"

            holder.tvTipo.text = when(comboio.tipo) {
                "AP" -> "Alfa Pendular"; "IC" -> "Intercidades"; "R" -> "Regional"; "U" -> "Urbano"; else -> comboio.tipo
            }

            // TRANSIÇÂO PARA O ITINERÀRIO (RF-5)
            // Quando clica no cartão, passa-se toda a imformação or Intent.
            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, ItinerarioActivity::class.java).apply {
                    putExtra("COMBOIO_OBJ", comboio)
                    putExtra("ESTACAO_ATUAL", nomeEstacaoAtual)
                    putExtra("DESTINO_PESQUISADO", textoPesquisa) // PASSAGEM DO DESTINO PESQUISADO
                }
                it.context.startActivity(intent)
            }
        }
    }


    // Mapeamento dos elementos visuais do ficheiro XML (Layout)
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