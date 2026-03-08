package com.diogo.guiaestacoes
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Estacao::class], version = 1, exportSchema = false) //função que é o chefe da base de dados de dados
//entities indica quais as tabelas que a base de dados vai gerir
//version: É o numero da versao atual da base de dados
//exportSchema = false tem o próposito para que nao crie ficheiros históricos da base de dados do projeto
abstract class AppDatabase : RoomDatabase() {
//classe definida como abstrata para que o room faça o trabalho pessado de escrever o codigo para crias as tabelas

    abstract fun estacaoDao(): EstacaoDao
//abstract fun: liga a base de dados de acordo com os meus comandos equivalente à criação de Queries
    companion object {
    //Impede que a app nao abre a base de dados várias vezes
        @Volatile
        //garante que o valor instance é lido sempre de forma atualizada por todas as partes da app evitando erro de leitura pelos processos

        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                //o sychronized é uma protocolor que impede duas partes da app de lerem a base de dados simultaneamente fazendo-as esperar pela sua vez

                Room.databaseBuilder(
                    context.applicationContext,
                    // Usa o contexto da aplicação para que a base de dados nao morra se mudares de ecrã
                    AppDatabase::class.java,
                    // Nome do ficheiro que contém a base de dados
                    "guia_estacoes_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}