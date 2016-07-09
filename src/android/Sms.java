package com.jsmobile.plugins.sms;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

/**
 * This class echoes a string called from JavaScript.
 */
//Changed from CDNsms to sms
public class Sms extends CordovaPlugin {
    private static final String SMS_GENERAL_ERROR = "SMS_GENERAL_ERROR";
    private static final String NO_SMS_SERVICE_AVAILABLE = "NO_SMS_SERVICE_AVAILABLE";
    private static final String SMS_FEATURE_NOT_SUPPORTED = "SMS_FEATURE_NOT_SUPPORTED";
    private static final String SENDING_SMS_ID = "SENDING_SMS";
    private static final String SMS_GENERAL_EXCEPTION = "SMS_GENERAL_EXCEPTION";
    
	public static final String SMS_URI_ALL = "content://sms/";
	public static final String SMS_URI_INBOX = "content://sms/inbox";
	public static final String SMS_URI_SEND = "content://sms/sent";
	public static final String SMS_URI_DRAFT = "content://sms/draft";
	public static final String SMS_URI_OUTBOX = "content://sms/outbox";
	public static final String SMS_URI_FAILED = "content://sms/failed";
	public static final String SMS_URI_QUEUED = "content://sms/queued";
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("sendMessage")) {
            String phoneNumber = args.getString(0);
            String message = args.getString(1);
            
            boolean isSupported = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
            
            if (! isSupported) {
                JSONObject errorObject = new JSONObject();
                
                errorObject.put("code", SMS_FEATURE_NOT_SUPPORTED);
                errorObject.put("message", "SMS feature is not supported on this device");
                
                callbackContext.sendPluginResult(new PluginResult(Status.ERROR, errorObject));
                return false;
            }
            
            this.sendSMS(phoneNumber, message, callbackContext);
            
            return true;
        }else if (action.equals("readMessage")) {
            String type = args.getString(0);
			try {
				JSONObject messages;
				if(type == "sms"){
					messages = readSMS(callbackContext);
				}else{
					messages = readWA(callbackContext);
				}
				callbackContext.sendPluginResult(new PluginResult(Status.OK, messages));
				return true;
            }  catch (Exception Ex) {
                JSONObject errorObject = new JSONObject();                
                errorObject.put("code", SMS_GENERAL_EXCEPTION);
                errorObject.put("message", "Got Exception " + Ex.getMessage());
				callbackContext.sendPluginResult(new PluginResult(Status.ERROR, errorObject));
            }
        }else if (action.equals("isWhatsAppInstalled")) {
            try {
                JSONObject messages = isWhatsAppInstalled(callbackContext);
				callbackContext.sendPluginResult(new PluginResult(Status.OK, messages));
				return true;
            }  catch (Exception Ex) {
                JSONObject errorObject = new JSONObject();                
                errorObject.put("code", SMS_GENERAL_EXCEPTION);
                errorObject.put("message", "Got Exception " + Ex.getMessage());
				callbackContext.sendPluginResult(new PluginResult(Status.ERROR, errorObject));
            }
        }
        
        return false;
    }
	
    private JSONObject isWhatsAppInstalled(final CallbackContext callbackContext) throws JSONException {
		JSONObject data = new JSONObject();
		JSONObject obj = new JSONObject();
		boolean installed = false;
		PackageManager pm = getApplicationContext().getPackageManager();
		
		try {
			pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
			installed =  true;
		} catch (NameNotFoundException e) {
			installed = false;
		}
		obj.put("installed", installed);
		data.put("whatsapp", obj);
		return data;
	}

	private JSONObject readWA(final CallbackContext callbackContext) throws Exception{	
		JSONObject data = new JSONObject();
		WhatsAppDBHelper db = new WhatsAppDBHelper("msgstore", getApplicationContext());
		db.openDataBase();		
		Cursor cursor = db.query("SELECT FROM ", new String[] {});	
		
		if (!cur.moveToFirst()) {
			db.close();
			return data;
		}
		
		while (cur.moveToNext()) {
			JSONObject obj = new JSONObject();
            obj.put("id",cur.getColumnIndex("key_id"));
            obj.put("number",cur.getColumnIndex("key_remote_jid")).replace("", "@s.whatsapp.net");
            obj.put("date",cur.getColumnIndex("timestamp"));
            obj.put("status",cur.getColumnIndex("status"));
            obj.put("type",cur.getColumnIndex("origin"));
            obj.put("body",cur.getColumnIndex("data"));

            String name = getContact(obj.getString("number"));
            if(!name.equals("")){
                obj.put("name",name);
            }
            smsList.put(obj);
        }
		
        db.close();
		return data;
	}
	
    private JSONObject readSMS(final CallbackContext callbackContext) throws JSONException {
        JSONObject data = new JSONObject();
        JSONArray smsList = new JSONArray();
        data.put("messages", smsList);
        Uri uriSMSURI = Uri.parse(SMS_URI_INBOX);
        Cursor cur = getContentResolver().query(uriSMSURI, (String[])null, "", (String[])null, null);
        
        if (!cur.moveToFirst()) {
			cur.close();
			return data;
		}
		
        while (cur.moveToNext()) {
			JSONObject obj = new JSONObject();
            obj.put("id",cur.getColumnIndex("_id"));
            obj.put("number",cur.getColumnIndex("address"));
            obj.put("date",cur.getColumnIndex("date"));
            obj.put("status",cur.getColumnIndex("status"));
            obj.put("type",cur.getColumnIndex("type"));
            obj.put("body",cur.getColumnIndex("body"));

            String name = getContact(obj.getString("number"));
            if(!name.equals("")){
                obj.put("name",name);
            }
            smsList.put(obj);
        }
        cur.close();
		return data;
	}
	
    private void sendSMS(String phoneNumber, String message, final CallbackContext callbackContext) throws JSONException {
        PendingIntent sentPI = PendingIntent.getBroadcast(getActivity(), 0, new Intent(SENDING_SMS_ID), 0);

        getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                case Activity.RESULT_OK:
                    callbackContext.sendPluginResult(new PluginResult(Status.OK, "SMS message is sent successfully"));
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    try {
                        JSONObject errorObject = new JSONObject();
                        
                        errorObject.put("code", NO_SMS_SERVICE_AVAILABLE);
                        errorObject.put("message", "SMS is not sent because no service is available");
                        
                        callbackContext.sendPluginResult(new PluginResult(Status.ERROR, errorObject));   
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    break;
                default:
                    try {
                        JSONObject errorObject = new JSONObject();
                        
                        errorObject.put("code", SMS_GENERAL_ERROR);
                        errorObject.put("message", "SMS general error");
                        
                        callbackContext.sendPluginResult(new PluginResult(Status.ERROR, errorObject));
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    
                    break;
                }
            }
        }, new IntentFilter(SENDING_SMS_ID));

        SmsManager sms = SmsManager.getDefault();
        
        sms.sendTextMessage(phoneNumber, null, message, sentPI, null);
    }
    
    private Activity getActivity() {
        return this.cordova.getActivity();
    }

	private String getContact(String number){
	    Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null,null,null,null);
	    String returnName = "";
	    if(cur.getCount() > 0){
	        while(cur.moveToNext()){
				String id =  cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				String name =  cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

				if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor pcur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
									ContactsContract.CommonDataKinds.Phone.NUMBER + "=?",new String[]{number},null);
					int numindex = pcur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA);
					if(pcur.moveToFirst()){
	                    String dbNum = pcur.getString(numindex);
	                    if(dbNum.equals(number)){
							returnName =  name;
	                    }
	                }
				}
	        }
	    }
    	return returnName;
	}
	
	private ContentResolver getContentResolver(){
	    return getActivity().getContentResolver();
	}
	
	private Context getApplicationContext(){
	    return getActivity().getApplicationContext();
	}
}

class WhatsAppDBHelper extends SQLiteOpenHelper{
	private String path = "/data/data/com.whatsapp/databases/";
	private String db_name = "";
	private SQLiteDatabase myDataBase;  
    private final Context myContext;
	
	public WhatsAppDBHelper(String name, Context context) {
		this.db_name = name;
		super(context, this.db_name, null, 1);
        this.myContext = context;
	}
	
	public void openDataBase() throws SQLException{
        String myPath = this.path + this.db_name;
    	this.myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY); 
    }
	
	public Cursor query(String query, String[] whereArgs){
		if(this.myDataBase != null)
			retrun this.myDataBase.rawQuery(query, whereArgs);
		return null;
	}
	
	@Override
	public synchronized void close() {
		if(myDataBase != null)
			myDataBase.close();
		super.close(); 
	}
}
