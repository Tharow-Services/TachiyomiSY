package eu.kanade.tachiyomi.source.online.all.mangadex.dto

import eu.kanade.tachiyomi.source.online.all.mangadex.MDConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(MDConstants.SCANLATION_GROUP)
data class ScanlationGroupDto(override val attributes: ScanlationGroupAttributes? = null) : EntityDto()

@Serializable
data class ScanlationGroupAttributes(val name: String) : AttributesDto()
