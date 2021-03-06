package com.nostra13.socialsharing.facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.util.Log;

import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.FacebookError;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class FacebookLogoutListener implements RequestListener {

	private static final String TAG = FacebookLogoutListener.class.getSimpleName();

	@Override
	public void onComplete(final String response, final Object state) {
		FacebookEvents.onLogoutComplete();
	}

	@Override
	public void onFacebookError(FacebookError e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
	}

	@Override
	public void onFileNotFoundException(FileNotFoundException e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
	}

	@Override
	public void onIOException(IOException e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
	}

	@Override
	public void onMalformedURLException(MalformedURLException e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
	}
}
