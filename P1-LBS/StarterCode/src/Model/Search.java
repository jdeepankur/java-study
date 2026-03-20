package Model;

public class Search {
    public status attempt = status.UNSOLVED;
    public String moves = "";
}

enum status {
    SOLVED,
    UNSOLVED,
    TIMEOUT;
}
