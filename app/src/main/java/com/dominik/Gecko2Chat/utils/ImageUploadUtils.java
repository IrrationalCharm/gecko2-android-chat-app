package com.dominik.Gecko2Chat.utils;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ImageUploadUtils {

    // Helper method to prepare the image for upload
    public static MultipartBody.Part prepareImagePart(Context context, String partName, Uri fileUri) {
        try {
            // 1. Determine the content type (e.g., image/jpeg)
            String mimeType = context.getContentResolver().getType(fileUri);
            if (mimeType == null) mimeType = "image/*"; // Fallback

            // 2. Determine extension (jpg, png) for the temp file
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null) extension = "jpg"; // Default fallback

            // 3. Create a unique temporary file in the app's cache directory
            File file = File.createTempFile("upload_" + System.currentTimeMillis(), "." + extension, context.getCacheDir());

            // 4. Stream data from the Uri to the Temp File
            try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                 OutputStream outputStream = new FileOutputStream(file)) {

                if (inputStream == null) return null;

                byte[] buffer = new byte[4096]; // 4KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            // 5. Create the RequestBody
            RequestBody requestFile = RequestBody.create(file, MediaType.parse(mimeType));

            // 6. Create the MultipartBody.Part
            return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
