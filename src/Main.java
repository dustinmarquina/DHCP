import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

public class Main {
    public static void main(String args[]) throws UnknownHostException{

        Vector<String> Available_Devices=new Vector<>(); // stores the list of available/connected devices
        String myip=InetAddress.getLocalHost().getHostAddress(); // IP of the PC in which the code is running/localhost
        if(myip.equals("127.0.0.1")){
            System.out.println("This PC is not connected to any network!");
        }
        else {
            String mynetworkips=new String(); // just a new string to store currently scanning ip

            // this loop finds the right most '.' of this PC's IP
            // suppose your PC's IP is 192.168.0.101, this loop finds the index of the '.' just before '101'
            // and as soon as it finds the '.', it creates a new string(actually substring of this PC's IP) starting at
            // index 0 and ending at index containing character '.' and exits from the loop
            // So here, if the IP was 192.168.0.101,mynetworkips will have the value "192.168.0."
            for(int i=myip.length()-1;i>=0;--i){
                if(myip.charAt(i)=='.'){
                    // .substring(i,j) returns a string starting from index i and ending at index j-1,so in order to
                    // include '.' , i put (i+1)
                    mynetworkips=myip.substring(0,i+1);
                    break;
                }
            }

            System.out.println("My Device IP: " + myip+"\n"); // Shows this PC's IP

            System.out.println("Search log:");

            // (loop bellow->) just add the string representation of i and add it to mynetworkips to get full IP
            // for example, when i=1 the ip will be(if mynetworkips is "192.168.0.") 192.168.0.1,
            // and then at next iteration it'll be 192.168.0.1
            // this means tis loop iterates over all possible ips and show you which one is available or not.

            // you can change i's range if you know that your network's IPs start from another
            // point(probably for most router(if not customized) , it will start from 192.168.0.100)
            for(int i=1;i<=254;++i){
                try {
                    // Create an InetAddrss object with new IP
                    InetAddress addr=InetAddress.getByName(mynetworkips + new Integer(i).toString());

                    if (addr.isReachable(1000)){ // See if it is reachable or simply available(check time is 1s=1000ms)
                        System.out.println("Available: " + addr.getHostAddress()); // show that it is available
                        Available_Devices.add(addr.getHostAddress()); // if available, add it to final list
                    }
                    else System.out.println("Not available: "+ addr.getHostAddress());  // show that it is not available

                }catch (IOException ioex){
                    // nothing to do, just catch it if something goes wrong
                }
            }

            // print the list of available devices
            System.out.println("\nAll Connected devices(" + Available_Devices.size() +"):");
            for(int i=0;i<Available_Devices.size();++i) System.out.println(Available_Devices.get(i));
        }
    }
}