import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncapsulationTest {
    @Test
    public void testAllPersonFieldsArePrivate() {
        Field[] fields = Person.class.getDeclaredFields();
        for (Field field : fields) {
            assertTrue(Modifier.isPrivate(field.getModifiers()),
                    "Field '" + field.getName() + "' should be private");
        }
    }
}
