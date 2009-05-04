package net.exiva.twitpic;

import danger.app.Application;
import danger.app.AppResources;
import danger.app.Bundle;
import danger.app.Event;
import danger.app.EventType;
import danger.app.SettingsDB;
import danger.app.SettingsDBException;

import danger.audio.Meta;

import danger.crypto.MD5;

import danger.mime.Base64;

import danger.net.HTTPConnection;
import danger.net.HTTPTransaction;
import danger.net.HiptopConnection;

import danger.ui.AlertWindow;
import danger.ui.DialogWindow;
import danger.ui.NotificationManager;
import danger.ui.MarqueeAlert;

import danger.util.StringUtils;
import danger.util.MetaStrings;

import java.io.StringReader;
import java.io.IOException;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import danger.util.DEBUG;

public class twitpic extends Application implements Resources, Commands {
	public boolean firstLaunch = true;
	private static boolean mIsAppForeground;
	public static int callHome;
	MarqueeAlert mMarquee;
	public static SettingsDB twitpicPrefs;
	static private String error, password, postStatus, tagName, text, url, username, className, source;
	static public twitpicView mWindow;
	static public twitpicLoginView mLogin;
	AlertWindow aPassword, aWelcome;

	public twitpic() {
		mWindow = twitpicView.create();
		mLogin = twitpicLoginView.create();
		className = getBundle().getClassName();
		mMarquee = new MarqueeAlert("null", Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE),1);
		mLogin.show();
		aWelcome = Application.getCurrentApp().getResources().getAlert(ID_WELCOME, this);
		aPassword = Application.getCurrentApp().getResources().getAlert(ID_PASSWORD_ERROR, this);
	}

	public void launch() {
		firstLaunch=false;
		restoreData();
	}

	public void resume() {
		mIsAppForeground=true;
	}

	public void suspend() {
		mIsAppForeground=false;
	}

	public void restoreData() {
		if (SettingsDB.findDB("twitpicSettings") == false) {
			twitpicPrefs = new SettingsDB("twitpicSettings", true);
			twitpicPrefs.setAutoSyncNotifyee(this);
			callHome();
			aWelcome.show();
		} else {
			twitpicPrefs = new SettingsDB("twitpicSettings", true);
			username = twitpicPrefs.getStringValue("username");
			password = twitpicPrefs.getStringValue("password");
			try {
			callHome = twitpicPrefs.getIntValue("callHome");
			} catch (SettingsDBException exception) {}
			mLogin.setLogin(username, password);
			checkAuth(username, password);
			if (callHome != 1) {
				callHome();
			}
		}
	}

	public static void callHome() {
		HTTPConnection.get("http://static.tmblr.us/hiptop/hiptopLog2.php?a="+className+"&n="+MetaStrings.get(MetaStrings.ID_PARTNER_NAME)+"&d="+MetaStrings.get(MetaStrings.ID_DEVICE_MODEL)+"&b="+MetaStrings.get(MetaStrings.ID_BRAND_NAME)+"&u="+HiptopConnection.getUserName(), null, (short) 0, 99);
		twitpicPrefs.setIntValue("callHome", 1);
	}

	public static void storeLogin(String inUser, String inPass) {
		twitpicPrefs.setStringValue("username", inUser);
		twitpicPrefs.setStringValue("password", inPass);
		mLogin.setLogin(inUser,inPass);
		username = inUser;
		password = inPass;
	}

	public static void checkAuth(String inUser, String inPass) {
		username = inUser;
		password = inPass;
		mLogin.disableInput();
		String twitterLogin = Base64.encode((inUser+":"+inPass).getBytes());
		HTTPConnection.get("http://twitter.com/account/verify_credentials.json", "Authorization: Basic "+twitterLogin, (short) 0, 2);
	}

	public static void postEntry(String body, byte[] oJPEG, String filename, int size) {
		if (body.equals("")) {
			url = "http://twitpic.com/api/upload";
		} else {
			url = "http://twitpic.com/api/uploadAndPost";
		}
		source = "hiptop";
		byte[] start = new String("--AaB03x\r\n" +
		   					"Content-Disposition: form-data; name=\"username\"\r\n" +
							"\r\n" +
							username+"\r\n" +
							"--AaB03x\r\n" +
							"Content-Disposition: form-data; name=\"password\"\r\n" +
							"\r\n" +
							password+"\r\n" +
							"--AaB03x\r\n" +
							"Content-Disposition: form-data; name=\"message\"\r\n" +
							"\r\n" +
							body+"\r\n" +
							"--AaB03x\r\n" +
							"Content-Disposition: form-data; name=\"source\"\r\n" +
							"\r\n" +
							source+"\r\n" +
							"--AaB03x\r\n" +
							"content-disposition: form-data; name=\"media\"; filename=\""+filename+"\"\r\n" +
							"Content-Type: image/jpeg\r\n" +
							"\r\n").getBytes();
		byte[] end = new String("\r\n" +
							"--AaB03x--").getBytes();
												
		byte[] body2 = new byte[start.length + oJPEG.length + end.length];
		System.arraycopy(start, 0, body2, 0, start.length);
		System.arraycopy(oJPEG, 0, body2, start.length, oJPEG.length);
		System.arraycopy(end, 0, body2, start.length + oJPEG.length, end.length);
		
		String headers = "Content-type: multipart/form-data, boundary=AaB03x\r\n" +
						"Content-length: " + body2.length;

		HTTPConnection.post(url, headers, body2, (short) 0, 1);
	}

	public void parsePostResponse(String response) {
		StringReader sr = new StringReader(response);
		String postStatus, postMessage, tagName, text;
		KXmlParser xpp = new KXmlParser();
		try {
			xpp.setInput(sr);
		}
		catch (XmlPullParserException ex) { }

		try {
			int eventType = xpp.getEventType();
 			while (eventType != xpp.END_DOCUMENT) {
				if (eventType == xpp.START_TAG) {
					tagName = xpp.getName();
					if (tagName.equals("rsp")) {
						postStatus = xpp.getAttributeValue(0);
						if (postStatus.equals("ok")) {
							//Close the sending post window, and clear the text.
							mWindow.clearPostWindow();
							if (!this.mIsAppForeground) { 
								mMarquee.setText("Picture successfully posted.");
								NotificationManager.marqueeAlertNotify(mMarquee);
							}
							Meta.play(Meta.BEEP_ACTION_SUCCESS);
							mWindow.clear();
						} else if (postStatus.equals("fail")) {
							mWindow.clearPostWindow();
						}
					}
					eventType = xpp.next();
					if (eventType == xpp.TEXT) {
						text = xpp.getText();
						if (tagName.equals("mediaurl")) {
							mWindow.setURL(text);
						}	
					}
				}
				eventType = xpp.next();
				if (eventType == 2) {
					text = xpp.getText();
					tagName = xpp.getName();
					if (tagName.equals("err")) {
							if (xpp.getAttributeValue(0).equals("1001")) {
								mLogin.show();
								mLogin.enableInput();
							} else {
								AlertWindow postedAlert = new AlertWindow("Twitpic", xpp.getAttributeValue(1), false);
								postedAlert.setWindowStyle(DialogWindow.APP_ALERT_STYLE);
								postedAlert.show();
						}
					}
				}
			}
		}
		catch (XmlPullParserException ex) { }
		catch (IOException ioex) { }
	}

	
	public void networkEvent(Object object) {
		if (object instanceof HTTPTransaction) {
			HTTPTransaction t = (HTTPTransaction) object;
			if((t.getSequenceID() == 1)) {
				if (t.getResponse() == 200) {
					parsePostResponse(t.getString());
				}
			}
			if (t.getSequenceID() == 2) {
				if (t.getResponse() != 200) {
					if (!this.mIsAppForeground) { 
						mMarquee.setText("Login error. Check your username and password,");
						NotificationManager.marqueeAlertNotify(mMarquee);
					}
					NotificationManager.playErrorSound();
					mLogin.show();
					mLogin.enableInput();
					aPassword.show();
				} else if (t.getResponse() == 200) {
					storeLogin(username, password);
					mLogin.hide();
					mLogin.stopThrobber();
					mWindow.show();
				}
			}
			t = null;
		}
	}

	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case Event.EVENT_AUTO_SYNC_DONE: {
				restoreData();
				return true;
			}
		}
		return (super.receiveEvent(e));
	}
}