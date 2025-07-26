package com.bookingbot.gateway

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

enum class Role { ADMIN, USER }

data class UserPrincipal(val id: Long, val role: Role) : Principal

fun Application.configureAuth() {

    val jwtSecret = environment.config.property("jwt.secret").getString()
    val basicUser  = environment.config.property("basic.user").getString()
    val basicPass  = environment.config.property("basic.pass").getString()

    install(Authentication) {

        jwt("auth-jwt") {
            realm = "bookingbot"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer("bookingbot")
                    .build()
            )
            validate { cred ->
                val id = cred.payload.getClaim("sub").asLong()
                val role = cred.payload.getClaim("role").asString()?.let { Role.valueOf(it) }
                if (id != null && role != null) UserPrincipal(id, role) else null
            }
        }

        basic("auth-basic") {
            realm = "bookingbot-basic"
            validate { creds ->
                if (creds.name == basicUser && creds.password == basicPass)
                    UserPrincipal(id = 0, role = Role.ADMIN)
                else null
            }
        }
    }
}
