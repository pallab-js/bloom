package com.vibenote.app.data.local

import androidx.room.TypeConverter
import com.vibenote.app.domain.model.CanvasBackground

class Converters {
    @TypeConverter
    fun fromCanvasBackground(value: CanvasBackground): String = value.key

    @TypeConverter
    fun toCanvasBackground(value: String): CanvasBackground = CanvasBackground.fromKey(value)
}
