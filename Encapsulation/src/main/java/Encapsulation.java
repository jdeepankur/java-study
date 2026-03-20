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
}
