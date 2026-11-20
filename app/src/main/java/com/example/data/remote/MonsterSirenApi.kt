package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface MonsterSirenApi {

    @GET("albums")
    suspend fun getAlbums(): ApiResponse<List<AlbumSummaryDto>>

    @GET("album/{cid}/detail")
    suspend fun getAlbumDetail(@Path("cid") cid: String): ApiResponse<AlbumDetailDto>

    @GET("songs")
    suspend fun getSongs(): ApiResponse<SongListWrapperDto>

    @GET("song/{cid}")
    suspend fun getSongDetail(@Path("cid") cid: String): ApiResponse<SongDetailDto>

    @GET("news")
    suspend fun getNews(): ApiResponse<NewsListWrapperDto>

    @GET("news/{cid}")
    suspend fun getNewsDetail(@Path("cid") cid: String): ApiResponse<NewsDetailDto>
}
