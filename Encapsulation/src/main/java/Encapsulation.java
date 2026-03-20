import java.text.DecimalFormat;

public class Encapsulation {
    public static void main(String[] args) {
        Person p = new Person();
        System.out.println("Person created: " + p);
    }
}

class Person {
    private String name = "";
    private int age = 0;
    private String country = "";

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return this.age;
    }
    public void setAge(int age) {
        this.age = age;
    }

    public String getCountry() {
        return this.country;
    }
    public void setCountry(String country) {
        this.country = country;
    } 
}

class BankAccount {
    private long accountNumber;
    private int balance;

    public long getAccountNumber() {
        return this.accountNumber;
    }
    public void setAccountNumber(long accountNumber) {
        if (accountNumber > 1e7) {
            this.accountNumber = accountNumber;
        }
        else {
            throw new RuntimeException("Account Number is usually 8 digits.");
        }
    }

    public float getBalance() {
        return (float) this.balance / 100;
    }
    public void setBalance(float balance) {
        if (balance >= 0) {
            this.balance = (int) (balance * 100);
        }
        else {
            throw new RuntimeException("This bank does not allow overdrafts!");
        }
    } 
}

class Rectangle {
    private int length = 0;
    private int width = 0;

    public int getLength() {
        return this.length;
    }
    public void setLength(int length) {
        if (length >= 0) {
            this.length = length;
        }
        else {
            throw new IllegalArgumentException("Length must be a positive integer.");
        }
    }

    public int getWidth() {
        return this.width;
    }
    public void setWidth(int width) {
        if (width >= 0) {
            this.width = width;
        }
        else {
            throw new IllegalArgumentException("Width must be a positive integer.");
        }
    }

    public int getArea() {
        return length * width;
    }
}

class Employee {
    private long employee_id;
    private String employee_name;
    private int employee_salary;

    public long getID() {
        return this.employee_id;
    }
    public void setID(long ID) {
        this.employee_id = ID;
    }

    public String getName() {
        return this.employee_name;
    }
    public void setName(String name) {
        this.employee_name = name;
    }

    public String getSalary() {
        // System.out.println("DEBUG salary before format = " + this.employee_salary);
        DecimalFormat df = new DecimalFormat("#,##0");
        int pounds = this.employee_salary / 100;
        int pence = this.employee_salary % 100;
        String salary_str = "£" + df.format(pounds) + "." + String.format("%02d", pence);
        return salary_str;
    }

    public Employee(String salary) {
        salary = salary.replaceAll(",", "");
        if (!salary.matches("£\\d+\\.\\d{2}")) {
            throw new IllegalArgumentException("Salary must be in the format '£XX,XXX.XX'");
        }
        String raw_salary = "";
        if (salary.contains(".")) {
            String[] parts = salary.split("\\.");
            raw_salary = parts[0].substring(1) + parts[1];
        }
        else {
            raw_salary = salary.substring(1) + "00";
        }
        this.employee_salary = Integer.parseInt(raw_salary);
    }
}