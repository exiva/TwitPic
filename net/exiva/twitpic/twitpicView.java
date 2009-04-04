package net.exiva.twitpic;

import danger.app.Application;
import danger.app.Event;
//4.6+
import danger.app.GalleryItemIPCPayload;
import danger.app.GalleryItem;
//Older OS's
import danger.app.PhotoRecord;
import danger.app.PhotoRecordIPCPayload;
import danger.app.IPCMessage;

import danger.ui.AlertWindow;
import danger.ui.Button;
import danger.ui.Bitmap;
import danger.ui.DialogWindow;
import danger.ui.EditText;
import danger.ui.ImageCodec;
import danger.ui.ImageView;
import danger.ui.Menu;
import danger.ui.MenuItem;
import danger.ui.photopicker.PhotoPicker;
import danger.ui.ProgressWindow;
import danger.ui.ScreenWindow;
import danger.ui.Shortcut;
import danger.ui.TextField;
import danger.ui.TextInputAlertWindow;

import danger.util.DEBUG;
import danger.app.IPCMessage;
import danger.app.Registrar;

public class twitpicView extends ScreenWindow implements Resources, Commands {
	AlertWindow tChooser, tClear, tPosting, tError;
	Button tLastPost, tPhoto, tPost;
	ImageView tNoImage, iv;
	MenuItem mClearPhoto, mLastPost, mPost;
	private static Button tStatusButton, tPhotosButton;
	public static boolean mPhotoSelec, isJPEG, menuClear, menuEnable;
	public static byte[] photoData;
	private static EditText bodyField;
	public static int photoSize, width, height;
	public ProgressWindow postProgress;
	public static String photoname, lastPost;

	public twitpicView() {
		tPosting = getApplication().getAlert(ID_POSTING, this);
		tError = getApplication().getAlert(ID_SUBMIT_ERROR, this);
		tClear = getApplication().getAlert(clearAlert, this);
		tChooser = getApplication().getAlert(chooserAlert, this);
	}

	public static twitpicView create() {
		twitpicView me = (twitpicView) Application.getCurrentApp().getResources().getScreen(ID_MAIN_SCREEN, null);
		return me;
	}

	public void onDecoded() {
		tNoImage = (ImageView)this.getDescendantWithID(NO_IMAGE);
		tLastPost = (Button)this.getDescendantWithID(LAST_POST);
		tPost = (Button)this.getDescendantWithID(POST_BUTTON);
		tPhoto = (Button)this.getDescendantWithID(PICTURE_BUTTON);
		bodyField = (EditText) getChildWithID(BODY_TEXT);
		tNoImage.show();
		tLastPost.disable();
		tPost.disable();
		//Enable spellcheck on the body. (4.6+ Only.)
		((EditText)getChildWithID(BODY_TEXT)).setSpellCheckEnabled(true);
	}

	public final void adjustActionMenuState(Menu menu) {
		menu.removeAllItems();
		menu.addFromResource(Application.getCurrentApp().getResources(), ID_MAIN_MENU, this);
		
		Menu urlmenu = menu.getItemWithID(ID_URLMENU).getSubMenu();
		mLastPost = urlmenu.getItemWithID(MENU_LAST_POST);
		mClearPhoto = menu.getItemWithID(ID_MENUCLEAR);
		mPost = menu.getItemWithID(ID_MENUPOST);
		
		if (this.menuEnable) {
			mLastPost.enable();
		}
		if (!menuClear) {
			mClearPhoto.disable();
			mPost.disable();
		}
	}

	public void changeState(int state) {
		//state 0 = button disabled. no photo.
		//state 1 = button enabled. photo.
		switch (state) {
			case 0:
			tPost.disable();
			break;
			case 1:
			tPost.enable();
			break;
		}
	}

	public void showPhotoPicker(Boolean camera) {
		//4.6+
		PhotoPicker p = PhotoPicker.createPicker(true, false);
		//legacy
		// PhotoPicker p = PhotoPicker.createPicker();
		p.setMaxSelectionCount(1);
		p.setTitle("Choose Existing Photo");
		p.setIcon(Application.getCurrentApp().getResources().getBitmap(ID_MARQUEE));
		if (camera) {
			DEBUG.p("Camera starting...");
			p.setStartInCaptureView(true);
		} 
		p.setEvent(this, EVENT_PHOTOS, 0, 0);
		p.show();
	}

	//4.6+
	public void showSelectedPhotos(GalleryItemIPCPayload photos) {
	//legacy devices
	// public void showSelectedPhotos(PhotoRecordIPCPayload photos) {
		for (int i = 0; i < photos.getRecordCount(); i++) {
			//clear the old imageview and photodata.
			if (iv != null) {
				photoData = null;
				iv.hide();
				menuClear = false;
				changeState(0);
			}
			//4.6+
			GalleryItem record = photos.getGalleryItemAt(i);
			//legacy
			// PhotoRecord record = photos.getRecordAt(i);
			photoname = record.getName();
			if (record.getWidth() > 640 && record.getHeight() > 480 || record.getWidth() > 480 && record.getHeight() > 640) {
				int width=record.getWidth()/2;
				int height=record.getHeight()/2;
				DEBUG.p("twitpic: width: "+width+" height: "+height);
				//4.6+
				Bitmap tmp1 = record.getBitmap(width, height);
				//legacy
				// Bitmap tmp1 = record.getDecodedBitmap();
				// Bitmap tmp2 = tmp1.scaleTo(width, height);
				

				byte[] tmp = new byte[width * height + 1000];
				//4.6+
				int len = ImageCodec.encodeJPEG(tmp1, tmp, 65);
				//legacy
				// int len = ImageCodec.encodeJPEG(tmp2, tmp, 65);
				photoData = new byte[len];
				System.arraycopy(tmp, 0, photoData, 0, len);
				photoSize = photoData.length;
			} else {
				//4.6+
				photoData = record.getData();
				photoSize = record.getDataSize();
				//legacy
				// photoData = record.getRawBitmapData();
				// photoSize = record.getRawBitmapDataSize();
			}
			isJPEG = ImageCodec.isJPEG(photoData);
			//4.6+
			iv = new ImageView(record.getBitmap(63,51));
			//legacy
			// iv = new ImageView(record.getThumbnailBitmapWithHints(63,51));
			iv.setPosition(9,147);
			iv.setSize(63,51);
			//4.6+
			iv.setAutoScale(true,true);
			//legacy
			// iv.setAutoScale(true,false);
			addChild(iv);
			tNoImage.hide();
			iv.show();
			menuClear = true;
			changeState(1);
			setFocusedChild(bodyField);
		}
	}

	public void setURL(String url) {
		lastPost = url;
		menuEnable = true;
		tLastPost.enable();
		mLastPost.enable();
	}

	public void clear() {
		bodyField.setText("");
		photoData = null;
		iv.hide();		
		tPosting.hide();
		tNoImage.show();
		menuClear = false;
		changeState(0);
		setFocusedDescendant(tPhoto);
	}

	public void setBody(String body) {
		bodyField.setText(body);
		// showPhotoPicker();
		tChooser.show();
	}

	public void clearImage() {
		if (iv != null) {
			photoData = null;
			iv.hide();
			tNoImage.show();
			menuClear = false;
			changeState(0);
			setFocusedDescendant(tPhoto);
		}
	}

	public void clearPostWindow() {
		tPosting.hide();
	}

	public void showLogin() {
		twitpic.mLogin.show();
		twitpicLoginView.enableInput();
	}

	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case EVENT_SIGN_UP: {
				try{
					danger.net.URL.gotoURL("https://twitter.com/signup");
				}
				catch (danger.net.URLException exc) { }
				return false;
			}
			case EVENT_OPEN_URL: {
				try {
					danger.net.URL.gotoURL(lastPost);
				}
				catch (danger.net.URLException exec) {}
				return false;
			}
			case EVENT_SETUP: {
				showLogin();
				return false;
			}
			case EVENT_MENU_CLEAR: {
				tClear.show();
				return false;
			}
			case EVENT_CLEAR_PHOTO: {
				clearImage();
				return false;
			}
			case EVENT_POST: {
				//can't post without a JPG photo
				if (!isJPEG) {
					tError.setMessage("Photo selected was not a JPG.");
					tError.show();
				}
				//everything is good. post and display window.
				if (isJPEG) {
					twitpic.postEntry(bodyField.toString(),photoData,photoname,photoSize);
					tPosting.show();
				}
				return true;
			}
			case EVENT_twitpicCOM: {
				try {
					danger.net.URL.gotoURL("http://twitpic.com/");
				}
				catch (danger.net.URLException exc) {}
				return false;
			}
			case EVENT_YOUtwitpicCOM: {
				String user = twitpic.twitpicPrefs.getStringValue("username");
				try {
					danger.net.URL.gotoURL("http://twitpic.com/photos/"+user);
				}
				catch (danger.net.URLException exc) {}
				return false;
			}
			case EVENT_PUBLICTIMELINE: {
				try{
					danger.net.URL.gotoURL("http://twitpic.com/public_timeline/");
				}
				catch (danger.net.URLException exc) {}
				return false;
			}
			case EVENT_PHOTOS: {
				//4.6+
				showSelectedPhotos((GalleryItemIPCPayload) e.argument);
				//legacy
				// showSelectedPhotos((PhotoRecordIPCPayload) e.argument);
				return true;
			}
			case EVENT_PHOTO_PICKER: {
				tChooser.show();
				return true;
			}
			case EVENT_CHOOSE_PHOTO: {
				showPhotoPicker(false);
				return true;
			}
			case EVENT_TAKE_PHOTO: {
				showPhotoPicker(true);
				return true;
			}
			case EVENT_CHOOSER_BACK: {
				tChooser.hide();
				return true;
			}
			
			case ABOUT: {
				// AlertWindow about = getApplication().getAlert(ID_ABOUT, this);
				// about.show();
				IPCMessage msg = new IPCMessage();
				msg.addItem("action", "send");
				Registrar.sendMessage("quickTwit", msg, null);
				return true;
			}
			default:
			break;
		}
		return super.receiveEvent(e);
	}

    public boolean eventWidgetUp(int inWidget, Event e) {
		switch (inWidget) {
			case Event.DEVICE_BUTTON_CANCEL:
			Application.getCurrentApp().returnToLauncher();
			return true;

			case Event.DEVICE_BUTTON_BACK:
			Application.getCurrentApp().returnToLauncher();
			return true;
		}
		return super.eventWidgetUp(inWidget, e);
	}
}