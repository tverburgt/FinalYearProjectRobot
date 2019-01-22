package com.example.bluetoothapplicationpart2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;



public class ControlInterface extends Activity {

	ArrayList<String> myArr = new ArrayList<String>();
	final int handlerState = 0;
	private StringBuilder recDataString = new StringBuilder();
	TextView sensorView0;
	Handler bluetoothIn;
	Button straight;
	ConnectThread connectThread;
	ConnectedThread connectedThread;
	
	
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.copy_control_interface);
		
		Intent i = getIntent();
		//connectThread = (ConnectThread)i.getSerializableExtra("socket");
		
		String[] codeLearnChapters = new String[] { "Android Introduction","Android Setup/Installation","Android Hello World","Android Layouts/Viewgroups","Android Activity & Lifecycle","Intents in Android"};
		
		ArrayAdapter<String> codeLearnArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, codeLearnChapters);

		ListView codeLearnLessons = (ListView)findViewById(R.id.listView1);
		
		
		codeLearnLessons.setAdapter(codeLearnArrayAdapter);

		
		
		
		
		
		
		
		sensorView0 = (TextView) findViewById(R.id.sensorView0);
		
		
	      straight = (Button) findViewById(R.id.straight);
	      straight.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
				byte bytes[] = new byte[] {100};
				System.out.println("I just pressed the send");
				connectedThread.write(bytes);
				connectedThread.start();
			}
	    	  
	      });
		
		
		
		
		
		
		
	    bluetoothIn = new Handler() {
		    public void handleMessage(android.os.Message msg) {
		        if (msg.what == handlerState) {                              //if message is what we want
		          String readMessage = (String) msg.obj;                   // msg.arg1 = bytes from connect thread
		      	  //byte value = Byte.valueOf(readMessage); //This is to convert from string to byte.
		            //System.out.println("Hello");
		            recDataString.append(readMessage);                       //keep appending to string until ~      
	                myArr.add(readMessage);
	                //System.out.println("Value in array: " + myArr.get(0));
		            
		            sensorView0.setText("String Length = " + readMessage);
		        }
		    }
		};

	}		
		
		
		

	
		
		   /**************************************This if from connected Thread*****************************/   
		   public class ConnectedThread extends Thread{
				
			   
			   public String convertHexToString(String hex){
				   
					  StringBuilder sb = new StringBuilder();
					  StringBuilder temp = new StringBuilder();
				 
					  //49204c6f7665204a617661 split into two characters 49, 20, 4c...
					  for( int i=0; i<hex.length()-1; i+=2 ){
				 
					      //grab the hex in pairs
					      String output = hex.substring(i, (i + 2));
					      //convert hex to decimal
					      int decimal = Integer.parseInt(output, 16);
					      //convert the decimal to character
					      sb.append((char)decimal);
				 
					      temp.append(decimal);
					  }
					  return sb.toString();
			   }
			   
				
				private final BluetoothSocket mmSocket;
			    private final InputStream mmInStream;
			    private final OutputStream mmOutStream;
			 
			    public ConnectedThread(BluetoothSocket socket) {
			        mmSocket = socket;
			        InputStream tmpIn = null;
			        OutputStream tmpOut = null;
			 
			        // Get the input and output streams, using temp objects because
			        // member streams are final
			        try {
			            tmpIn = socket.getInputStream();
			            tmpOut = socket.getOutputStream();
			        } catch (IOException e) { }
			 
			        mmInStream = tmpIn;
			        mmOutStream = tmpOut;
			    }
			 
			    public void run() {
			        byte[] buffer = new byte[2];  // buffer store for the stream  1024
			        int bytes; // bytes returned from read()
			        // Keep listening to the InputStream until an exception occurs
			        System.out.println("I want to collect data");
			        while (true) {
			            try {
			            	 bytes = mmInStream.read(buffer);            //read bytes from input buffer
			                    String readMessage = new String(buffer, 0, bytes);
			                    String x = convertHexToString(readMessage);
			                    System.out.println("Number is: " + readMessage + " (" + x + ")");

			                    // Send the obtained bytes to the UI Activity via handler
			                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
			                    
			                   

			            } catch (IOException e) {
			                break;
			            }
			        }
			    }
			 
			    
			    /* Call this from the main activity to send data to the remote device */
			    public void write(byte[] bytes) {
			        try {
			            mmOutStream.write(bytes);
			        } catch (IOException e) { }
			    }
			 
			    /* Call this from the main activity to shutdown the connection */
			    public void cancel() {
			        try {
			            mmSocket.close();
			        } catch (IOException e) { }
			    }


			}
		/******************************************************************************************************************/ 

}