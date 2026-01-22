package com.dominik.Gecko2Chat.database;

import androidx.room.TypeConverter;

import java.time.Instant;

public class DateConverter {

    @TypeConverter
    public static Instant fromTimestamp(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Instant date) {
        return date == null ? null : date.toEpochMilli();
    }
}
