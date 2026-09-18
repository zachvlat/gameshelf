package com.zachvlat.gamelibrary.library.store.igdb

import kotlinx.serialization.Serializable

@Serializable
data class IgdbTokenResponse(
    val access_token: String,
    val expires_in: Long,
    val token_type: String = ""
)

@Serializable
data class IgdbGame(
    val id: Long = 0,
    val name: String? = null,
    val summary: String? = null,
    val first_release_date: Long? = null,
    val cover: IgdbCover? = null,
    val involved_companies: List<IgdbInvolvedCompany>? = null,
    val genres: List<IgdbGenre>? = null
)

@Serializable
data class IgdbCover(
    val url: String? = null
)

@Serializable
data class IgdbInvolvedCompany(
    val developer: Boolean = false,
    val company: IgdbCompany? = null
)

@Serializable
data class IgdbCompany(
    val name: String? = null
)

@Serializable
data class IgdbGenre(
    val name: String? = null
)