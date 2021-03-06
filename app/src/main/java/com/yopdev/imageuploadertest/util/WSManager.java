package com.yopdev.imageuploadertest.util;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by sirkuryaki on 27/08/2018.
 * YOPdev.com
 */
public class WSManager {

    private static final MediaType JPG = MediaType.parse("image/jpeg");
    private final String mAcceptLanguage;
    private final Executor networkIO;
    private final OkHttpClient httpclient = getNewHttpClient();


    public WSManager(Executor networkIO) {
        this.networkIO = networkIO;
        mAcceptLanguage = Locale.getDefault().toString().replace("_", "-");
    }

    private OkHttpClient getNewHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(false);

        return builder.build();
    }

    @NonNull
    public LiveData<WSResponse<String>> postImage(@NonNull String url,
                                                  @NonNull String applicationToken,
                                                  @NonNull String accessToken,
                                                  @NonNull String fileUri,
                                                  @NonNull String formDataPartName,
                                                  @NonNull String formDataFilename) {
        return new LiveData<WSResponse<String>>() {

            final AtomicBoolean started = new AtomicBoolean(false);

            @Override
            protected void onActive() {

                super.onActive();
                if (started.compareAndSet(false, true)) {
                    networkIO.execute(() -> postValue(executeImageRequest(url, applicationToken, accessToken, fileUri, formDataPartName, formDataFilename)));
                }
            }
        };
    }

    @NonNull
    private WSResponse<String> executeImageRequest(@NonNull String url,
                                                   @NonNull String applicationToken,
                                                   @NonNull String accessToken,
                                                   @NonNull String fileUri,
                                                   @NonNull String formDataPartName,
                                                   @NonNull String formDataFilename) {

        WSResponse<String> response = new WSResponse<>();

        File file = new File(fileUri);

        try {
            RequestBody bodyImage = RequestBody.create(JPG, file);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(formDataPartName, formDataFilename, bodyImage)
                    .build();

            Request.Builder request = new Request.Builder()
                    .url(url)
                    .post(requestBody);

            request.addHeader("Accept-Language", mAcceptLanguage);
            request.addHeader("Authorization", "Bearer " + applicationToken);
            request.addHeader("Authorizationpostulante", accessToken);

            try {
                Response executed = httpclient.newCall(request.build()).execute();
                response.setHttpCode(executed.code());
                response.setBody(response.getHttpCode() + ": " + executed.body().string());
            } catch (IOException e) {
                response.setBody(e.toString());
                response.setHttpCode(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
            }

        } catch (OutOfMemoryError error) {
            response.setBody("OutOfMemoryError");
            response.setHttpCode(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
        }

        response.setData(response.getBody());
        return response;
    }
}
