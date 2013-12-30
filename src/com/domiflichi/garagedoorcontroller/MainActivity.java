package com.domiflichi.garagedoorcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;


import java.io.BufferedWriter; // output
import java.io.BufferedReader; // input
import java.io.InputStreamReader; // input
import java.io.OutputStreamWriter; // output
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


/*
 
 Post-Project To-Dos:
 
 1. After project is COMPLETE, create a different version that has more security. i.e.
 	Hash (MD5) the password, add salt (like the date or something)
 
 Notes
 
 May want to note the following:
 http://stackoverflow.com/questions/6567749/android-alert-box-after-3000-ms
 Note the bolded note about using 'removeMessages()' on your handler in onStop() to avoid exceptions
 
 This program requires the following permissions:
 Internet - Uses the Internet to connect the Garage Door Controller device
 Access Network State - To make sure there is Internet access upon program opening, closes if none available
 
 */


public class MainActivity extends Activity implements OnClickListener, OnSharedPreferenceChangeListener {
	
	
	
	private Button connectNet;
	private Button toggleDoor;
	
	private CheckBox crackDoor;

	private TextView tvGDStatusText; // represents the 'garage door status text'
	private TextView tvConStatusText; // represents the program's connection status
	
	private ImageView imgGarageDoorStatus; //NEW 11-29-13!!!
	private Drawable newGDStatusImage; //NEW 11-29-13

	private String settings_ip_domain;
	private String serverIpAddress = "";
	private String myPass;
	private String line;
	private String newStatusOfGarageDoor = "";
	private String currentStatusOfGarageDoor = "";
	private String milsToPauseForCrack = "";

	private boolean connected = false;

	private AtomicInteger myAtomicButtonInteger;

	private int serverPort; // Is this ok, or should it be a 'long'??
	
	private Handler handler = new Handler();
	//private Handler handler; // Doing it this way seemed to have caused problems :( ... Not sure what the difference is

	private EditText defIPDomain;
	private EditText defPort;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        // Check network state
        boolean isNetworkConnected = isNetworkAvailable();
        if (!isNetworkConnected) {
        	// Toast message here
        	//Toast.makeText(MainActivity.this, "We have connectivity", Toast.LENGTH_SHORT).show();
        	
        	// Alert dialog here
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("You have no network/internet access. Please try again later. This program will now exit.")
        			.setCancelable(false)
        			.setNeutralButton("OK", new DialogInterface.OnClickListener(){
        				public void onClick(DialogInterface dialog, int id) {
        					MainActivity.this.finish();
        				}
        			});
        	AlertDialog alert = builder.create();
        	// builder.show();
        	alert.show();
        	// End alert dialog here
 
        }
        
        
        // TODO - !!!PUT THIS ALL IN A SEPARATE METHOD!!!
        // Populate field with default values from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        // This line is part of the new code that allows things to be updated when preferences/settings
        // are changed. We had to also 'implement OnSharedPreferenceChangeListener' on the
        // main class too.
        // The only other code is in the onSharedPreferenceChanged() method way below
        // For more info, can see: http://stackoverflow.com/questions/2633769/android-after-changing-a-preference-a-setting-text-showing-settings-doesnt
        settings.registerOnSharedPreferenceChangeListener(this);

        myPass = settings.getString("settings_password", "default password");
        settings_ip_domain = settings.getString("settings_ipdomain", "123.45.67.89"); // If no settings, default to the ip in the 2nd parameter
        String settings_port = settings.getString("settings_port", "4999"); // If no settings, default to the port in the 2nd parameter
        milsToPauseForCrack = settings.getString("settings_crack_ms", "default milliseconds");
        
        myPass = myPass + "*"; // Add the end-of-password delimiter

        defIPDomain = (EditText) findViewById(R.id.ip_domain);
        defPort = (EditText) findViewById(R.id.port);
        defIPDomain.setText(settings_ip_domain);
        defPort.setText(settings_port);
        
        
        
        connectNet = (Button) findViewById(R.id.button_connect);
    	connectNet.setOnClickListener(this);
    	
    	toggleDoor = (Button) findViewById(R.id.button_toggle_door);
    	toggleDoor.setOnClickListener(this);
    	
    	crackDoor = (CheckBox) findViewById(R.id.crack_door);
    	
    	tvGDStatusText = (TextView) findViewById(R.id.status_text);
    	
    	tvConStatusText = (TextView) findViewById(R.id.connection_status);
    	
    	imgGarageDoorStatus = (ImageView) findViewById(R.id.garage_door_status);
    	
    	myAtomicButtonInteger = new AtomicInteger();
    	
    	
    	// Set a default of 1000ms (1 second) if the user didn't enter anything
    	// for the 'crack garage door' setting
    	if (milsToPauseForCrack.equals("")) {
    		milsToPauseForCrack = "3000";
    	}
        
    }
    
    
    
    
    
 // This was created when using the 'implements OnClickListener' in the class
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v.getId() == R.id.button_connect) {
    		// All code from below goes here!!!!
    		
    		if (!connected) { // IF we're not connected at the moment, then let's connect!
    			serverIpAddress = defIPDomain.getText().toString();
    	        serverPort = Integer.valueOf(defPort.getText().toString()); // ******* BE CAREFUL WITH THIS LINE - IT WILL CAUSE THE APP TO CRASH IF THERE IS NOTHING THERE!!!!!!! *****************
    			if (!serverIpAddress.equals("")) {
    				Thread cThread = new Thread(new ClientThread());
    				cThread.start();
    				connectNet.setText(R.string.connect_button_connecting);
    			}
    		} else { // NEw ELSE STATEMENT FOR SINGLE CONNECT/DISCONNECT BUTTON
    			// NEW CODE FOR SINGLE CONNECT/DISCONNECT BUTTON
    			//Log.d("ClientActivity", "Disconnect button pressed");
        		myAtomicButtonInteger.getAndSet(4);
        		//Log.d("ClientActivity", "(After) tmpAtomic=" + myAtomicButtonInteger.get());
        		toggleDoor.setEnabled(false);
        		connectNet.setText(R.string.connect_button_connect); // Change the text of the connect/disconnect button back to 'Connect'
        		currentStatusOfGarageDoor = "0"; // This needs to be reset because it needs to be updated when reconnecting
        		connected = false;
    		}
    		
    	
    	} else if (v.getId() == R.id.button_toggle_door) {
    		
    		//Log.d("ClientActivity", "Toggle button pressed");
    		
    		if (crackDoor.isChecked()) {
    			// if the crack checkbox is checked
    			// send a 'crack' garage door command
    			myAtomicButtonInteger.getAndSet(3);
    			// Then uncheck the box
    			crackDoor.setChecked(false);
    		} else {
    			// if the crack checkbox is not checked
    			// just send a normal 'toggle' command
    			myAtomicButtonInteger.getAndSet(2);
    		}  	
    	}
    }
    
    
    
    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.settings_menu, menu);
    	return true;
    }
    
   
 
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	// Creates a new intent to open up our settings activity (screen/page)
    	Intent i = new Intent(this, SettingsActivity.class);
    	startActivity(i);
    	
    	//Is the following line messing with our starting a new class?
    	//(No reason to believe it is, just keep in mind in case of trouble)
    	return super.onOptionsItemSelected(item);
    }
    

    
    
    
    
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        myPass = settings.getString("settings_password", "default password");
        settings_ip_domain = settings.getString("settings_ipdomain", "123.45.67.89"); // If no settings, default to the ip in the 2nd parameter
        String settings_port = settings.getString("settings_port", "4999"); // If no settings, default to the port in the 2nd parameter
        milsToPauseForCrack = settings.getString("settings_crack_ms", "default milliseconds");
        
        myPass = myPass + "*"; // Add the end-of-password delimiter

        defIPDomain = (EditText) findViewById(R.id.ip_domain);
        defPort = (EditText) findViewById(R.id.port);
        defIPDomain.setText(settings_ip_domain);
        defPort.setText(settings_port);
        
        // *****************BELOW IS NEW 11-29-13
		newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_unknown);
    	imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
    	// *****************END NEW 11-29-13
    }
    
    
    
    
    
    
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    
    
    
    
    
    
    
    public class ClientThread implements Runnable {

    	public void run() {
    		try {
    			
    			//Log.d("ClientActivity", "C: Connecting...");
    			
    			Socket socket = new Socket();
    			SocketAddress adr = new InetSocketAddress(serverIpAddress, serverPort);
    			socket.connect(adr, 5000); // 2nd parameter is timeout!!!

    			connected = true;
    			
    			while (connected) {
    					
    			// New handler to update connect button to say 'disconnect' because we are connected at this point!
				handler.post(new Runnable() {
					public void run() {
						connectNet.setText(R.string.connect_button_disconnect);	
					}
				});
				
    				
    				
    			try {
    				//Log.d("ClientActivity", "C: Sending password.");
    				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    				// where you issue the commands
    				out.println(myPass);
    				//Log.d("ClientActivity", "C: Password," + myPass + ", sent.");
    				
    				
    				// Not sure if this should go here, but we updated the connection status and enable the toggle door button here
    				handler.post(new Runnable() {
						public void run() {
							tvConStatusText.setText(R.string.connection_status_connected);
							toggleDoor.setEnabled(true);
						}
					});
    				
    				
    				
    				BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    				final StringBuilder total = new StringBuilder(); // 'final' was added to allow this string to be able to be used below
    				//String line;
    				while ((line = r.readLine()) != null) {
    					total.append(line); // This probably isn't needed, right? We only want one line at a time
    					
    					/* *** ALL Code should go in here ??? ****
    				
    					Because it will catch everything , until the socket connection is closed???
    					See: http://stackoverflow.com/questions/2500107/how-should-i-read-from-a-buffered-reader 
    					and: http://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html

    					Note: We should read 'line' instead of 'total' as we are currently below
    					(out of this loop... - handler.post...)
    				
    					*/
    					

    					if (line.toString().contentEquals("incorrect password")) {
    						// Incorrect password stuff here
    						connected = false;
    						socket.close();
    						//Log.d("ServerActivity", "Incorrect Password block ran");
    						handler.post(new Runnable() {
								public void run() {
									tvConStatusText.setText(R.string.connection_status_disconnected);
									tvGDStatusText.setText(R.string.status_text_unknown);
									connectNet.setText(R.string.connect_button_connect);
									toggleDoor.setEnabled(false);
									Toast.makeText(MainActivity.this, "Incorrect password. Please check your password and try again.", Toast.LENGTH_SHORT).show();
								}
							});
    						break;
    					}
    					
    				// Had to add this if statement because the status wasn't getting updated
    				// upon an immediate reconnection
    				if (connected) {
    					// We only want to execute the below code if we received a garage door status
    					// This allows us capture other kind of data below this if block later on
    					// as a feature update later
    					if (line.toString().contentEquals("status:open") || line.toString().contentEquals("status:closed")) {
    					
    						newStatusOfGarageDoor = line.toString();
    						//Log.d("ServerActivity", "newStatusOfGarageDoor: " + newStatusOfGarageDoor);
    						//Log.d("ServerActivity", "currentStatusOfGarageDoor: " + currentStatusOfGarageDoor);
    						
    						
    						// If the new and old garage statuses don't match, let's do some stuff
    						// like update the UI
    						if (!newStatusOfGarageDoor.equals(currentStatusOfGarageDoor)) {
    							handler.post(new Runnable() {
    								public void run() {
    									
    									if (newStatusOfGarageDoor.equals("status:open")) {
    										tvGDStatusText.setText(R.string.status_text_open);
    										toggleDoor.setText(R.string.toggle_button_close);
    										crackDoor.setEnabled(false);
    										
    										// *****************BELOW IS NEW 11-29-13
    										newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_open);
    								    	imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
    								    	// *****************END NEW 11-29-13
    										
    										//Log.d("ServerActivity", "gdStateChanged!");
    									} else if (newStatusOfGarageDoor.equals("status:closed")) {
    										tvGDStatusText.setText(R.string.status_text_closed);
    										toggleDoor.setText(R.string.toggle_button_open);
    										crackDoor.setEnabled(true);
    										
    										// *****************BELOW IS NEW 11-29-13
    										newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_closed);
    								    	imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
    								    	// *****************END NEW 11-29-13
    								    	
    										//Log.d("ServerActivity", "gdStateChanged!");
    									} else {
    										
    										// Temporary..just to see if something else comes in
    										tvGDStatusText.setText(newStatusOfGarageDoor);
    										// Temporary..just to see if something else comes in
    										
    									}
    									
    									currentStatusOfGarageDoor = newStatusOfGarageDoor;

    								}
    							});
    						} // End if the new and current garage statuses don't match
    					
    					
    					} // End if a status request was received
    					
    				} // End of new if statement
    				// Had to add this if statement because the status wasn't getting updated
    				// upon an immediate reconnection
    					
    					
    					
    					
    					
    					//Log.d("Server response", line.toString());
    					
    					//Log.d("ClientActivity", "(Still?) tmpAtomic=" + myAtomicButtonInteger.get());
    					
    					
    					if (myAtomicButtonInteger.get() == 2) {
    						out.println("cmd=gdToggle@");
    						//Log.d("ClientActivity", "Sent: cmd=gdToggle@");
    						myAtomicButtonInteger.set(0);
    					} else if (myAtomicButtonInteger.get() == 3) {
    						out.println("cmd=gdCrack:" + milsToPauseForCrack + "@");
    						//Log.d("ClientActivity", "Sent: cmd=gdCrack:" + milsToPauseForCrack + "@");
    						myAtomicButtonInteger.set(0);
    					} else if (myAtomicButtonInteger.get() == 4) {
    						out.println("disconnect@");
    						//Log.d("ClientActivity", "Sent: disconnect@");
    						connected = false;
    						socket.close();
    						myAtomicButtonInteger.set(0);
    						// Use a handler to update the connection status to show that we're disconnected now
    						handler.post(new Runnable() {
								public void run() {
									tvConStatusText.setText(R.string.connection_status_disconnected);
									tvGDStatusText.setText(R.string.status_text_unknown);
									crackDoor.setChecked(false);
									crackDoor.setEnabled(false);
									
									// *****************BELOW IS NEW 11-29-13
									newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_unknown);
							    	imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
							    	// *****************END NEW 11-29-13
								}
							});

    						break; // If the disconnect button was pressed, break out of the current 'while' loop (disconnect)
    					}
    		 

    				}
    				
    			
    			
    				
    				
    			} catch (Exception e) {
    				Log.e("ClientActivity", "S: Error", e);
    			}
    			
    			}
    			
    			
    				socket.close();
    				//Log.d("ClientActivity", "C: Closed.");
    				
    				
    				
    				
    			// Timeout catch
				} catch (java.net.SocketTimeoutException ste) {
					// Do something here to notify user
					connected = false;
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, "Connection timed out! Please check your internet connection, and address/port settings", Toast.LENGTH_LONG).show();
							connectNet.setText(R.string.connect_button_connect);
							// Change text on button to 'CONNECT' so user can try different ip/port!
						}
					});
					
				//}
				// End timeout catch
    				
    				
    			} catch (Exception e) {
    				Log.e("ClientActivity", "C: Error", e);
    				connected = false;
    				
    			}
    		
    		
    			


    	} // End of run()
    	

    } // End of class ClientThread
    
    
    
    
    // If another app comes into the foreground and interrupts our app
    @Override
    protected void onStop() {
    	super.onStop();
    	connectNet.setText(R.string.connect_button_connect);
    	tvGDStatusText.setText(R.string.status_text_unknown);
		toggleDoor.setEnabled(false);
		myAtomicButtonInteger.getAndSet(4);
		newStatusOfGarageDoor = "";
		currentStatusOfGarageDoor = "";
		crackDoor.setChecked(false);
		crackDoor.setEnabled(false);
		
		// *****************BELOW IS NEW 11-29-13
		newGDStatusImage = getResources().getDrawable(R.drawable.ic_inapp_status_unknown);
    	imgGarageDoorStatus.setImageDrawable(newGDStatusImage);
    	// *****************END NEW 11-29-13
    	

    }
    
    
    
    
    @Override
    protected void onResume()	{
    	super.onResume();
    	
    	// Reset our 'myAtomicButtonInteger' back to '0' when we come back into the app after being
    	//  Disturbed/paused/stopped
    	myAtomicButtonInteger.getAndSet(0);
    	
    	// Not sure why we had to add these two following lines yet, but the reason we did is because:
    	// If we were connected, and pressed the 'home' button on the device, then came back into the app
    	//  and reconnected, it wouldn't display the garage door status
    	newStatusOfGarageDoor = "";
		currentStatusOfGarageDoor = "";
    }
    

    
    

    
    
    
    
} // End of MainActivity Class