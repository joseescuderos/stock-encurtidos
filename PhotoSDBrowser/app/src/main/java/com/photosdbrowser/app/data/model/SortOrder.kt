package com.photosdbrowser.app.data.model

/** Criterio de ordenación para carpetas y fotos, elegible desde la barra superior. */
enum class SortOrder(val label: String) {
    NAME("Nombre (A-Z)"),
    DATE_DESC("Más recientes primero")
}
