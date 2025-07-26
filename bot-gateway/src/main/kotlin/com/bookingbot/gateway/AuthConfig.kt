package com.bookingbot.gateway

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "bookingbot"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer("bookingbot")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("sub").asString().isNullOrBlank()) null
                else JWTPrincipal(credential.payload)
            }
        }

        basic("auth-basic") {
            realm = "bookingbot-basic"
            validate { creds ->
                val user = environment.config.property("basic.user").getString()
                val pass = environment.config.property("basic.pass").getString()
                if (creds.name == user && creds.password == pass) UserIdPrincipal(creds.name) else null
            }
        }
    }
}
