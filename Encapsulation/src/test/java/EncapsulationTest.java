import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    public void testNameGetterAndSetter() {
        Person person = new Person();
        String expectedName = "John Doe";
        person.setName(expectedName);
        assertEquals(expectedName, person.getName());
    }

    @Test
    public void testAgeGetterAndSetter() {
        Person person = new Person();
        int expectedAge = 25;
        person.setAge(expectedAge);
        assertEquals(expectedAge, person.getAge());
    }

    @Test
    public void testCountryGetterAndSetter() {
        Person person = new Person();
        String expectedCountry = "USA";
        person.setCountry(expectedCountry);
        assertEquals(expectedCountry, person.getCountry());
    }

    @Test
    public void testAllBankAccountFieldsArePrivate() {
        Field[] fields = BankAccount.class.getDeclaredFields();
        for (Field field : fields) {
            assertTrue(Modifier.isPrivate(field.getModifiers()),
                    "Field '" + field.getName() + "' should be private");
        }
    }

    @Test
    public void testSetAccountNumberWithValidValue() {
        BankAccount account = new BankAccount();
        long validAccountNumber = 12345678L;
        account.setAccountNumber(validAccountNumber);
        assertEquals(validAccountNumber, account.getAccountNumber());
    }

    @Test
    public void testSetAccountNumberWithLargeValidValue() {
        BankAccount account = new BankAccount();
        long largeAccountNumber = 99999999L;
        account.setAccountNumber(largeAccountNumber);
        assertEquals(largeAccountNumber, account.getAccountNumber());
    }

    @Test
    public void testSetAccountNumberWithMinimumBoundaryValue() {
        BankAccount account = new BankAccount();
        long boundaryValue = (long)(1e7 + 1);
        account.setAccountNumber(boundaryValue);
        assertEquals(boundaryValue, account.getAccountNumber());
    }

    @Test
    public void testSetAccountNumberWithInvalidValue() {
        BankAccount account = new BankAccount();
        long invalidAccountNumber = 1234567L;
        assertThrows(RuntimeException.class, () -> {
            account.setAccountNumber(invalidAccountNumber);
        });
    }

    @Test
    public void testSetAccountNumberWithZero() {
        BankAccount account = new BankAccount();
        assertThrows(RuntimeException.class, () -> {
            account.setAccountNumber(0L);
        });
    }

    @Test
    public void testSetAccountNumberWithNegativeValue() {
        BankAccount account = new BankAccount();
        assertThrows(RuntimeException.class, () -> {
            account.setAccountNumber(-12345678L);
        });
    }

    @Test
    public void testGetAccountNumber() {
        BankAccount account = new BankAccount();
        long expectedValue = 98765432L;
        account.setAccountNumber(expectedValue);
        assertEquals(expectedValue, account.getAccountNumber());
    }

    @Test
    public void testSetBalanceWithValidValue() {
        BankAccount account = new BankAccount();
        float validBalance = 5000.50f;
        account.setBalance(validBalance);
        assertEquals(validBalance, account.getBalance());
    }

    @Test
    public void testSetBalanceWithZero() {
        BankAccount account = new BankAccount();
        account.setBalance(0.00f);
        assertEquals(0.00f, account.getBalance());
    }

    @Test
    public void testSetBalanceWithLargeValue() {
        BankAccount account = new BankAccount();
        float largeBalance = 99999.99f;
        account.setBalance(largeBalance);
        assertEquals(largeBalance, account.getBalance());
    }

    @Test
    public void testSetBalanceWithCents() {
        BankAccount account = new BankAccount();
        float balanceWithCents = 100.50f;
        account.setBalance(balanceWithCents);
        assertEquals(balanceWithCents, account.getBalance());
    }

    @Test
    public void testSetBalanceWithSingleCent() {
        BankAccount account = new BankAccount();
        float singleCent = 0.01f;
        account.setBalance(singleCent);
        assertEquals(singleCent, account.getBalance());
    }

    @Test
    public void testSetBalanceWithNegativeValue() {
        BankAccount account = new BankAccount();
        assertThrows(RuntimeException.class, () -> {
            account.setBalance(-100.50f);
        });
    }

    @Test
    public void testGetBalance() {
        BankAccount account = new BankAccount();
        float expectedBalance = 2500.75f;
        account.setBalance(expectedBalance);
        assertEquals(expectedBalance, account.getBalance());
    }

    @Test
    public void testBalanceFieldIsPrivate() {
        Field[] fields = BankAccount.class.getDeclaredFields();
        boolean balanceIsPrivate = false;
        for (Field field : fields) {
            if (field.getName().equals("balance")) {
                balanceIsPrivate = Modifier.isPrivate(field.getModifiers());
                break;
            }
        }
        assertTrue(balanceIsPrivate, "Balance field should be private");
    }

    @Test
    public void testRectangleGettersAndSetters() {
        Rectangle rect = new Rectangle();
        rect.setLength(7);
        rect.setWidth(4);
        assertEquals(7, rect.getLength());
        assertEquals(4, rect.getWidth());
    }

    @Test
    public void testRectangleAreaCalculation() {
        Rectangle rect = new Rectangle();
        rect.setLength(5);
        rect.setWidth(3);
        assertEquals(15, rect.getArea());
    }

    @Test
    public void testRectangleZeroDimensions() {
        Rectangle rect = new Rectangle();
        rect.setLength(0);
        rect.setWidth(0);
        assertEquals(0, rect.getArea());
    }

    @Test
    public void testRectangleNegativeLengthThrows() {
        Rectangle rect = new Rectangle();
        assertThrows(IllegalArgumentException.class, () -> rect.setLength(-1));
    }

    @Test
    public void testRectangleNegativeWidthThrows() {
        Rectangle rect = new Rectangle();
        assertThrows(IllegalArgumentException.class, () -> rect.setWidth(-1));
    }

    @Test
    public void testAllEmployeeFieldsArePrivate() {
        Field[] fields = Employee.class.getDeclaredFields();
        for (Field field : fields) {
            assertTrue(Modifier.isPrivate(field.getModifiers()),
                    "Field '" + field.getName() + "' should be private");
        }
    }

    @Test
    public void testEmployeeIDGetterAndSetter() {
        Employee employee = new Employee("£50,000.00");
        long employeeID = 12345L;
        employee.setID(employeeID);
        assertEquals(employeeID, employee.getID());
    }

    @Test
    public void testEmployeeIDWithLargeValue() {
        Employee employee = new Employee("£75,000.00");
        long largeID = 9999999999L;
        employee.setID(largeID);
        assertEquals(largeID, employee.getID());
    }

    @Test
    public void testEmployeeIDWithZero() {
        Employee employee = new Employee("£60,000.00");
        employee.setID(0L);
        assertEquals(0L, employee.getID());
    }

    @Test
    public void testEmployeeNameGetterAndSetter() {
        Employee employee = new Employee("£50,000.00");
        String name = "John Smith";
        employee.setName(name);
        assertEquals(name, employee.getName());
    }

    @Test
    public void testEmployeeNameWithEmptyString() {
        Employee employee = new Employee("£50,000.00");
        employee.setName("");
        assertEquals("", employee.getName());
    }

    @Test
    public void testEmployeeNameWithSpecialCharacters() {
        Employee employee = new Employee("£50,000.00");
        String name = "Jean-Pierre O'Brien";
        employee.setName(name);
        assertEquals(name, employee.getName());
    }

    @Test
    public void testEmployeeSalaryOnInitialization() {
        Employee employee = new Employee("£50,000.00");
        assertEquals("£50,000.00", employee.getSalary());
    }

    @Test
    public void testEmployeeSalaryWithZero() {
        Employee employee = new Employee("£0.00");
        assertEquals("£0.00", employee.getSalary());
    }

    @Test
    public void testEmployeeSalaryWithLargeValue() {
        Employee employee = new Employee("£999,999.00");
        assertEquals("£999,999.00", employee.getSalary());
    }

    @Test
    public void testEmployeeIDNegativeValue() {
        Employee employee = new Employee("£50,000.00");
        long negativeID = -12345L;
        employee.setID(negativeID);
        assertEquals(negativeID, employee.getID());
    }

    @Test
    public void testEmployeeSalaryThousandSeparator() {
        Employee employee = new Employee("£125,750.00");
        assertEquals("£125,750.00", employee.getSalary());
    }

    @Test
    public void testEmployeeSalarySmallAmount() {
        Employee employee = new Employee("£500.00");
        assertEquals("£500.00", employee.getSalary());
    }

    @Test
    public void testEmployeeSalaryImproperlyFormattedThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Employee("50000");
        });
    }
}
