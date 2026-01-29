package com.dominik.Gecko2Chat.database;

import androidx.room.TypeConverter;

import com.dominik.Gecko2Chat.enums.MessageStatus;

import java.time.Instant;

public class Converters {

    @TypeConverter
    public static String fromStatus(MessageStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static MessageStatus toStatus(String status) {
        return status == null ? null : MessageStatus.valueOf(status);
    }

    @TypeConverter
    public static Instant fromTimestamp(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Instant date) {
        return date == null ? null : date.toEpochMilli();
    }
}