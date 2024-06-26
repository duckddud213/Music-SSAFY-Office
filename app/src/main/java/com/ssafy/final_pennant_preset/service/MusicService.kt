package com.ssafy.final_pennant_preset.service

import com.ssafy.final_pennant_preset.dto.ServerMusicDTO
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface MusicService {

    @GET("/music/genre/{genre}")
    suspend fun getMusicListByGenre(@Path("genre") genre: String): ArrayList<ServerMusicDTO>

    @GET("/music/{id}")
    suspend fun getMusicById(@Path("id") id: String): ServerMusicDTO

    @GET("/music/userId/{userId}")
    suspend fun getMusicListByUserId(@Path("userId") userId: String): ArrayList<ServerMusicDTO>

    @DELETE("/music/{id}")
    suspend fun deleteMusic(@Path("id") musicId: String)

    @PUT("/music/")
    suspend fun updateMusic(@Body serverMusicDTO: ServerMusicDTO)
    /**
     * 서버에 원하는 파일을 형식에 맞게 보내기 위한 작업 필요
     *
     */
    @Multipart
    @POST("/music/upload/{userId}/{genre}")
    suspend fun uploadMusic(@Path("userId") userId: String
                    , @Path("genre") genre: String
                    , @Part file: MultipartBody.Part) : String

}