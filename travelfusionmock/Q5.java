import java.util.*;

class Q5 {
    private class Interfaces {
        interface Bookable {
            String getName();
            boolean checkBooking(String bookingID);
            void makeBooking(String bookingID);
            int getCapacity();
        }

        static class Hotel implements Bookable {
            private List<String> bookings = new ArrayList<String>();
            private String name;
            private int rooms;

            public String getName() {
                return name;
            }
            public void setName(String name) {
                this.name = name;
            }

            public boolean checkBooking(String bookingID) {
                return bookings.contains(bookingID);
            }
            public void makeBooking(String bookingID) {
                if (bookings.size() < rooms) {
                    bookings.add(bookingID);
                }
            }

            public Hotel(int rooms) {
                this.rooms = rooms;
            }
            public int getCapacity() {
                return rooms;
            }
            
        }

        static class Flight implements Bookable{

            public String getName() {
                return this.name;
            }


            public boolean checkBooking(String bookingID) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'checkBooking'");
            }


            public void makeBooking(String bookingID) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'makeBooking'");
            }


            public int getCapacity() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getCapacity'");
            }
        }

    }

    public static void main(String[] args) {
        var in = new Scanner(System.in);

        String type = in.next();
        Interfaces.Bookable service;

        switch(type) {
            case ("flight"):
                service = new Interfaces.Flight();
                break;
            case ("hotel"):
                service = new Interfaces.Hotel(10);
                break;
            default:
                throw new IllegalArgumentException("Unknown service type: " + type);
        }

    }
}