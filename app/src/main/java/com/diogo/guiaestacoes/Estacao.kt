package com.diogo.guiaestacoes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "estacao_table")
//@Entity: indica que esta classe representa uma tabela na base de dados SQLite.
//tableName: Define o nome da tabela real no sistema.
data class Estacao(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    //Define a Chave Primária da tabela.
    // 'autoGenerate = true' faz com que o Room crie um ID único automaticamente
    //Variaveis da tabela onde os dados são armazenados
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val descricao_hist: String
)

