package linbactsim.model;

// Source: SURE.Pixel
// DONE
public class Pixel {

    private boolean isWall;
    private int count;
    private int row, col;
    private boolean isExit;

    public Pixel(int row, int col) {
        this.row = row;
        this.col = col;
        this.isWall = false;
        this.count = 0;
        this.isExit = false;
    }

    public boolean isWall() {return isWall;}

    public void setWall(boolean wall) {this.isWall = wall;}

    public boolean isExit() {return isExit;}

    public void setExit(boolean exit) {this.isExit = exit;}

    public int getCount() {return count;}

    public void addCount() {this.count++;}

    public int getRow() {return row;}

    public int getCol() {return col;}

    public void resetCount() {this.count = 0;}
}
