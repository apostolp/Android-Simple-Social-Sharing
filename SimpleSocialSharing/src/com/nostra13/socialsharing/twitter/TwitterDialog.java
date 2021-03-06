package com.nostra13.socialsharing.twitter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class TwitterDialog extends Dialog {
	public static final String TAG = "twitter";

	static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

	static final String JS_HTML_EXTRACTOR = "javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');";
	static final String OAUTH_PIN_BLOCK_REGEXP = "id=\\\"oauth_pin((.|\\n)*)(\\d{7})";
	static final String OAUTH_PIN_REGEXP = "\\d{7}";

	private ProgressDialog spinner;
	private WebView browser;
	private FrameLayout content;

	private AsyncTwitter twitter;
	private RequestToken requestToken;

	public TwitterDialog(Context context, AsyncTwitter twitter) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		this.twitter = twitter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		spinner = new ProgressDialog(getContext());
		spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		spinner.setMessage("Loading...");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		content = new FrameLayout(getContext());
		setUpWebView(10);
		addContentView(content, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	@Override
	public void show() {
		super.show();
		browser.setVisibility(View.INVISIBLE);
		spinner.show();
		if (requestToken == null) {
			retrieveRequestToken();
		} else {
			browser.loadUrl(requestToken.getAuthorizationURL());
		}
	}

	private void retrieveRequestToken() {
		twitter.getOAuthRequestToken(new AuthRequestListener() {
			@Override
			public void onAuthRequestFailed(TwitterException e) {
				Log.e(TAG, e.getErrorMessage(), e);
				String errorMessage = e.getErrorMessage();
				if (errorMessage == null) {
					errorMessage = e.getMessage();
				}
				TwitterEvents.onLoginError(errorMessage);
				spinner.dismiss();
				dismiss();
			}

			@Override
			public void onAuthRequestComplete(RequestToken requestToken) {
				TwitterDialog.this.requestToken = requestToken;
				browser.loadUrl(requestToken.getAuthorizationURL());
			}
		});
	}

	private void setUpWebView(int margin) {
		LinearLayout webViewContainer = new LinearLayout(getContext());
		browser = new WebView(getContext());
		browser.setVerticalScrollBarEnabled(false);
		browser.setHorizontalScrollBarEnabled(false);
		browser.setWebViewClient(new TwitterDialog.TwWebViewClient());
		browser.getSettings().setJavaScriptEnabled(true);
		browser.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		browser.setLayoutParams(FILL);

		webViewContainer.setPadding(margin, margin, margin, margin);
		webViewContainer.addView(browser);
		content.addView(webViewContainer);
	}

	private class TwWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			TwitterEvents.onLoginError(description);
			TwitterDialog.this.dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d(TAG, "WebView loading URL: " + url);
			super.onPageStarted(view, url, favicon);
			spinner.show();
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			browser.loadUrl(JS_HTML_EXTRACTOR);

			content.setBackgroundColor(Color.TRANSPARENT);
			browser.setVisibility(View.VISIBLE);
		}
	}

	class MyJavaScriptInterface {
		public void processHTML(String html) {
			String blockWithPin = findExpression(html, OAUTH_PIN_BLOCK_REGEXP);
			if (blockWithPin != null) {
				String pin = findExpression(blockWithPin, OAUTH_PIN_REGEXP);
				if (pin != null) {
					autorizeApp(pin);
					spinner.dismiss();
					dismiss();
				}
			}
			spinner.dismiss();
		}

		private String findExpression(String text, String regExp) {
			Pattern p = Pattern.compile(regExp);
			Matcher m = p.matcher(text);
			if (m.find()) {
				return m.group(0);
			} else {
				return null;
			}
		}

		private void autorizeApp(String pin) {
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, pin);
				TwitterSessionStore.save(accessToken, getContext());
				TwitterEvents.onLoginSuccess();
			} catch (TwitterException e) {
				Log.e(TAG, e.getMessage(), e);
				TwitterEvents.onLoginError(e.getErrorMessage());
			}
		}
	}
}
