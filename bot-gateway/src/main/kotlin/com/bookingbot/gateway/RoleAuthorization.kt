package com.bookingbot.gateway

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** DSL helper: `authorize(Role.ADMIN) { … }` */
fun Route.authorize(vararg roles: Role, build: Route.() -> Unit): Route =
    authenticate("auth-jwt", "auth-basic") {
        route("") {
            intercept(ApplicationCallPipeline.Plugins) {
                val principal = call.principal<UserPrincipal>()
                    ?: return@intercept call.respond(AuthFailure)
                if (principal.role !in roles) return@intercept call.respond(AuthForbidden)
            }
            build()
        }
    }

private suspend fun AuthContext.respond(response: AuthResponse) =
    when (response) {
        AuthFailure   -> call.respond(HttpStatusCode.Unauthorized)
        AuthForbidden -> call.respond(HttpStatusCode.Forbidden)
    }

private sealed interface AuthResponse
private object AuthFailure : AuthResponse
private object AuthForbidden : AuthResponse
