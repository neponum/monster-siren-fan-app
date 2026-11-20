package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "code") val code: Int = -1,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class AlbumSummaryDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "name") val name: String,
    @Json(name = "coverUrl") val coverUrl: String? = null,
    @Json(name = "artistes") val artistes: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDetailDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "name") val name: String,
    @Json(name = "intro") val intro: String? = null,
    @Json(name = "belong") val belong: String? = null,
    @Json(name = "coverUrl") val coverUrl: String? = null,
    @Json(name = "coverDeUrl") val coverDeUrl: String? = null,
    @Json(name = "songs") val songs: List<AlbumSongRefDto>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumSongRefDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "name") val name: String,
    @Json(name = "artistes") val artistes: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SongListWrapperDto(
    @Json(name = "list") val list: List<SongSummaryDto>? = null,
    @Json(name = "autoplay") val autoplay: String? = null
)

@JsonClass(generateAdapter = true)
data class SongSummaryDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "name") val name: String,
    @Json(name = "albumCid") val albumCid: String,
    @Json(name = "artists") val artists: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SongDetailDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "name") val name: String,
    @Json(name = "albumCid") val albumCid: String,
    @Json(name = "sourceUrl") val sourceUrl: String? = null,
    @Json(name = "lyricUrl") val lyricUrl: String? = null,
    @Json(name = "mvUrl") val mvUrl: String? = null,
    @Json(name = "mvCoverUrl") val mvCoverUrl: String? = null,
    @Json(name = "artists") val artists: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class NewsListWrapperDto(
    @Json(name = "list") val list: List<NewsSummaryDto>? = null,
    @Json(name = "end") val end: Boolean = true
)

@JsonClass(generateAdapter = true)
data class NewsSummaryDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "title") val title: String,
    @Json(name = "cate") val cate: Int = 0,
    @Json(name = "date") val date: String = ""
)

@JsonClass(generateAdapter = true)
data class NewsDetailDto(
    @Json(name = "cid") val cid: String,
    @Json(name = "title") val title: String,
    @Json(name = "cate") val cate: Int = 0,
    @Json(name = "author") val author: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "date") val date: String = ""
)
