

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

public class Rectangle {
    private int length = 0;
    private int width = 0;

    public int getLength() {
        return this.length;
    }
    public void setLength(int length) {
        if (length > 0) {
            this.length = length;
        }
        else {
            throw new RuntimeException("Length must be a positive integer.");
        }
    }

    public int getLength() {
        return this.length;
    }
    public void setLength(int length) {
        if (length > 0) {
            this.length = length;
        }
        else {
            throw new RuntimeException("Length must be a positive integer.");
        }
    }

    public getArea() {}
}