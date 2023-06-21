package dev.tmsoft.lib.query

import dev.tmsoft.lib.Config
import dev.tmsoft.lib.exposed.testDatabase
import dev.tmsoft.lib.query.paging.PagingParameters
import dev.tmsoft.lib.query.paging.SortingParameter
import dev.tmsoft.lib.query.paging.sortingParameters
import dev.tmsoft.lib.query.paging.toContinuousList
import io.ktor.http.Parameters
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ContinuousListTest {
    @Test
    fun `test sorting`() {
        val database = Database.connect(
            Config.h2DatabaseUrl,
            Config.h2Driver,
            Config.h2User,
            Config.h2Password
        )

        transaction(database) {
            SchemaUtils.create(UserTable)
            for (i in 1..5) {
                UserTable.insert {
                    it[name] = "test"
                    it[number] = i
                    it[modifyAt] = LocalDate.now()
                }
            }

            val users = runBlocking {
                UserTable.selectAll()
                    .toContinuousList(
                        PagingParameters(30, 1,""),
                        ResultRow::toUser,
                        listOf(
                            SortingParameter("number", SortOrder.DESC),
                            SortingParameter("name", SortOrder.ASC)
                        )
                    )

            }
            assertTrue(
                users.data.first().order == 5 &&
                        users.data.last().order == 1
            )


            Assertions.assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    UserTable.selectAll()
                        .toContinuousList(
                            PagingParameters(30, 1,""),
                            ResultRow::toUser,
                            listOf(
                                SortingParameter(
                                    "modifyAt",
                                    SortOrder.DESC
                                )
                            )
                        )

                }
            }

            val parameters = object : Parameters {
                override val caseInsensitiveName: Boolean = false
                override fun entries(): Set<Map.Entry<String, List<String>>> {
                    return mapOf("sorting[name]" to listOf("asc")).entries
                }

                override fun getAll(name: String): List<String> = emptyList()
                override fun isEmpty(): Boolean = false
                override fun names(): Set<String> = emptySet()
            }.sortingParameters().first()

            assertTrue {
                parameters.name == "name" &&
                        parameters.sortOrder == SortOrder.ASC
            }
        }
    }

    @Test
    fun `test count`() {
        val database = Database.connect(
            Config.h2DatabaseUrl,
            Config.h2Driver,
            Config.h2User,
            Config.h2Password
        )

        transaction(database) {
            SchemaUtils.create(UserTable)
            val count = 60
            for (i in 1..count) {
                UserTable.insert {
                    it[name] = "test"
                    it[number] = i
                    it[modifyAt] = LocalDate.now()
                }
            }

            val users = runBlocking {
                UserTable.selectAll()
                    .toContinuousList(
                        PagingParameters(30, 1,""),
                        ResultRow::toUser,
                        emptyList(),
                        true
                    )

            }
            assertEquals(users.count, count.toLong())
        }
    }

    @Test
    fun `postgres uniq count count pagination with join`() {
        transaction(testDatabase) {
            SchemaUtils.create(UserTable)
            SchemaUtils.create(AddressTable)
            val count = 3
            for (i in 1..count) {
                val user = UserTable.insertAndGetId {
                    it[name] = "test_$i"
                    it[number] = i
                    it[modifyAt] = LocalDate.now()
                }
                for (j in 1..5) {
                    AddressTable.insert {
                        it[address] = "address_$j"
                        it[this.user] = user.value
                        it[sequence] = i * j
                    }
                }
            }

            val users = runBlocking {
                UserTable
                    .join(AddressTable, JoinType.LEFT, AddressTable.user, UserTable.id)
                    .selectAll()
                    .toContinuousList(
                        PagingParameters(2, 1,""),
                        ResultRow::toUser,
                        listOf(
                            SortingParameter(
                                "sequence",
                                SortOrder.DESC
                            )
                        ),
                        true
                    )

            }
            assertEquals(count.toLong(), users.count)
        }
    }
}

object UserTable : IntIdTable() {
    val name = varchar("name", 255)
    val number = integer("number")
    val modifyAt = date("modify_at").default(LocalDate.now())
}

object AddressTable : IntIdTable() {
    val user = integer("user_id")
    val address = varchar("address", 255)
    val sequence = integer("sequence")
}

data class User(
    val name: String,
    val order: Int
)

fun ResultRow.toUser() = User(
    this[UserTable.name],
    this[UserTable.number]
)
