# TravelFusion TestDome — Practice Test Cases

One folder per question, each containing testN.in / testN.out pairs.

## How to use

Your program reads from stdin and writes to stdout.

Run a single test:
  java -cp out Q1 < q1/test1.in

Diff against expected:
  java -cp out Q1 < q1/test1.in | diff - q1/test1.out

No output = pass. Any output = the diff between yours and expected.

Run all tests for one question:
  ./check.sh q1

Run everything:
  ./check.sh

---

## Format per question

### Q1 — Closest Pair Sum
INPUT  line 1: space-separated prices
       line 2: target integer
OUTPUT the two prices (ascending) whose sum is closest to target

  100 200 300 450 500
  400
  --> 100 300

Skeleton main():
  int[] prices = Arrays.stream(sc.nextLine().trim().split("\\s+")).mapToInt(Integer::parseInt).toArray();
  int target = Integer.parseInt(sc.nextLine().trim());
  // call closestPair(), sort result, print: result[0] + " " + result[1]

---

### Q2 — Balanced HTML Tags
INPUT  one HTML string (may be empty)
OUTPUT true or false

  <div><p></p></div>  -->  true
  <div><p></div></p>  -->  false

Skeleton main():
  String html = sc.hasNextLine() ? sc.nextLine() : "";
  System.out.println(isBalanced(html));

---

### Q3 — Binary Search
INPUT  line 1: sorted space-separated integers
       line 2: target
OUTPUT 0-based index, or -1 if not found

  1 3 5 7 9 11 13 15 17 19
  11
  --> 5

Skeleton main():
  int[] arr = Arrays.stream(sc.nextLine().trim().split("\\s+")).mapToInt(Integer::parseInt).toArray();
  int target = Integer.parseInt(sc.nextLine().trim());
  System.out.println(binarySearch(arr, target));

---

### Q4 — Cycle Detection
INPUT  line 1: space-separated node values
       line 2 (optional): "cycle N" — last node points back to node at index N
OUTPUT true or false

  1 2 3          -->  false
  1 2 3          -->  true
  cycle 1

Skeleton main():
  int[] vals = Arrays.stream(sc.nextLine().trim().split("\\s+")).mapToInt(Integer::parseInt).toArray();
  ListNode[] nodes = new ListNode[vals.length];
  for (int i = 0; i < vals.length; i++) nodes[i] = new ListNode(vals[i]);
  for (int i = 0; i < vals.length - 1; i++) nodes[i].next = nodes[i+1];
  if (sc.hasNextLine()) {
      String line = sc.nextLine().trim();
      if (line.startsWith("cycle")) {
          int idx = Integer.parseInt(line.split("\\s+")[1]);
          nodes[vals.length - 1].next = nodes[idx];
      }
  }
  System.out.println(hasCycle(nodes[0]));

---

### Q5 — Bookable Interface
INPUT  "<type> <id> <passengerId>"   (type = flight or hotel)
OUTPUT your booking reference string

  flight BA200 P001  -->  any string containing "BA200" and "P001"

NOTE: check.sh uses substring matching for Q5, not exact diff.
      The format of the reference is your design choice.

Skeleton main():
  String[] parts = sc.nextLine().trim().split("\\s+");
  Bookable b = parts[0].equals("flight") ? new Flight(parts[1]) : new Hotel(parts[1]);
  System.out.println(b.book(parts[2]));

---

### Q6 — Factory Pattern
INPUT  type string
OUTPUT simple class name of the created object, or ERROR for unknown type

  flight      -->  Flight
  submarine   -->  ERROR

Skeleton main():
  String type = sc.nextLine().trim();
  try { System.out.println(create(type).getClass().getSimpleName()); }
  catch (IllegalArgumentException e) { System.out.println("ERROR"); }

---

### Q7 — Composition / LoyaltyScheme
INPUT  line 1: "basic" or "premium"
       line 2: price (integer)
OUTPUT points earned (integer)
       basic = 1pt per £1,  premium = 2.5pts per £1 (truncated)

  premium     -->  250
  100

Skeleton main():
  String scheme = sc.nextLine().trim();
  double price = Double.parseDouble(sc.nextLine().trim());
  LoyaltyScheme s = scheme.equals("premium") ? new PremiumLoyalty() : new BasicLoyalty();
  System.out.println(new FlightWithLoyalty(s).pointsFor(price));

---

### Q8 — Thread-Safe Singleton
INPUT  one or more lines of "<amount> <rate>"
OUTPUT one result per line, 2 decimal places

  100 1.27    -->  127.00
  200 0.85         170.00

Skeleton main():
  while (sc.hasNextLine()) {
      String[] p = sc.nextLine().trim().split("\\s+");
      System.out.printf("%.2f%n", CurrencyConverter.getInstance()
                                      .convert(Double.parseDouble(p[0]),
                                               Double.parseDouble(p[1])));
  }
