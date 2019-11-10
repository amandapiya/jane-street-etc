/* HOW TO RUN
   1) Configure things in the Configuration class
   2) Compile: javac Bot.java
   3) Run in loop: while true; do java Bot; sleep 1; done
*/
import java.lang.*;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.time.chrono.MinguoChronology;
import java.util.ArrayList;

class Configuration {
    String exchange_name;
    int    exchange_port;
    /* 0 = prod-like
       1 = slow
       2 = empty
    */
    final Integer test_exchange_kind = 0;
    /* replace REPLACEME with your team name */
    final String  team_name          = "warnerbrothers";

    Configuration(Boolean test_mode) {
        if(!test_mode) {
            exchange_port = 20000;
            exchange_name = "production";
        } else {
            exchange_port = 20000 + test_exchange_kind;
            exchange_name = "test-exch-" + this.team_name;
        }
    }

    String  exchange_name() { return exchange_name; }
    Integer port()          { return exchange_port; }
}

public class Bot
{
    public static void main(String[] args)
    {
        /* The boolean passed to the Configuration constructor dictates whether or not the
           bot is connecting to the prod or test exchange. Be careful with this switch! */
        Configuration config = new Configuration(true);
        try
        {
            Socket skt = new Socket(config.exchange_name(), config.port());
            skt.setSoTimeout(10*1000);
            BufferedReader from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            PrintWriter to_exchange = new PrintWriter(skt.getOutputStream(), true);

            /*
              A common mistake people make is to to_exchange.println() > 1
              time for every from_exchange.readLine() response.
              Since many write messages generate marketdata, this will cause an
              exponential explosion in pending messages. Please, don't do that!
            */
            to_exchange.println(("HELLO " + config.team_name).toUpperCase());
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);

            int ValePrice[] = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}; //
            int ValbzPrice[] = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};//first 2 buy last 2 sell
            int BondPrice[] = {0, 0, 0,0}; //
            int GsPrice[] = {0, 0, 0, 0};//first 2 buy last 2 sel
            int MsPrice[] = {0, 0, 0, 0};
            int WfcPrice[] = {0, 0, 0, 0}; //first 2 buy last 2 sel
            int XlfPrice[] = {0,0,0,0};


            int orderNum = 1;
            
            for(String line = from_exchange.readLine(); line != null; line = from_exchange.readLine())
            {
                String[] lineArray = line.split(" ",-1);
                if(lineArray[0].equals("BOOK") && lineArray[1].equals("BOND"))
                {
                    if(!lineArray[3].equals("SELL") && Integer.parseInt(lineArray[3].split(":",-1)[0]) > 1000)
                    {
                        to_exchange.println("ADD " + orderNum++ + " BOND SELL " + Integer.parseInt(lineArray[3].split(":",-1)[0]) + " " +Integer.parseInt(lineArray[3].split(":",-1)[1])  );
                        System.out.println("Sold " +Integer.parseInt(lineArray[3].split(":",-1)[0]) + " " +Integer.parseInt(lineArray[3].split(":",-1)[1]) );
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            if(Integer.parseInt(lineArray[i+1].split(":",-1)[0]) < 1000)
                            {
                                to_exchange.println("ADD " + orderNum++ + " BOND BUY " + Integer.parseInt(lineArray[i+1].split(":",-1)[0]) + " "+Integer.parseInt(lineArray[i+1].split(":",-1)[1])  );
                                System.out.println("Bought " +Integer.parseInt(lineArray[i+1].split(":",-1)[0]) + " "+Integer.parseInt(lineArray[i+1].split(":",-1)[1])  );
                            } 
                        }
                    }
                }

                if(lineArray[0].equals("BOOK") && lineArray[1].equals("VALE"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        ValePrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        ValePrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            ValePrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            ValePrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    }
               
                    if (Math.min(ValePrice[1], ValbzPrice[3])*(ValePrice[0]-ValbzPrice[2]) >10){
                        to_exchange.println("ADD " + orderNum++ + " VALBZ BUY " + ValbzPrice[2] + " " +Math.min(ValePrice[1], ValbzPrice[3])); 
                        to_exchange.println("CONVERT " + orderNum++ + " VALE BUY " + Math.min(ValePrice[1], ValbzPrice[3])); 
                        to_exchange.println("ADD " + orderNum++ + " VALE SELL " +  ValePrice[0] + " " +Math.min(ValePrice[1], ValbzPrice[3]));
                        System.out.println("Made " + (Math.min(ValePrice[1], ValbzPrice[3])*(ValePrice[0]-ValbzPrice[2]) -10) + " dollars");
                    } 
                    if (Math.min(ValePrice[3], ValbzPrice[1])*(ValbzPrice[0]-ValePrice[2]) >10){
                        to_exchange.println("ADD " + orderNum++ + " VALE BUY " + ValePrice[2] + " " +Math.min(ValePrice[3], ValbzPrice[1])); 
                        to_exchange.println("CONVERT " + orderNum++ + " VALE SELL " + Math.min(ValePrice[3], ValbzPrice[1])); 
                        to_exchange.println("ADD " + orderNum++ + " VALBZ SELL " +  ValbzPrice[0] + " " +Math.min(ValePrice[3], ValbzPrice[1]));
                        System.out.println("Made " + (Math.min(ValePrice[3], ValbzPrice[1])*(ValbzPrice[0]-ValePrice[2]) -10) + " dollars"); 
                    }
                }
                if(lineArray[0].equals("BOOK") && lineArray[1].equals("VALBZ"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        ValbzPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        ValbzPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            ValbzPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            ValbzPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    }

                    if (Math.min(ValePrice[1], ValbzPrice[3])*(ValePrice[0]-ValbzPrice[2]) >10){
                        to_exchange.println("ADD " + orderNum++ + " VALBZ BUY " + ValbzPrice[2] + " " +Math.min(ValePrice[1], ValbzPrice[3])); 
                        to_exchange.println("CONVERT " + orderNum++ + " VALE BUY " + Math.min(ValePrice[1], ValbzPrice[3])); 
                        to_exchange.println("ADD " + orderNum++ + " VALE SELL " +  ValePrice[0] + " " +Math.min(ValePrice[1], ValbzPrice[3]));
                        System.out.println("Made " + (Math.min(ValePrice[1], ValbzPrice[3])*(ValePrice[0]-ValbzPrice[2]) -10) + " dollars");
                    } 
                    if (Math.min(ValePrice[3], ValbzPrice[1])*(ValbzPrice[0]-ValePrice[2]) >10){
                        to_exchange.println("ADD " + orderNum++ + " VALE BUY " + ValePrice[2] + " " +Math.min(ValePrice[3], ValbzPrice[1])); 
                        to_exchange.println("CONVERT " + orderNum++ + " VALE SELL " + Math.min(ValePrice[3], ValbzPrice[1])); 
                        to_exchange.println("ADD " + orderNum++ + " VALBZ SELL " +  ValbzPrice[0] + " " +Math.min(ValePrice[3], ValbzPrice[1]));
                        System.out.println("Made " + (Math.min(ValePrice[3], ValbzPrice[1])*(ValbzPrice[0]-ValePrice[2]) -10) + " dollars"); 
                    }
                }
                
                boolean history = false;

                if(lineArray[0].equals("BOOK") && lineArray[1].equals("XLF"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        XlfPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        XlfPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            XlfPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            XlfPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                    //test if history exists for other stocks
                    if(!history)
                    {
                        if(BondPrice[0] != 0 && GsPrice[0] != 0 && MsPrice[0] != 0 && WfcPrice[0] != 0)
                            history = true;
                    }

                    if(history)
                    {
                        if(100 + (XlfPrice[0] * 10) < BondPrice[2] * 3 + GsPrice[2] * 2 + MsPrice[2] * 3 + WfcPrice[2] * 2)
                        {
                            int leftMaxTrades = XlfPrice[1] / 10;
                            int rightMaxTrades = Math.min( Math.min(BondPrice[3] / 3, GsPrice[3] / 2), Math.min(MsPrice[3]/3, WfcPrice[3]/2));
                            int trades = Math.min(leftMaxTrades,rightMaxTrades);

                            to_exchange.println("ADD " + orderNum++ + " XLF BUY " + XlfPrice[0] + " " + trades * 10); 
                            to_exchange.println("CONVERT " + orderNum++ + " XLF SELL " + trades * 10); 
                            to_exchange.println("ADD " + orderNum++ + " BOND SELL " +  BondPrice[2] + " " +3 * trades);
                            to_exchange.println("ADD " + orderNum++ + " GS SELL " +  GsPrice[2] + " " +2 * trades);
                            to_exchange.println("ADD " + orderNum++ + " MS SELL " +  MsPrice[2] + " " + 3 * trades);
                            to_exchange.println("ADD " + orderNum++ + " WFC SELL " +  WfcPrice[2] + " " + 2 * trades);
                            System.out.println("Made " +(100 + (XlfPrice[0] * 10) - BondPrice[2] * 3 + GsPrice[2] * 2 + MsPrice[2] * 3 + WfcPrice[2] * 2) + " dollars");

                        }
                    }
                }

                if(lineArray[0].equals("BOOK") && lineArray[1].equals("BOND"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        BondPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        BondPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            BondPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            BondPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                
                }
                if(lineArray[0].equals("BOOK") && lineArray[1].equals("GS"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        GsPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        GsPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            GsPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            GsPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                }
                if(lineArray[0].equals("BOOK") && lineArray[1].equals("MS"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        MsPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        MsPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            MsPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            MsPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                }
                if(lineArray[0].equals("BOOK") && lineArray[1].equals("WFC"))
                {
                    if(!lineArray[3].equals("SELL"))
                    {
                        WfcPrice[0] = Integer.parseInt(lineArray[3].split(":")[0]);
                        WfcPrice[1] = Integer.parseInt(lineArray[3].split(":")[1]);
                    }
                    for (int i=0; i<lineArray.length; i++){
                        if (lineArray[i].equals("SELL") && i<lineArray.length-1){
                            WfcPrice[2] = Integer.parseInt(lineArray[i+1].split(":")[0]);
                            WfcPrice[3] = Integer.parseInt(lineArray[i+1].split(":")[1]);
                        }
                    } 
                } 

            }

        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }
}
/*
class Ticker
{
    ArrayList<Double> buyPrices = new ArrayList<Double>();
    ArrayList<Integer> buyAmounts = new ArrayList<Integer>();
    ArrayList<Double> sellPrices = new ArrayList<Double>();
    ArrayList<Integer> sellAmounts = new ArrayList<Integer>();

    public Ticker(String tickerSymbol)
    {

    }

    public addBuy(double price, int amount)
    {
        buyPrices.add(price);
        buyAmounts.add(amount);
    }
    public addSell(double price, int amount)
    {
        sellPrices.add(price);
        sellAmounts.add(amount);
    }
}
*/