 

import java.io.IOException;
import java.util.List;

import purejavahidapi.DeviceRemovalListener;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;


/*
 * Author: Michelle Grotaers
 * Date : 20/08/2021
 * 
 * This program tests the speed of the DataCollector class in the MyScopeJava
 * program.  It plays with speed of java threads as well as HID device input.
 * Need to apply time to HID device collectData function and think about
 * making assumption that 1000 inputs are spaced at even time steps to 
 * improve frequency of input measurements.
 */
public class ScopeTester {
    
    private volatile static boolean deviceOpen = false;

    private static ScopeTester dc;
    
    private double time;
    private double[] deltaTimeArray;
    
    public Thread testThread;
    private double pt;

    public static void main(String[] args) {
        try {
            //start the device and collect the data
            dc = new ScopeTester();
            HidDevice theDevice = dc.findDevice();
            if(theDevice == null){
                System.out.println("HID Device NOT Connected.  USE DUMMY DATA");
                dc.collectData();
            } else {
                //Device found
                dc.checkDevice(theDevice);
                dc.collectData(theDevice);
            }
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    public ScopeTester() {
        //set initial conditions of variables
        time = 0;
        pt = 0;
        deltaTimeArray = new double[20];
        
    }
    
    /*
     * This is test dummy data collector and is accessed when nothing
     * connected to computer.
     */
    public void collectData(){
        
        //This is test data collector for when device not connected.
        //In this example it will only simulate time response.
        //Max speed on laptop is 30kHz if not buffered to 75kHz if buffered.
        time = 0;
        pt = System.nanoTime();//.currentTimeMillis();//.nanoTime()
        System.out.println("start: " + pt);
        
        //TEST SIGN GRAPH START
        testThread = new Thread(new Runnable(){
            public void run(){
                        
                while (true){
                    //modify code with a buffer of 1000 times run.  
                    //Max freq at 76kHz.  Read out is 75Hz.
                    double ct = System.nanoTime();//.currentTimeMillis();//.nanoTime();
                    double dt_new = ct - pt;
                    
                    pt = ct;
                    time = dt_new/1E6;//value in milli seconds
                    
                    pushValue((short) 0, time);
                    /*
                    //Apply Buffer.
                    int i = 0;
                    while (i<1000){
                        System.out.print("*");
                        i++;
                    }
                    System.out.println();
                    */
                    
                }   
            }
            
        });
        testThread.start();
        testThread.setName("Scope Dummy Data");
        
    }
    
    /*
     * This is device data collector and is accessed when circuit connected
     * to computer.
     */
    public void collectData(final HidDevice dev){
        
        //START REAL CODE
        //System.out.println("Is Device Open: " + deviceOpen);
        if (deviceOpen){
            time = 0;
            pt = System.nanoTime();//.currentTimeMillis();//.nanoTime()
            System.out.println("start: " + pt);
            
            testThread = new Thread(new Runnable(){
                public void run(){
                    
                    //time = 0;
                    
                    while (true){
                        double ct = System.nanoTime();//.currentTimeMillis();//.nanoTime();
                        double dt_new = ct - pt;
                    
                        pt = ct;
                        time = dt_new/1E6;//value in milli seconds
                        
                        byte[] data = new byte[10];
                        data[0] = 1;
                        if (((dev.getFeatureReport(data, data.length)) >= 0) && true) {
                            //System.out.println(convertBytes(data[0],data[1]));
                            pushValue((short) convertBytes(data[0],data[1]), time);
                        }
                    }
                }
            });
            testThread.start();
            testThread.setName("Scope Real Data");
        }
        
        //END READ CODE
    }
    
    
    
    /*
     * All functions to handle time prints to console.
     */
    private void pushValue(short curValue, double timeValue){
        
        //Time and value
        //System.out.println("delta time: " + timeValue + ", value: " + curValue);
        System.out.println(timeValue);
        //delta time and frequency on moving average of 20 samples.
        //pop first value and push value for graph at end of dataset
        /*
        for (int i=0; i<deltaTimeArray.length-1; i++){
            deltaTimeArray[i] = deltaTimeArray[i+1];
        }
        deltaTimeArray[deltaTimeArray.length-1]= timeValue;
        
        double sumTimeArray = (double) 0;
        for (int j=1; j<deltaTimeArray.length-1; j++){
            double deltaTime = deltaTimeArray[j] - deltaTimeArray[j-1];
            sumTimeArray = sumTimeArray + deltaTime;
        }
        
        
        //System.out.println(deltaTimeArray[0] + "," + deltaTimeArray[1] + "," + deltaTimeArray[2]);
        System.out.println("av time (sec): " + sumTimeArray/(deltaTimeArray.length-1)
                + ", av freq (Hz): " + 1/(sumTimeArray/(deltaTimeArray.length-1)));
        */
    }
    
    /*
     * All functions to handle HID Device.
     */
    
    public HidDevice findDevice() throws IOException, InterruptedException {
        HidDeviceInfo devInfo = null;
        //Find the device and let the user know if found
        if (!deviceOpen){
            System.out.println("scanning");
            //Scanning for device
            List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
            for (HidDeviceInfo info : devList){
                if (info.getVendorId() == (short) 0x4242 && info.getProductId() == (short) 0x0002){
                    //System.out.println("device info found");
                    //System.out.print("Info: ");
                    //System.out.print(info);
                    devInfo = info;
                    break;
                }
            }
            //If no device found return nothing.
            if (devInfo == null){
                System.out.println("device not found");
                deviceOpen = false;
                return null;
            }
            //If device found open it and return the opened device
            else {
                System.out.println("device found and open");
                deviceOpen = true;
                final HidDevice dev = PureJavaHidApi.openDevice(devInfo);
                System.out.println("error here");
                return dev;
            }
        } else {
            System.out.println("device not found or open");
            deviceOpen = false;
            return null;
        }
    }
    
    //Check if device has been removed
    public void checkDevice (HidDevice dev){
        if (deviceOpen){
            dev.setDeviceRemovalListener(new DeviceRemovalListener(){
                public void onDeviceRemoval(HidDevice source){
                    //System.out.println("device removed");
                    deviceOpen = false;
                }
            });
        }
    }
    
    /*
     * This function takes the binary input and converts the byte code
     * to a integer number between 0 and 1024.
     */
    private static int convertBytes (byte a, byte b){
        return (int)(
                (0xff & a) << 8 |
                (0xff & b) << 0
                );
    }
    
}