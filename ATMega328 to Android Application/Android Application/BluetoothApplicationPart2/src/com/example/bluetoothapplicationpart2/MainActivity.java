package com.example.bluetoothapplicationpart2;


import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Set;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomButton;


public class MainActivity extends Activity {

   private static final int REQUEST_ENABLE_BT = 1;
   //************************Widget and button within the UI initialised***************************************
   private Button searchButton, capture_image, left, straight, right, backwards, update; 
   private TextView text, altitude;
   private BluetoothAdapter myBluetoothAdapter;

   private ListView listView;
   private ArrayAdapter<String> BTArrayAdapter;
   private BluetoothDevice mBluetoothDevice;
   public ToggleButton toggleButton;
   public Handler bluetoothIn;
   final int handlerState = 0;   //used to identify handler message
   //*************************************************************************************************************
   
   
   private Set<BluetoothDevice> pairedDevices;
   private StringBuilder recDataString = new StringBuilder();
   ArrayList<String> myArr = new ArrayList<String>();  //Array list that will hold the Hex values of the captured image.
   private TextView battery_power, temperature, atmospheric_pressure, data_coming_in;
   Bitmap bitmap;
   ImageView imageView;
   SeekBar seekBar;
   private ConnectedThread connectedThread;
   public ConnectThread connectThread;
   public boolean endOfPicture = false;
   public int no_of_asterix = 0;



@SuppressLint("HandlerLeak")
@Override
   protected void onCreate(Bundle savedInstanceState) {  //Activity Main which is first run when the application starts
      super.onCreate(savedInstanceState);
      setContentView(R.layout.control_interface);
      //Each UI widget is define with an associated id from the control_interface xml file.
      battery_power = (TextView) findViewById(R.id.battery_power);
      temperature = (TextView) findViewById(R.id.temperature);
      atmospheric_pressure = (TextView) findViewById(R.id.atmospheric_pressure);
      altitude = (TextView) findViewById(R.id.altitude);
      //data_coming_in = (TextView) findViewById(R.id.data_coming_in);
      capture_image = (Button) findViewById(R.id.capture_image);
      left = (Button) findViewById(R.id.left);
      straight = (Button) findViewById(R.id.straight);
      right = (Button) findViewById(R.id.right);
      backwards = (Button) findViewById(R.id.backwards);
      update = (Button) findViewById(R.id.update);
      seekBar = (SeekBar) findViewById(R.id.seekBar1);
      //***********************************************************************************
      
      
      
      myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      //Check whether Device has a BluetoothAdapter. If not, disable UI application widgets.
      if(myBluetoothAdapter == null) {   
    	  searchButton.setEnabled(false);
    	  capture_image.setEnabled(false);
    	  toggleButton.setEnabled(false);
    	  listView.setEnabled(false);
    	  update.setEnabled(false);
    	  straight.setEnabled(false);
    	  backwards.setEnabled(false);
    	  left.setEnabled(false);
    	  right.setEnabled(false);
    	  update.setEnabled(false);
    	  
    	  //Displays a toast message that device does not support bluetooth
    	  Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
         		 Toast.LENGTH_LONG).show();
      } else {
    	  
    	  
	      toggleButton = (ToggleButton) findViewById(R.id.bluetoothOnOffButton);
	      //Uses to make toggle button read events from the UI. The setOnCheckListener is used from
	      //ToggleButton API for this to be done.
	      toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	        	  
	              if (isChecked) { // The toggle is enabled
	            	  on();
	              }else {  // The toggle is disabled
	                  off();   
	             }
	          }
	      });
	      
	      //Update the values of battery power, temperature, pressure and altitude
	      update.setOnClickListener(new OnClickListener(){  
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				myArr.removeAll(myArr);   //Removes all elements from the ArrayList.
				//Initialisation of a ConnectThread used to send and Received data.
				connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
				connectedThread.start(); //Start the thread
				byte bytes[] = new byte[] {'d'};
				connectedThread.write(bytes); //The character 'd' is sent to the ATMega328.
			} 
	      });
	      
	      
	      
	      searchButton = (Button)findViewById(R.id.searchButton);
	      searchButton.setOnClickListener(new OnClickListener() {
	  		
	  		@Override
	  		public void onClick(View v) {
	  			// TODO Auto-generated method stub
	  			search(v);
	  		}
	      });
	      

	      
	      //Create a list all the paired devices in a list view.
	      listView = (ListView)findViewById(R.id.listView);
	      // create the arrayAdapter that contains the BTDevices, and set it to the ListView
	      BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	      listView.setAdapter(BTArrayAdapter);
	      
	      listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				mBluetoothDevice = myBluetoothAdapter.getRemoteDevice("00:13:02:25:90:66");
				BluetoothDevice x = mBluetoothDevice;
				myBluetoothAdapter.cancelDiscovery();
				connectThread = new ConnectThread(x, myBluetoothAdapter);
				connectThread.start();
			}  
	  });    
	      
	      
	      
        

	        //Used to tell the robot to take a picture and
	        //used to fetch the hexadecimal values that correspond to the picture.
	      	capture_image.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					//Clear all data from ArrayList (if there is data) from previous image.
					myArr.removeAll(myArr);
					//Initialisation of a ConnectThread used to send and Received data.
					connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
					connectedThread.start();  //Thread is started.
					byte bytes[] = new byte[] {'p'}; //Used to have 100
					connectedThread.write(bytes); //The value 100 is sent to the ATMega328-PU.
				}
	      		
	      	});
	      
	      	
	      	
	      	//Method used to send the character f to make the robot drive forward.
	      	straight.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					//Initialisation of a ConnectThread used to send and Received data.
					connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
					//connectedThread.start();   //Thread is started.
					byte bytes[] = new byte[] {'f'};
					connectedThread.write(bytes); //Send the value char f to ATMega328-PU.
				}
	      		
	      	});
	      	
	      	
	      //Method used to send the character l to make the robot turn left.
	      	left.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					//Initialisation of a ConnectThread used to send and Received data.
					connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
					//connectedThread.start();   //Thread is started.
					byte bytes[] = new byte[] {'l'};
					connectedThread.write(bytes);   //Send the value char l to ATMega328-PU
				}
	      		
	      	});
	      	
	      //Method used to send the character r to make the robot turn right.
	      	right.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
					byte bytes[] = new byte[] {'r'};
					connectedThread.write(bytes);
				}
	      	});

	      //Method used to send the character l to make the robot turn right.
	      	backwards.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
					//connectedThread.start();
					byte bytes[] = new byte[] {'b'};
					connectedThread.write(bytes);
				}
	      		
	      	});
	      	
	      	
	      //Used to handle seekbar processes.	
	  	  seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	  		  int position = 0;
	  		    
	  		  //Used to notify that the progress level has changed.
			  @Override
			  public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
				  position = progresValue;  
			  }
			//Used to notify that the user has started a touch gesture.
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { 
				//No additional actions within this overridden method.
			}
			
			//Used to notify that the user has finished a touch gesture.
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			    //Send the position value of the seek bar to he ATMega328-PU.
				connectedThread = new ConnectedThread(connectThread.getBluetoothSocket());
				//connectedThread.start();
				byte bytes[] = new byte[] {(byte) position}; //progresValue is between 1-10.
				connectedThread.write(bytes);  //Send the progresValue as byte the ATMega328.
			}
		   });
	      	
	      	
	      	
	      	
	      	
	      	
    
	      
    }     //End of Major else statement
      
      
      //Handler used for the Main activity to interact with processes in
      //ConnectedThread thread. The main activity can't acccess variables 
      //from a child thread.
      bluetoothIn = new Handler() {
  	    public void handleMessage(android.os.Message msg) {
  	        if (msg.what == handlerState) {                              //if message is what we want
  	          String readMessage = (String) msg.obj;                   // msg.arg1 = bytes from connect thread
  	            recDataString.append(readMessage);                       //keep appending to string until ~      

                 
  	            if(readMessage.equals("#")){       //Check if the image if finished coming in.
					saveCameraHexValuestoTextFileInSDCard();  //Save image in a text file format in external SD card.
					ConvertHexValuesToASCIIAndDisplayImage(); //Display the image in the application.
  	            }
	  	           
  	            
	            myArr.add(readMessage); //Add every single piece in to an ArrayList.

	            
	                //This if statement is used to check for asterisk values in data that comes in.
	                //Values with asterisk data contains values of power, temperature, pressure and altitude.
	                //This statement has the job of extracting these preferred values without the the asterisks from
	                //the ArraList. Once preferred data is extract they are displayed in the UI
	  	            if(readMessage.equals("*")){    //Detect if power, temperature, pressure and altitude values have come in.
	  	            	no_of_asterix++;
	  	            	if(no_of_asterix == 5){
	  	            		
	  	            	   
	  	            	     int[] astrixPosition = new int[5];
	  	            		int index = 0;
	  	            		
	  	            			for(int i = 0; i<myArr.size(); i++){
	  	            				if(myArr.get(i).equals("*")){
	  	            					astrixPosition[index] = i;
	  	            					index++;
	  	            				}
	  	            			}
	  	            			
	  	            			
	  	            			for(int x = 0; x<5; x++)
	  	            				System.out.println(astrixPosition[x]);
	  	            			
	  	            			
	  	          	          String b_power = "";
	  	        	          String temp ="";
	  	        	          String pressure ="";
	  	        	          String alti ="";
	  	            			

	  		  	            	//Battery Power
	  		  	            	for(int position = astrixPosition[0]+1; position < astrixPosition[1]; position++){
	  		  	            		b_power += myArr.get(position);
	  		  	            	}
	  		  	            	for(int position = astrixPosition[1]+1; position < astrixPosition[2]; position++){
	  		  	            		temp += myArr.get(position);
	  		  	            	}
	  		  	            	for(int position = astrixPosition[2]+1; position < astrixPosition[3]; position++){
	  		  	            		pressure += myArr.get(position);
	  		  	            	}
	  		  	            	for(int position = astrixPosition[3]+1; position < astrixPosition[4]; position++){
	  		  	            		alti += myArr.get(position);
	  		  	            	}
	  		  	            	
	  		  	            	
	  		  	            	battery_power.setText(b_power + " %");
	  		  	            	//System.out.println("The batter power is: " + b_power);
	  		  	            	temperature.setText(temp + " °C");
	  		  	            	//System.out.println("The temperature is: " + temp + " °C");
	  		  	            	atmospheric_pressure.setText(pressure + " Pa");
	  		  	            	//System.out.println("The pressure is: " + pressure + " Pa");
	  		  	            	altitude.setText(alti + " m");
	  		  	            	//System.out.println("The altitude is: " + alti + " m");
	  	            	}
  	
	  	          }  	//End of if statement.     
  	        }
  	    }
  	}; 
  	
  	
  	//When a BluetoothDevice is picked from an ListView. The ConnectThread is called to obtain
  	//a BluetoothSocket. This socket instance is returned that indicates a connection. This socket 
  	//instance is used to send an receive data to and rom the Android Application.
  	listView.setOnItemClickListener(new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			mBluetoothDevice = myBluetoothAdapter.getRemoteDevice("00:13:02:25:90:66");
			BluetoothDevice x = mBluetoothDevice;
			myBluetoothAdapter.cancelDiscovery();
			connectThread = new ConnectThread(x, myBluetoothAdapter);
			connectThread.start();	
		}
  		
  	});
  	
  	
  	
  
}    //End of OnCreate() 


    //Created method used turn on Bluetooth if it is off.
	public void on(){
      if (!myBluetoothAdapter.isEnabled()) {
         Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
         Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
        		 Toast.LENGTH_LONG).show();
      }else{   //Else if already on display a toast message indicating that.
          Toast.makeText(getApplicationContext(),"Bluetooth already on" ,
         		 Toast.LENGTH_LONG).show();
      }
   }
	
	//Created method used to switch off Bluetooth.
	public void off(){ 
		if(myBluetoothAdapter.isEnabled())
		{
		    myBluetoothAdapter.disable();
		    Toast.makeText(getApplicationContext(),"Bluetooth turned off" ,
	        Toast.LENGTH_LONG).show();
		}else{
	          Toast.makeText(getApplicationContext(),"Bluetooth already off" ,
	          		 Toast.LENGTH_LONG).show();
	       }

	}
	
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	   // TODO Auto-generated method stub
	   if(requestCode == REQUEST_ENABLE_BT){
		   if(myBluetoothAdapter.isEnabled()) {
			   text.setText("Status: Enabled");
		   } else {   
			   text.setText("Status: Disabled");
		   }
	   }
   }

   //Displays paired devices that in a ListView using the ArrayAdapter.
   public void list(View view){
	  // get paired devices
      pairedDevices = myBluetoothAdapter.getBondedDevices();
      
      // put it's one to the adapter
      for(BluetoothDevice device : pairedDevices){
    	  BTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());
      }

      Toast.makeText(getApplicationContext(),"Show Paired Devices",
    		  Toast.LENGTH_SHORT).show();
      
   }
   
   
  
   
   //Used to discover other Bluetooth devices within the vicinity.
   final BroadcastReceiver bReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	             // Get the BluetoothDevice object from the Intent
	        	 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	 // add the name and the MAC address of the object to the arrayAdapter
	             BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	             BTArrayAdapter.notifyDataSetChanged();
	        }
	    }
	};
	
	//Creatd method to start and end a discovery of Bluetooth devices in the vicinity
   public void search(View view) {
	   if (myBluetoothAdapter.isDiscovering()) {
		   // the button is pressed when it discovers, so cancel the discovery
		   myBluetoothAdapter.cancelDiscovery();
	   }
	   else {
			BTArrayAdapter.clear();
			myBluetoothAdapter.startDiscovery();
			
			registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));	
		}    
   }
  
   public void off(View view){
	  myBluetoothAdapter.disable();
	  text.setText("Status: Disconnected");
	  
      Toast.makeText(getApplicationContext(),"Bluetooth turned off",
    		  Toast.LENGTH_LONG).show();
   }
   
   //Override activity activity life cycle used to excute code when the user
   //decides to exit the appliction. In this case the Broadcast receiver is 
   //made unregisters and the connectedThread is shutdown using the cancel().
   @Override
   protected void onDestroy() {
	   // TODO Auto-generated method stub
	   super.onDestroy();
	   unregisterReceiver(bReceiver);
	   connectedThread.cancel();
   }   
   
   
   
   
   //Method called to save the image data (Hexidecimal values) in 
   //.txt file. This file later used to produce an image on the application
   //captured by the robot
   public void saveCameraHexValuestoTextFileInSDCard(){
		
			String address = "/sdcard/mysdfile.txt";
		
				try {
					File myFile = new File("/sdcard/mysdfile.txt"); //The .txt file is save in this directory of the phone.
					myFile.createNewFile();     //Create file if one is not created
					FileOutputStream fOut = new FileOutputStream(myFile);   //(myFile, true); Set true to append after existing line.
					OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut); //Initiliase output stream to output the values
					//into the .txt file
					for(int counter = 0; counter< myArr.size(); counter++)
					myOutWriter.append(myArr.get(counter)); //Every piece of new data is append to alread current data in .txt file.
					myOutWriter.close();
					fOut.close();
					Toast.makeText(getBaseContext(),
							"Done writing SD 'mysdfile.txt'",
							Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(getBaseContext(), e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
   }
   
   
   //Used to extract image hex values from .txt file and convert to ASCII. ASCII values 
   //are then stored in a byte array which a Bitmap uses to display an image.
   public void ConvertHexValuesToASCIIAndDisplayImage(){
		String line = "";
        String line_final = "";
        try {
            String sCurrentLine;
            BufferedReader br = new BufferedReader(new FileReader("/sdcard/mysdfile.txt"));//test.txt hex code string
            DataOutputStream os = new DataOutputStream(new FileOutputStream("/sdcard/hello.jpg"));
            while ((sCurrentLine = br.readLine()) != null) {
                //line = StringUtils.deleteWhitespace(sCurrentLine);
                byte[] temp = convertHexadecimal2Binary(sCurrentLine.getBytes());
                os.write(temp);
            }
            os.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
		File imageFile = new File("/sdcard/hello.jpg");
		imageView = (ImageView)findViewById(R.id.imageView);
		bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
		imageView.setImageBitmap(bitmap);
	   
	   
	   
   }
   
   
   
   
   
	//*********************************************convertHexadecimal2Binary********************************************
   //A method also used to covert all hex values that come in to ASCII. This method is used for testing purposes
   //the the ConnectedThread thread to analysig the type of ASCII values coming in.
   //This method was found at the following URL: http://stackoverflow.com/questions/8624203/converting-a-hex-string-to-an-image-file
	public static byte[] convertHexadecimal2Binary(byte[] hex) {
	   String HEX_STRING = "0123456789ABCDEF";

      int block = 0;
      byte[] data = new byte[hex.length / 2];
      int index = 0;
      boolean next = false;
      for (int i = 0; i < hex.length; i++) {
          block <<= 4;
          int pos = HEX_STRING.indexOf(Character.toUpperCase((char) hex[i]));
          if (pos > -1) {
              block += pos;
          }
          if (next) {
              data[index] = (byte) (block & 0xff);
              index++;
              next = false;
          } else {
              next = true;
          }
      }
      return data;
  }

//***************************************************End of Function********************************************	
   
   /**************************************This is ConnectedThread Class*****************************/   
	/* This thread is used to send and receive data to and from the robot using the obtain BluetoothSocket */
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
	 
	    //Constructor used to obtainthe BluetoothSocket instance from the Main Activity.
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
	 
	    public void run() {  //Run method used to start a thread.
	        byte[] buffer = new byte[1];  // buffer store for the stream  1024
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
	                    
	                    
	                    if(readMessage.equals("#")){
	                    	System.out.println("We broke out Thread for image data");
	                    	break;
	                    }
	                    
	                  //The if statement is break out the thread one all values of power, temperature, pressure and 
	                  //and altitude have come in. There is five asterisks embedding the string data. 
	                  //Counting the number of asterisk help the application know all the data has come in.
	                  if(no_of_asterix == 5){
	                    	System.out.println("We broke out of thread for pressure, temp...");
	                    	no_of_asterix = 0;
	                    	break;
	                  }
	                

	            } catch (IOException e) {
	            	System.out.println("We broke out the thread");
	                break;
	            }
	        }
	    }
	 
	    
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	            System.out.println("Just sent something");
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
   
 
