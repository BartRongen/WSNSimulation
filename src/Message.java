import java.time.Instant;

//class to represent a message sent by the nodes to the basestation
public class Message {

    //the message contains the node id, and its sequence number
    public int node;
    public int seqNumber;
    public Instant timeStamp;

    public Message(int node, int seqNumber){
        this.node = node;
        this.seqNumber = seqNumber;
        this.timeStamp = Instant.now();
    }
}
