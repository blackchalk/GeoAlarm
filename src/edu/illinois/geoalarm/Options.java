package edu.illinois.geoalarm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ToggleButton;

/**
 * This Activity is used to set various user options
 * for the GeoAlarm app
 * @author deflume1
 *
 */

public class Options extends Activity 
{		
	GeoAlarmDB database;
	TextView sessionUsageTxView;
	TextView sessionUsageRxView;
	TextView totalUsageTxView;	 
	TextView totalUsageRxView;
	EditText ringLengthEdit;
	EditText vibrateLengthEdit;
	Spinner backgroundColorSelectSpinner;
	ToggleButton toggleSplashScreenButton;
	ProgressDialog updateProgressDialog;

	private static final String DB_PATH = "/data/data/edu.illinois.geoalarm/databases/";	
	private static String DB_NAME = "geoAlarmDB.sqlite";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);        
		setContentView(R.layout.options);

		sessionUsageTxView = (TextView)findViewById(R.id.sessionDataTxShow);
		sessionUsageRxView = (TextView)findViewById(R.id.sessionDataRxShow);
		totalUsageTxView = (TextView)findViewById(R.id.totalDataTxShow);
		totalUsageRxView = (TextView)findViewById(R.id.totalDataRxShow);       
		backgroundColorSelectSpinner = (Spinner)findViewById(R.id.backgroundColorSelectSpinner);
		ringLengthEdit = (EditText)findViewById(R.id.ringtoneLengthEditText);
		vibrateLengthEdit = (EditText)findViewById(R.id.vibrationLengthEditText);
		toggleSplashScreenButton = (ToggleButton)findViewById(R.id.toggleSplashScreenButton);
		SharedPreferences settings = getSharedPreferences("GeoAlarm", Activity.MODE_PRIVATE);

		toggleSplashScreenButton.setChecked(settings.getBoolean("splash_screen", false));
		ringLengthEdit.setText(String.valueOf(settings.getInt("ring_length", 3)));
		vibrateLengthEdit.setText(String.valueOf(settings.getInt("vibrate_length", 3))); 

		updateProgressDialog = new ProgressDialog(Options.this);
		updateProgressDialog.setMessage("Updating GeoAlarm Database");
		updateProgressDialog.setIndeterminate(false);
		updateProgressDialog.setMax(100);
		updateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

	}

	@Override 
	public void onStart()
	{				
		populateBackgroundColorSelectSpinner();
		setBackgroundColorSelectSpinnerEventListeners();
		setToggleButtonEventListeners();
		super.onStart();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		loadDatabase();
		showUsageData();	
		
		// Update database if first run
		SharedPreferences settings = getSharedPreferences("GeoAlarm", Activity.MODE_PRIVATE);
		View v = findViewById(R.id.optionsTopLayout);
		v.setBackgroundColor(settings.getInt("color_value", R.color.Blue));
		boolean firstRun = settings.getBoolean("geo_alarm_first_run", true);
		if(firstRun)
		{			
				if(isOnline())
				{
					onClickUpdateDatabase(null);		
				}
		}
		
	}	

	@Override
	public void onStop()
	{		
		database.close();
		super.onStop();
	}
	
	/**
	 * Checks whether we have a network connection
	 * @return true if connected, false otherwise
	 */
	public boolean isOnline() 
	{
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

	/**
	 * Helper function to load the database
	 */
	public void loadDatabase()
	{
		// Instantiate the database
		database = new GeoAlarmDB(this.getApplicationContext());

		// Check the custom SQLite helper functions that load existing DB
		try
		{
			database.createDataBase();
		}
		catch (IOException e)
		{
			throw new Error("Unable to create/find database");
		}	

		// Open the SQLite database
		try
		{
			database.openDataBase();
		}
		catch (SQLException sql)
		{
			throw new Error("Unable to execute sql in: " + sql.toString());
		}
	}    


	/**
	 * This function updates the usage data labels when its launched.  It uses the data stored in the UsageTable of GeoAlarmDB
	 */
	public void showUsageData()
	{
		long numBytesLastReceivedSession =  database.getBytes(GeoAlarmDB.DB_RX_SESSION);
		long numBytesLastTransmittedSession =  database.getBytes(GeoAlarmDB.DB_TX_SESSION);
		long numBytesReceived = database.getBytes(GeoAlarmDB.DB_RX);
		long numBytesTransmitted = database.getBytes(GeoAlarmDB.DB_TX);		
		database.close();

		double numMegaBytesReceivedSession = ((double) numBytesLastReceivedSession) / 1E6;		
		double numMegaBytesTransmittedSession = ((double) numBytesLastTransmittedSession) / 1E6;
		double numMegaBytesReceived = ((double) numBytesReceived) / 1E6;
		double numMegaBytesTransmitted = ((double) numBytesTransmitted) / 1E6;

		DecimalFormat df = new DecimalFormat("#.###");
		String displaySessionRx = " " + df.format(numMegaBytesReceivedSession) + " MB";
		String displaySessionTx = " " + df.format(numMegaBytesTransmittedSession) + " MB";
		String displayTotalRx = " " + df.format(numMegaBytesReceived) + " MB";
		String displayTotalTx = " " + df.format(numMegaBytesTransmitted) + " MB";

		sessionUsageRxView.setText(displaySessionRx);
		sessionUsageTxView.setText(displaySessionTx);		
		totalUsageRxView.setText(displayTotalRx);	   
		totalUsageTxView.setText(displayTotalTx);
		sessionUsageRxView.getRootView().invalidate();
	}
	
	/**
	 * Updates the stored usage data with the most up-to-date numbers
	 */
	public void updateUsageData()
	{
		if(database != null)
		{		
			long numBytesLastReceivedSession =  database.getBytes(GeoAlarmDB.DB_RX_SESSION);
			long numBytesLastTransmittedSession =  database.getBytes(GeoAlarmDB.DB_TX_SESSION);
			long numBytesReceived = database.getBytes(GeoAlarmDB.DB_RX);
			long numBytesTransmitted = database.getBytes(GeoAlarmDB.DB_TX);
			long numBytesReceivedDelta = TrafficStats.getUidRxBytes(Process.myUid()) - database.getBytes(GeoAlarmDB.DB_RX_TARE_SESSION) - numBytesLastReceivedSession;
			long numBytesTransmittedDelta = TrafficStats.getUidTxBytes(Process.myUid()) - database.getBytes(GeoAlarmDB.DB_TX_TARE_SESSION) - numBytesLastTransmittedSession;
		
			database.setBytes(GeoAlarmDB.DB_RX_SESSION, numBytesLastReceivedSession + numBytesReceivedDelta);
			database.setBytes(GeoAlarmDB.DB_TX_SESSION, numBytesLastTransmittedSession + numBytesTransmittedDelta);
			database.setBytes(GeoAlarmDB.DB_RX, numBytesReceived + numBytesReceivedDelta);
			database.setBytes(GeoAlarmDB.DB_TX, numBytesTransmitted + numBytesTransmittedDelta);			
		}
	}

	/**
	 * This function populates the color select spinner
	 */
	public void populateBackgroundColorSelectSpinner()
	{	   
		String[] colorList = this.getResources().getStringArray(R.array.color_array);		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getBaseContext(), android.R.layout.simple_spinner_item, colorList);		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		backgroundColorSelectSpinner.setAdapter(adapter);
	}

	/**
	 * This function sets the event listener for the color select spinner, which sets the color and saves it when clicked
	 */
	public void setBackgroundColorSelectSpinnerEventListeners()
	{
		/* Set a new event listener for the Spinner item selection */
		backgroundColorSelectSpinner.setOnItemSelectedListener(new OnItemSelectedListener() 
		{    
			/* Implement the onItemSelected method to handle item selections */
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) 
			{   				
				String selectedColor = (String) backgroundColorSelectSpinner.getSelectedItem();

				int color = Color.parseColor(selectedColor.toLowerCase());
				SharedPreferences settings = getSharedPreferences("GeoAlarm", Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("color_value", color);
				editor.putString("color_name", selectedColor);   		
				editor.commit();		   	

				View v = findViewById(R.id.optionsTopLayout);
				View root = v.getRootView();
				root.setBackgroundColor(color);
			}

			/* We do nothing here.  May want to change behavior so the last selected item behavior is used */
			public void onNothingSelected(AdapterView<?> parentView) 
			{
				// do nothing
			}

		});
	}

	/**
	 * Method called when Save button is clicked, writes preferences
	 */
	public void saveButton(View view)
	{
		SharedPreferences settings = getSharedPreferences("GeoAlarm", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		int ringLength = 0;
		int vibrateLength = 0;

		try
		{
			ringLength = Integer.parseInt(ringLengthEdit.getText().toString());
			vibrateLength = Integer.parseInt(vibrateLengthEdit.getText().toString());
		}
		catch (NumberFormatException ex)
		{
			ex.printStackTrace();
			ringLength = 3;
			vibrateLength = 3;
		}

		if(ringLength < 0)
		{
			ringLength = 3;
		}

		if(vibrateLength < 0)
		{
			vibrateLength = 3;
		}

		editor.putInt("ring_length", ringLength);
		editor.putInt("vibrate_length", vibrateLength);
		editor.commit();

		ringLengthEdit.setText(String.valueOf(ringLength));
		vibrateLengthEdit.setText(String.valueOf(vibrateLength));
	}

	/**
	 * Method called to register event listener for toggle button
	 */
	public void setToggleButtonEventListeners()
	{
		toggleSplashScreenButton.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) 
			{
				SharedPreferences settings = getSharedPreferences("GeoAlarm", Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("splash_screen", isChecked);
				editor.commit();
			}

		});
	} 	

	/**
	 * Method called when user clicks on update database button.
	 * @param view The button that's clicked clicked
	 */
	public void onClickUpdateDatabase(View view)
	{
		if(isOnline())
		{
			DownloadNewDatabase download = new DownloadNewDatabase(this);  	
			download.execute("http://deflume1.projects.cs.illinois.edu/geoAlarmDB.sqlite");
		}
		else
		{
			AlertDialog.Builder failureBuilder = new AlertDialog.Builder(this);
			failureBuilder.setMessage("Network connection failure! GeoAlarm must be connected to the internet to update the database!");
			failureBuilder.setTitle("Sorry!");
			failureBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) 
				{
					// do nothing
				}
				
			});
			AlertDialog failure = failureBuilder.create();
			failure.show();
		}
	}

	private class DownloadNewDatabase extends AsyncTask<String, Integer, Integer> 
	{	
		Context mContext;
		final int DOWNLOAD_OK = 0;
		final int ERROR_URL = 1;
		final int ERROR_IO = 2;   
		final String tempFileName = "/data/data/edu.illinois.geoalarm/dbdownload.temp";
		int fileSize;
		
		long numBytesLastReceivedSession;
		long numBytesLastTransmittedSession;
		long numBytesReceived;
		long numBytesTransmitted;
		long numBytesReceivedTare;
		long numBytesTransmittedTare;

		public DownloadNewDatabase(Context taskContext)
		{		
			super();	
			loadDatabase();
			numBytesLastReceivedSession =  database.getBytes(GeoAlarmDB.DB_RX_SESSION);
			numBytesLastTransmittedSession =  database.getBytes(GeoAlarmDB.DB_TX_SESSION);
			numBytesReceived = database.getBytes(GeoAlarmDB.DB_RX);
			numBytesTransmitted = database.getBytes(GeoAlarmDB.DB_TX);
			numBytesReceivedTare = database.getBytes(GeoAlarmDB.DB_RX_TARE_SESSION);
			numBytesTransmittedTare = database.getBytes(GeoAlarmDB.DB_TX_TARE_SESSION);
			mContext = taskContext;
		}

		@Override
		protected Integer doInBackground(String... sUrl)
		{    	   			 
			try
			{
				// open connection to database on server
				URL url = new URL(sUrl[0]);     		
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);
				urlConnection.connect();

				// Delete old temp file
				File tempFile = new File(tempFileName);   	 
				if(tempFile.exists())
				{
					tempFile.delete();   	    			
				}

				OutputStream tempFileOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));

				InputStream inputStream = urlConnection.getInputStream();

				int totalSize = urlConnection.getContentLength();
				int downloadedSize = 0;

				byte[] buffer = new byte[1024];
				int bufferLength = 0;

				while((bufferLength = inputStream.read(buffer)) > 0)
				{
					tempFileOutputStream.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;    
					publishProgress((int) ((downloadedSize * 100) / totalSize));
				}

				tempFileOutputStream.flush();    		
				tempFileOutputStream.close();
				inputStream.close();

				// Check for download error
				if(totalSize != downloadedSize)
				{    			
					if(tempFile.exists())
					{
						tempFile.delete();
					}
					return ERROR_IO;
				}	    		

				fileSize = totalSize;

			}
			catch(MalformedURLException e)
			{
				e.printStackTrace();
				return ERROR_URL;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				return ERROR_IO;
			}

			return DOWNLOAD_OK;    	
		}      

		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			database.close();
			updateProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress)
		{
			super.onProgressUpdate(progress);
			updateProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result)
		{   	    	
			switch(result)
			{
			case DOWNLOAD_OK:
				copyNewDatabase();
				AlertDialog.Builder successBuilder = new AlertDialog.Builder(mContext);
				successBuilder.setMessage("Database successfully updated");
				successBuilder.setTitle("Success!");
				successBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) 
					{						
						// Do nothing
					}
					
				});
				AlertDialog success = successBuilder.create();
				success.show();
				break;
			case ERROR_URL:
				AlertDialog.Builder errorURLbuilder = new AlertDialog.Builder(mContext);
				errorURLbuilder.setMessage("Couldn't reach update server");
				errorURLbuilder.setTitle("URL Error");
				errorURLbuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) 
					{						
						// Do nothing
					}
					
				});
				AlertDialog urlError = errorURLbuilder.create();
				urlError.show();
				updateProgressDialog.dismiss();
				return;				
			case ERROR_IO:
				AlertDialog.Builder errorIObuilder = new AlertDialog.Builder(mContext);
				errorIObuilder.setMessage("A problem was encountered downloading the file");
				errorIObuilder.setTitle("Download Error");
				errorIObuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) 
					{						
						// Do nothing
					}
					
				});
				AlertDialog ioError = errorIObuilder.create();
				ioError.show();
				updateProgressDialog.dismiss();
				return;					   	    		
			}   	   
			updateProgressDialog.dismiss();
			
			loadDatabase();
			database.setupUsageDataTable();
			
			database.setBytes(GeoAlarmDB.DB_RX_SESSION, numBytesLastReceivedSession);
			database.setBytes(GeoAlarmDB.DB_TX_SESSION, numBytesLastTransmittedSession);
			database.setBytes(GeoAlarmDB.DB_RX, numBytesReceived);
			database.setBytes(GeoAlarmDB.DB_TX, numBytesTransmitted);	
			database.setBytes(GeoAlarmDB.DB_RX_TARE_SESSION, numBytesReceivedTare);
			database.setBytes(GeoAlarmDB.DB_TX_TARE_SESSION, numBytesTransmittedTare);
			
			updateUsageData();
			showUsageData();			
			
			SharedPreferences settings = getSharedPreferences("GeoAlarm", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("geo_alarm_first_run", false);
			editor.commit();			
		}

		private void copyNewDatabase()
		{
			try
			{
				//delete old database, and copy new one
				File databaseFile = new File(DB_PATH + DB_NAME);
				if(databaseFile.exists())
				{
					databaseFile.delete();
				}
				mContext.deleteDatabase(DB_NAME);

				File tempFile = new File(tempFileName);   	    		
				InputStream newDatabaseInputStream = new BufferedInputStream(new FileInputStream(tempFile));

				File newDatabaseFile = new File(DB_PATH + DB_NAME);
				OutputStream newDatabaseOutputStream = new BufferedOutputStream(new FileOutputStream(newDatabaseFile));

				int sizeSoFar = 0;

				byte[] buffer = new byte[1024];
				int bufferLength = 0;

				while((bufferLength = newDatabaseInputStream.read(buffer)) > 0)
				{
					newDatabaseOutputStream.write(buffer, 0, bufferLength);
					sizeSoFar += bufferLength;    
				}

				newDatabaseOutputStream.flush();
				newDatabaseOutputStream.close();
				newDatabaseInputStream.close();

				if(tempFile.exists())
				{
					tempFile.delete();
				}

				if(sizeSoFar != fileSize)
				{
					// horrible error
				}

			}
			catch(FileNotFoundException e)
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}   	    	
		}
	}




}
