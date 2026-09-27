package eu.kanade.tachiyomi.source.online.all.mangadex.dto

import eu.kanade.tachiyomi.source.online.all.mangadex.MDConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(MDConstants.USER)
data class UserDto(override val attributes: UserAttributes? = null) : EntityDto()

@Serializable
data class UserAttributes(val username: String) : AttributesDto()
