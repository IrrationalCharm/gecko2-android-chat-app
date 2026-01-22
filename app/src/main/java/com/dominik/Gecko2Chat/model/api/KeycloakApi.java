package com.dominik.Gecko2Chat.model.api;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface KeycloakApi {

    @FormUrlEncoded
    @POST("/realms/gecko2-realm/protocol/openid-connect/revoke")
    Call<Void> revokeToken(@Field("client_id") String clientId,
                           @Field("token") String token,
                           @Field("token_type_hint") String tokenTypeHint);
}
