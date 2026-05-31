package com.example.acordehub.api;

import com.example.acordehub.modelos.spotify.SpotifySearchResponse;
import com.example.acordehub.modelos.spotify.TokenResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SpotifyService {
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("api/token")
    Call<TokenResponse> getToken(
            @Header("Authorization") String basicAuth,
            @Field("grant_type") String grantType
    );

    @Headers("Accept: application/json")
    @GET("search")
    Call<SpotifySearchResponse> searchArtists(
            @Header("Authorization") String token,
            @Query("q") String query,
            @Query("type") String type,
            @Query("market") String market
    );
}
