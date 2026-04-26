package com.vibenote.app.domain.model

enum class CanvasBackground(val key: String) {
    DARK("dark"), 
    WHITE("white"), 
    LINED("lined"), 
    DOTTED("dotted"), 
    GRID("grid");

    companion object {
        fun fromKey(key: String): CanvasBackground {
            return entries.find { it.key == key } ?: DARK
        }
    }
}
