package fi.decentri.dataapi.routes

import io.ktor.server.application.*

data class JsonbFilter(val key: String, val value: String)

fun ApplicationCall.parseJsonbFilters(): List<JsonbFilter> =
    request.queryParameters.getAll("filter")
        ?.mapNotNull { token ->
            token.split(":", limit = 2)
                .takeIf { it.size == 2 }
                ?.let { (k, v) -> JsonbFilter(k, v) }
        }
        ?: emptyList()