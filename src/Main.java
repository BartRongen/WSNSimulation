import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        if (!Config.generateData){

            //Runs the simulation once without storing data in .csv file
            WSN wsn = new WSN();
            wsn.startSimulation();
        } else {

            //Runs the simulation multiple times and storing the data in a .csv file
            WSN[] wsns = new WSN[Config.numberOfSimulations];
            for (int i=0; i<wsns.length; i++){
                wsns[i] = new WSN();
            }
            //Runs the simulations
            for (int i=0; i<wsns.length; i++){
                System.out.println("Starting simulation " + (i+1) + "/" + Config.numberOfSimulations);
                wsns[i].startSimulation();
            }
            //Stores the accumulated data from all simulations in one hashmap
            HashMap<String, ArrayList<String>> tempData = new HashMap<>();
            for (WSN wsn : wsns){
                for (Map.Entry<String, String> entry : wsn.getData().entrySet()){
                    if (!tempData.containsKey(entry.getKey())){
                        ArrayList<String> values = new ArrayList<>();
                        values.add(entry.getValue());
                        tempData.put(entry.getKey(), values);
                    } else {
                        ArrayList<String> values = tempData.get(entry.getKey());
                        values.add(entry.getValue());
                        tempData.replace(entry.getKey(), values);
                    }
                }
            }
            //Creates data lines for in .csv file
            List<String[]> data = new ArrayList<>();
            for (Map.Entry<String, ArrayList<String>> entry : tempData.entrySet()){
                String[] dataLine = new String[entry.getValue().size() + 1];
                dataLine[0] = entry.getKey();
                for (int i=0; i<entry.getValue().size(); i++){
                    dataLine[i+1] = entry.getValue().get(i);
                }
                data.add(dataLine);
            }
            //Writes to .csv file
            CSVWriter csvWriter = new CSVWriter();
            String filename = "data_mem" + Config.memory + "_PRR" + Config.PRR + "_nodes" + Config.numNodes + "_hours" + Config.hours + ".csv";
            try {
                csvWriter.writeToCSV(data, filename);
            } catch (IOException e) {
                System.out.println("Couldn't write to csv");
                e.printStackTrace();
            }
        }
    }
}
