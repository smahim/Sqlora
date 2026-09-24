package com.example.navigation

sealed class Screen(val route: String) {
    object DatabaseList : Screen("database_list")
    
    object DatabaseDetail : Screen("database_detail/{databaseId}") {
        fun createRoute(databaseId: Long): String = "database_detail/$databaseId"
    }

    object TableViewer : Screen("table_viewer/{databaseId}/{tableName}") {
        fun createRoute(databaseId: Long, tableName: String): String = "table_viewer/$databaseId/$tableName"
    }

    object SqlConsole : Screen("sql_console?databaseId={databaseId}") {
        fun createRoute(databaseId: Long? = null): String {
            return if (databaseId != null) "sql_console?databaseId=$databaseId" else "sql_console"
        }
    }

    object Settings : Screen("settings")
}
