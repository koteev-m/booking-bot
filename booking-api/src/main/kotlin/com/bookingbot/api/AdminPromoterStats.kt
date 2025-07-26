package com.bookingbot.api

import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.auth.jwt.JWTPrincipal
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.panel.Panel
import io.ktor.panel.resources
import io.ktor.panel.register
import java.math.BigDecimal
import kotlinx.html.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Admin panel section for promoter statistics.
 */
object PromoterStatsTable : IntIdTable("promoter_stats") {
    val promoterId = long("promoter_id").uniqueIndex()
    val visits = integer("visits").default(0)
    val totalDeposit = decimal("total_deposit", 10, 2).default(BigDecimal.ZERO)
}

/** DAO for [PromoterStatsTable]. */
class PromoterStats(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PromoterStats>(PromoterStatsTable) {
        /**
         * Increases visit count and deposit for a promoter.
         */
        fun increase(promoterId: Long?, deposit: BigDecimal) {
            if (promoterId == null) return
            transaction {
                val stat = find { PromoterStatsTable.promoterId eq promoterId }
                    .singleOrNull() ?: new {
                        this.promoterId = promoterId
                        visits = 0
                        totalDeposit = BigDecimal.ZERO
                    }
                stat.visits = stat.visits + 1
                stat.totalDeposit = stat.totalDeposit + deposit
            }
        }
    }

    var promoterId by PromoterStatsTable.promoterId
    var visits by PromoterStatsTable.visits
    var totalDeposit by PromoterStatsTable.totalDeposit
}

/** Install admin panel section for promoter stats. */
fun Application.configurePromoterStatsPanel(myAuth: AuthenticationProvider) {
    install(Panel) { authProvider = myAuth }

    resources {
        register<PromoterStats>("Promoter Stats") {
            dao = PromoterStats.Companion
            listFields("promoterId", "visits", "totalDeposit")
            formFields()
            readPermission { hasRole(ADMIN) || (hasRole(PROMOTER) && obj.promoterId == userId) }
            writePermission { hasRole(ADMIN) }
            menuGroup = "Analytics"
        }
    }

    routing {
        route("/my_stats") {
            authenticate("jwt") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val id = principal!!.payload.getClaim("id").asLong()
                    call.respondRedirect("/panel/promoter-stats?filter=promoterId:$id")
                }
            }
        }
    }
}

/* SQL to create table if absent:
CREATE TABLE IF NOT EXISTS promoter_stats (
    id SERIAL PRIMARY KEY,
    promoter_id BIGINT NOT NULL UNIQUE,
    visits INT NOT NULL DEFAULT 0,
    total_deposit DECIMAL(10,2) NOT NULL DEFAULT 0.00
);
*/
