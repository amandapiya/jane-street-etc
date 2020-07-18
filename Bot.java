import java.lang.*;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.time.chrono.MinguoChronology;
import java.util.ArrayList;

class Configuration{
    String exchange_name;
    int exchange_port;
    /*
        0 = prod-like
        1 = slow
        2 = empty
    */
    final Integer test_exchange_kind = 0;
    final String team_name = "warnerbrothers";

    Configuration(Boolean test_mode){
        if(!test_mode){
            exchange_port = 20000;
            exchange_name = "production";
        }else{
            exchange_port = 20000 + test_exchange_kind;
            exchange_name = "test-exch-" + this.team_name;
        }
    }

    String exchange_name() {return exchange_name;}
    Integer port()         {return exchange_port;}
}

public class Bot{
    public static void main(String[] args){
        Configuration config = new Configuration(true);
        try{
            Socket skt = new Socket(config.exchange_name(), config.port());
            skt.setSoTimeout(10*1000);
            BufferedReader from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            PrintWriter to_exchange = new PrintWriter(skt.getOutputStream(), true);
            to_exchange.println(("HELLO " + config.team_name).toUpperCase());
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);

            int valePrice[] = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
            int valbzPrice[] = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}; // first 2 buy last 2 sell
            int bondPrice[] = {0, 0, 0,0};
            int gsPrice[] = {0, 0, 0, 0};  // first 2 buy last 2 sell
            int msPrice[] = {0, 0, 0, 0};
            int wfcPrice[] = {0, 0, 0, 0}; // first 2 buy last 2 sell
            int xlfPrice[] = {0, 0, 0, 0};
            int xlfAmount = 0;

            int orderID = 1;
            int bondThreshold = 1000;
            int vThreshold = 10;
            
            for (String line = from_exchange.readLine(); line != null; line = from_exchange.readLine()){
                String[] lineArray = line.split(" ", -1);
                if (lineArray[0].equals("BOOK") && lineArray[1].equals("BOND")){
                    if (!lineArray[3].equals("SELL") && Integer.parseInt(lineArray[3].split(":", -1)[0]) > bondThreshold){
                        to_exchange.println("ADD " + orderID++ + " BOND SELL " + Integer.parseInt(lineArray[3].split(":", -1)[0]) + " " + Integer.parseInt(lineArray[3].split(":", -1)[1]));
                        System.out.println("Sold " + Integer.parseInt(lineArray[3].split(":", -1)[0]) + " " + Integer.parseInt(lineArray[3].split(":", -1)[1]));
                    }

                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            if (Integer.parseInt(lineArray[i+1].split(":", -1)[0]) < bondThreshold)
                                to_exchange.println("ADD " + orderID++ + " BOND BUY " + Integer.parseInt(lineArray[i+1].split(":", -1)[0]) + " " + Integer.parseInt(lineArray[i+1].split(":", -1)[1]));
                                System.out.println("Bought " + Integer.parseInt(lineArray[i+1].split(":", -1)[0]) + " "+ Integer.parseInt(lineArray[i+1].split(":", -1)[1]));
                            } 
                        }
                    }
                }

                if (lineArray[0].equals("BOOK") && lineArray[1].equals("VALE")){
                    if (!lineArray[3].equals("SELL")){
                        valePrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        valePrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            valePrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            valePrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    }
               
                    if (Math.min(valePrice[1], valbzPrice[3])*(valePrice[0]-valbzPrice[2]) > vThreshold){
                        to_exchange.println("ADD " + orderID++ + " VALBZ BUY " + valbzPrice[2] + " " + Math.min(valePrice[1], valbzPrice[3]));
                        to_exchange.println("CONVERT " + orderID++ + " VALE BUY " + Math.min(valePrice[1], valbzPrice[3]));
                        to_exchange.println("ADD " + orderID++ + " VALE SELL " + valePrice[0] + " " + Math.min(valePrice[1], valbzPrice[3]));
                        System.out.println("Made $" + (Math.min(valePrice[1], valbzPrice[3])*(valePrice[0]-valbzPrice[2])-vThreshold));
                    }
                    if (Math.min(valePrice[3], valbzPrice[1])*(valbzPrice[0]-valePrice[2]) > vThreshold){
                        to_exchange.println("ADD " + orderID++ + " VALE BUY " + valePrice[2] + " " + Math.min(valePrice[3], valbzPrice[1]));
                        to_exchange.println("CONVERT " + orderID++ + " VALE SELL " + Math.min(valePrice[3], valbzPrice[1]));
                        to_exchange.println("ADD " + orderID++ + " VALBZ SELL " + valbzPrice[0] + " " + Math.min(valePrice[3], valbzPrice[1]));
                        System.out.println("Made $" + (Math.min(valePrice[3], valbzPrice[1])*(valbzPrice[0]-valePrice[2])-vThreshold));
                    }
                }

                if (lineArray[0].equals("BOOK") && lineArray[1].equals("VALBZ")){
                    if (!lineArray[3].equals("SELL")){
                        valbzPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        valbzPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }

                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            valbzPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            valbzPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    }

                    if (Math.min(valePrice[1], valbzPrice[3])*(valePrice[0]-valbzPrice[2]) > vThreshold){
                        to_exchange.println("ADD " + orderID++ + " VALBZ BUY " + valbzPrice[2] + " " + Math.min(valePrice[1], valbzPrice[3]));
                        to_exchange.println("CONVERT " + orderID++ + " VALE BUY " + Math.min(valePrice[1], valbzPrice[3]));
                        to_exchange.println("ADD " + orderID++ + " VALE SELL " + valePrice[0] + " " + Math.min(valePrice[1], valbzPrice[3]));
                        System.out.println("Made $ " + (Math.min(valePrice[1], valbzPrice[3])*(ValePrice[0]-valbzPrice[2])-vThreshold));
                    }
                    if (Math.min(valePrice[3], valbzPrice[1])*(valbzPrice[0]-valePrice[2]) > vThreshold){
                        to_exchange.println("ADD " + orderID++ + " VALE BUY " + valePrice[2] + " " + Math.min(valePrice[3], valbzPrice[1]));
                        to_exchange.println("CONVERT " + orderID++ + " VALE SELL " + Math.min(valePrice[3], valbzPrice[1]));
                        to_exchange.println("ADD " + orderID++ + " VALBZ SELL " + valbzPrice[0] + " " + Math.min(valePrice[3], valbzPrice[1]));
                        System.out.println("Made $" + (Math.min(valePrice[3], valbzPrice[1])*(valbzPrice[0]-valePrice[2])-vThreshold));
                    }
                }

                if (lineArray[0].equals("BOOK") && lineArray[1].equals("XLF")){
                    if (!lineArray[3].equals("SELL")){
                        xlfPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        xlfPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            xlfPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            xlfPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    }

                    if (bondPrice[0] != 0 && gsPrice[0] != 0 && MsPrice[0] != 0 && wfcPrice[0] != 0){
                        if (xlfPrice[0]*10 < bondPrice[2]*3 + gsPrice[2]*2 + MsPrice[2]*3 + wfcPrice[2]*2){
                            to_exchange.println("ADD " + orderID++ + " XLF BUY " + xlfPrice[0] + " " + xlfPrice[1]);
                            xlfAmount += xlfPrice[1];
                            /*
                            if (trades > 0){
                                to_exchange.println("ADD " + orderID++ + " XLF BUY " + xlfPrice[0] + " " + trades*10);
                                to_exchange.println("CONVERT " + orderID++ + " XLF SELL " + trades*10);
                                to_exchange.println("ADD " + orderID++ + " BOND SELL " + bondPrice[2] + " " + 3*trades);
                                to_exchange.println("ADD " + orderID++ + " GS SELL " + gsPrice[2] + " " + 2*trades);
                                to_exchange.println("ADD " + orderID++ + " MS SELL " + msPrice[2] + " " + 3*trades);
                                to_exchange.println("ADD " + orderID++ + " WFC SELL " + wfcPrice[2] + " " + 2*trades);
                                System.out.println("Made $" + (100 + (xlfPrice[0]*10) - (bondPrice[2]*3 + gsPrice[2]*2 + msPrice[2]*3 + wfcPrice[2]*2)));
                            }
                            */
                        }
                        if ((xlfPrice[2]*10) > bondPrice[2]*3 + gsPrice[2]*2 + msPrice[2]*3 + wfcPrice[2]*2 && xlfAmount > 0){
                            to_exchange.println("ADD " + orderID+++ + " XLF SELL " + xlfPrice[0] + " " + xlfPrice[3]);
                            xlfAmount -= xlfPrice[3];
                        }
                    }
                }

                if (lineArray[0].equals("BOOK") && lineArray[1].equals("BOND")){
                    if (!lineArray[3].equals("SELL")){
                        bondPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        bondPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            bondPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            bondPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    }
                }
                if (lineArray[0].equals("BOOK") && lineArray[1].equals("GS")){
                    if (!lineArray[3].equals("SELL")){
                        gsPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        gsPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            gsPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            gsPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                }
                if (lineArray[0].equals("BOOK") && lineArray[1].equals("MS")){
                    if (!lineArray[3].equals("SELL")){
                        msPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        msPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            msPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            msPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                }
                if (lineArray[0].equals("BOOK") && lineArray[1].equals("WFC")){
                    if (!lineArray[3].equals("SELL")){
                        wfcPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        wfcPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i = 0; i < lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i < lineArray.length-1){
                            wfcPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            wfcPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                }
            }
        }
        catch(Exception e){
            e.printStackTrace(System.out);
        }
    }
}