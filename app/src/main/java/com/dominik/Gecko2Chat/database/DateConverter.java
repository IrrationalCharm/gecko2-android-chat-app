package com.dominik.Gecko2Chat.database;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateConverter {

    @TypeConverter
    public static LocalDateTime fromTimestamp(Long value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }

    @TypeConverter
    public static Long dateToTimestamp(LocalDateTime date) {
        return date == null ? null : date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
