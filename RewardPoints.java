import java.util.HashMap;

public class RewardPoints {
    private HashMap<String, Integer> customers;

    public RewardPoints() {
        this.customers = new HashMap<String, Integer>();
    }

    public void earnPoints(String customerName, int points) {
        if (!this.customers.keySet().contains(customerName)) {
            this.customers.put(customerName, Math.max(0, points));
        }

        if ((this.customers.get(customerName) == 0) && points >= 1){
            points += 500;
        }

        this.customers.put(customerName, this.customers.get(customerName) + Math.max(0, points));
    }

    public int spendPoints(String customerName, int points) {
        if (points <= this.customers.get(customerName)){
            this.customers.put(customerName, this.customers.get(customerName) - points);
        }
        
        if (!this.customers.keySet().contains(customerName)) {
            return 0;
        } else {
            return this.customers.get(customerName);
        }
    }

    public static void main(String[] args) {
        RewardPoints rewardPoints = new RewardPoints();
        rewardPoints.earnPoints("John", 520);
        System.out.println(rewardPoints.spendPoints("John", 840));
    }
}