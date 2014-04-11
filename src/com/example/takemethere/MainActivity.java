package com.example.takemethere;

import java.util.List;

import com.example.takemethere.Location.LocationType;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {

	public int stepCounter;
	public int angle;
	private SensorServiceReceiver sensorReceiverDirection;
	private SensorServiceReceiver sensorReceiverStep;
	private static DatabaseHelper dbHelper = null;
	private static Floor startFloor;
	private static Location startLocation;
	List<Location> locations;	
	ArrayAdapter<Location> adapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		init();
		//Show the input screen to get start location
		startLocation = getStartLocation();
		//Once start location is identified, show the screen with list of locations in that floor
		displayPossibleDestinations();
		//Location endLocation = getEndLocation();
		
		//TODO: Get initial sensor values and start tracking
		
		/*startService(new Intent(this, SensorService.class));
		regsiterBroadCastReceivers();*/
	}
	private void drawPath(Path path) {
		drawLine(path.startPoint.x,path.startPoint.y,path.endPoint.x,path.endPoint.y,Color.BLACK);
	}
	private Route getRoute(Location startLocation, Location endLocation) {
		return dbHelper.getRoute(startLocation, endLocation);
	}
	private Location getEndLocation() {	
		
		Location endLocation = new Location();
		endLocation.floorId = startFloor.id;
		endLocation.id = 2;
		endLocation.locationPoint = new Point();
		endLocation.locationPoint.set(50, 50);
		endLocation.name = "5303";
		return endLocation;
	}
	/**
	 * 
	 */
	private void displayPossibleDestinations() {
		List<Location> possibleDestinations = dbHelper.getPossibleDestinations(startFloor.id);
		//TODO: Show destination list to user and when user selects return selected location
		ListView locationsListView = (ListView) findViewById(R.id.locationsListview);
		int resId = R.layout.locations_row_layout;
		adapter = new LocationsArrayAdapter(this,resId,possibleDestinations);
		locationsListView.setAdapter(adapter);
		LocationClickListener clickListener = new LocationClickListener();
		locationsListView.setOnItemClickListener(clickListener);
	}
	private Location getStartLocation() {
		// TODO Show QR Code screen and get input from the QR Code scanner
		//QR Code will give the location info.
		Location startLocation = getLocationFromQRCode();
		startFloor = dbHelper.getFloor(startLocation.floorId);
		return startLocation;
	}
	private Location getLocationFromQRCode() {
		// TODO Auto-generated method stub
		Location l = new Location();
		l.id=4;
		l.floorId=1;
		l.name="Wean Cafe";
		l.type = LocationType.ENTRY;
		l.locationPoint = new Point();
		l.locationPoint.set(300, 227);
		return l;
	}
	public void init(){
		dbHelper = new DatabaseHelper(this,null);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		db.close();
	}
	private void drawLine(float startX, float startY, float endX, float endY, int color) {
		ImageView imgMap = (ImageView) findViewById(R.id.imgMap);
		//System.out.println(imgMap.getWidth());
		//TODO: Get size of the bitmap
	    Bitmap bmp = Bitmap.createBitmap(500, 900, Config.ARGB_8888);
	    Canvas c = new Canvas(bmp);
	    imgMap.draw(c);

	    Paint paint = new Paint();
	    paint.setColor(color);
	    c.drawLine(startX, startY, endX, endY, paint);
	    imgMap.setImageBitmap(bmp);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void onResume(){   
		super.onResume();
		init();
	}
	/**
	 * Creates and registers two intent filters - for direction and steps update 
	 */
	private void regsiterBroadCastReceivers() {
		IntentFilter directionFilter = new IntentFilter(SensorService.DIRECTION_UPDATE);
		sensorReceiverDirection = new SensorServiceReceiver();
		registerReceiver(sensorReceiverDirection, directionFilter);
		IntentFilter stepsFilter = new IntentFilter(SensorService.STEP_UPDATE);
        sensorReceiverStep = new SensorServiceReceiver();
        registerReceiver(sensorReceiverStep, stepsFilter);
	}

	@Override
	public void onStop(){
		super.onStop();
		System.out.println("Stop");
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		System.out.println("Destroy");
		unregisterReceiver(sensorReceiverDirection);
		unregisterReceiver(sensorReceiverStep);
	}
	public class SensorServiceReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(intent.getAction().equals(SensorService.STEP_UPDATE))
				stepCounter = intent.getIntExtra(SensorService.STEPS, 0);
			else if(intent.getAction().equals(SensorService.DIRECTION_UPDATE))
				angle = intent.getIntExtra(SensorService.ANGLE,0);
			System.out.println(stepCounter + " " + angle);	
			//updateGUI();			
		}
	}
	
	class LocationClickListener implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> parent, View rowView, int position,long id) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<Location> adapter = (ArrayAdapter<Location>) parent.getAdapter();
			Location endLocation = adapter.getItem(position);
			//Once destination is selected ,we will have start and end points => get the route using that
			Route route = getRoute(startLocation,endLocation);
			//Get the list of paths associated with the route draw the paths
			
			ImageView imgMap = (ImageView) findViewById(R.id.imgMap);
			imgMap.setVisibility(View.VISIBLE);
			
			ListView locationsList = (ListView) findViewById(R.id.locationsListview);
			locationsList.setVisibility(View.GONE);
			
			//TODO: Get proper size of the bitmap
		    Bitmap bmp = Bitmap.createBitmap(500, 900, Config.ARGB_8888);
		    Canvas c = new Canvas(bmp);
		    imgMap.draw(c);
		    Paint paint = new Paint();
		    paint.setColor(Color.RED);
		    paint.setStrokeWidth(5);
			for(Path path : route.paths){
				//drawPath(path);
				c.drawLine(path.startPoint.x, path.startPoint.y,path.endPoint.x,path.endPoint.y, paint);
			}
			imgMap.setImageBitmap(bmp);
		}	
	}
}
