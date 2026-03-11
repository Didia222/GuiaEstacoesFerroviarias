package com.diogo.guiaestacoes
//Processador de dados
import android.content.Context
import android.util.Log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

object CsvHelper {
//CsvHelper: O uso do object em vez de class, estás a criar um Singleton. Isso significa que não precisas de instanciar o helper (fazer val helper = CsvHelper()) para usar seus métodos globalmente por toda a app.


    fun carregarEstacoesDoCsv(context: Context): List<Estacao> {
        //
        return try {
            val inputStream = context.assets.open("estacoes.csv")
            carregarDeStream(inputStream)
            //O código lê os bytes do ficheiro primeiro. Isto é inteligente porque permite que a mesma lógica
            //Conversão de Charset: Força a leitura em UTF-8 para garantir que os nomes das estações com acentos (como Campanhã ou Marco) não fiquem com caracteres estranhos.

        } catch (e: Exception) {
            Log.e("CsvHelper", "Erro ao abrir asset: ${e.message}")
            emptyList()
        }
    }
    //Leitura do fluxo de dados do csv presente no github
    fun carregarDeStream(inputStream: InputStream): List<Estacao> {
        val bytes = inputStream.readBytes()
        val conteudo = String(bytes, Charset.forName("UTF-8"))
        // O código lê os bytes do ficheiro primeiro. Isto é inteligente porque permite que a mesma lógica
        //Conversão de Charset: Força a leitura em UTF-8 para garantir que os nomes das estações com acentos nao fiquem com caracteres estranhos.


        
        // Tenta detetar o delimitador (vírgula ou ponto e vírgula)
        val delimitador = if (conteudo.contains(";")) ';' else ','
        //Esta é uma parte muito robusta do teu código. Ele verifica se o contém ; ou,.
        val listaEstacoes = mutableListOf<Estacao>()
        try {
            val reader = InputStreamReader(bytes.inputStream(), Charset.forName("UTF-8"))

            val csvParser = CSVFormat.Builder.create()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader)

            for (record in csvParser) {
                try {
                    val latStr = getCellValue(record, listOf("latitude", "lat", "@lat"))?.replace(",", ".")
                    val lonStr = getCellValue(record, listOf("longitude", "lon", "long", "@lon"))?.replace(",", ".")
                    val nome = getCellValue(record, listOf("nome", "name", "estacao"))
                    val tipo = getCellValue(record, listOf("tipo", "railway", "type")) ?: "station"

                    if (latStr != null && lonStr != null && nome != null) {
                        val estacao = Estacao(
                            nome = nome,
                            latitude = latStr.toDouble(),
                            longitude = lonStr.toDouble(),
                            descricao_hist = if (tipo.contains("station", true)) "Estação Ferroviária" else "Apeadeiro"
                        )
                        listaEstacoes.add(estacao)
                    }
                } catch (e: Exception) {
                    // Linha inválida
                }
            }
        } catch (e: Exception) {
            Log.e("CsvHelper", "Erro no parser: ${e.message}")
        }
        return listaEstacoes
    }
    //Inspetor de segurança ele assegura que a informações vinda do csv online vem bem formatada e se nao tiver dá um aviso
    private fun getCellValue(record: CSVRecord, potentialNames: List<String>): String? {
        for (name in potentialNames) {
            try {
                val value = record.get(name)
                if (value != null && value.isNotBlank()) return value
            } catch (e: Exception) {}
        }
        return null
    }
}
