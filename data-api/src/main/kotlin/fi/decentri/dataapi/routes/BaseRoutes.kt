package fi.decentri.dataapi.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*


fun Route.baseRoutes() {
    // Static resources
    staticResources("/images", "static/images")

    // Health check endpoint
    get("/health") {
        call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
    }

    // Landing page
    get("/") {
        call.respond(ThymeleafContent("analytics-landing.html", mapOf("title" to "Data Ingestion Service")))
    }
}