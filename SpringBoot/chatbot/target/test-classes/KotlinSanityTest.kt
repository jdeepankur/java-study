import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class KotlinSanityTest {

    @Test
    fun kotlinCompilesAndRuns() {
        val message = "Kotlin works"
        assertEquals("Kotlin works", message)
    }

    // @Test
    // fun kotlinCanAccessJavaClass() {
    //     val db = com.testapp.model.Database
    //     assertNotNull(db.allTables())
    // }
}
