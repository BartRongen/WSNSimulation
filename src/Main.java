import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Main {

    long sL = 1000/(12*16);

    public static void main(String[] args) {

        Main main = new Main();
        main.run();
    }

    public void run(){
        Instant start = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant lap = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant second = Instant.now().truncatedTo(ChronoUnit.MICROS);
        int frame = 0;
        int slot = 0;
        int totalSlots = 0;
        int GTS = 0;
        int slotTime = 1;

        Instant.now().truncatedTo(ChronoUnit.MICROS);

        while(true){
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            if (second.compareTo(now.minusMillis(1000)) < 0){
                System.out.println("1 second");
                second = now;
            }
            if (start.compareTo(now.minusMillis(slotTime*sL)) < 0){
                totalSlots++;
                slot++;
                slotTime++;
                if (slot>15){
                    frame++;
                    slot = 0;
                    if (frame>11){
                        frame = 0;
                        System.out.println("totalsSlots: " + totalSlots + ", GTS: " + GTS);
                        totalSlots = 0;
                        GTS = 0;
                    }
                } else if (slot>8){
                    GTS++;
                }
                lap = now;
            }
        }
    }
}
