public class PriceUpdate {
    public int destination;
    public int seqNumber;
    public long createdAt;
    public long recievedAt = -1;

    public PriceUpdate(int destination, int seqNumber, long time){
        this.destination = destination;
        this.seqNumber = seqNumber;
        this.createdAt = time;
    }
}
