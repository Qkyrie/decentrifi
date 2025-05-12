package fi.decentri.dataapi.routes

import fi.decentri.dataapi.waitlist.EmailRequest
import fi.decentri.dataapi.waitlist.WaitlistRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("fi.decentri.dataapi.routes.WaitlistRoutes")

fun Route.waitlistRoutes(
    waitlistRepository: WaitlistRepository
) {
    post("/waitlist") {
        try {
            val emailRequest = call.receive<EmailRequest>() // Receive the JSON payload
            logger.info("Received email for waitlist: ${emailRequest.email}")

            // Save email to database
            val id = waitlistRepository.insert(emailRequest.email)
            logger.info("Saved email to waitlist with ID: $id")

            call.respond(HttpStatusCode.OK, mapOf("message" to "Email received")) // Send success response
        } catch (e: Exception) {
            logger.error("Failed to process waitlist request", e)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request"))
        }
    }
}