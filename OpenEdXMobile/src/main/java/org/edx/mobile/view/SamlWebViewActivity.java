package org.edx.mobile.view;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.inject.Inject;


import org.edx.mobile.R;
import org.edx.mobile.authentication.LoginAPI;
import org.edx.mobile.base.BaseFragmentActivity;
import org.edx.mobile.databinding.ActivitySamlWebViewBinding;
import static org.edx.mobile.http.constants.ApiConstants.URL_LOGIN_SAML;

import org.edx.mobile.http.HttpStatus;
import org.edx.mobile.http.HttpStatusException;
import org.edx.mobile.model.api.ProfileModel;
import org.edx.mobile.module.analytics.AnalyticsRegistry;
import org.edx.mobile.module.prefs.LoginPrefs;
import org.edx.mobile.task.Task;
import org.edx.mobile.util.Config;
import org.edx.mobile.util.IntentFactory;


public class SamlWebViewActivity extends BaseFragmentActivity {

    @Inject
    private Config config;

    @Inject
    LoginPrefs loginPrefs;

    @Inject
    protected AnalyticsRegistry analyticsRegistry;

    private ActivitySamlWebViewBinding activitySamlWebViewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySamlWebViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_saml_web_view);
        setToolbarAsActionBar();
        String idpSlug = config.getSamlConfig().getSamlIdpSlug();
        setTitle(idpSlug);
        WebView myWebView = activitySamlWebViewBinding.samlWebView;
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new MyWebViewClient());
        String url = URL_LOGIN_SAML.replace("{idpSlug}", idpSlug);
        myWebView.loadUrl(config.getApiHostURL()+url);
    }

    @NonNull
    public static Intent newIntent() {
        return IntentFactory.newIntentForComponent(SamlWebViewActivity.class);
    }

    private class MyWebViewClient extends WebViewClient {

        private static final String KEY_WORD_REGISTER = "register";
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            boolean isPlatform = url.contains(config.getApiHostURL());

            if (url.contains(KEY_WORD_REGISTER) && isPlatform) {
                view.setVisibility(View.VISIBLE);
            } else if (isPlatform) {
                view.setVisibility(View.GONE);
                activitySamlWebViewBinding.webViewProgress.loadingIndicator.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.VISIBLE);
                activitySamlWebViewBinding.webViewProgress.loadingIndicator.setVisibility(View.GONE);
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            String cookies = CookieManager.getInstance().getCookie(url);
            if (url.contains(config.getApiHostURL()) && cookies!= null  && !url.contains(KEY_WORD_REGISTER)) {
                loginPrefs.storeUserCookies(cookies);
                ProfileTask profileTask = new ProfileTask(getApplicationContext());
                profileTask.execute();
            }
        }
    }

    private class ProfileTask extends Task<ProfileModel> {

        @Inject
        private LoginAPI loginAPI;

        public ProfileTask(@NonNull Context context) {
            super(context);
        }

        @Override
        public void onSuccess(@NonNull ProfileModel profile) {
            onUserLoginSuccess(profile);
        }

        @Override
        public void onException(Exception ex) {
            if (ex instanceof HttpStatusException &&
                    ((HttpStatusException) ex).getStatusCode() == HttpStatus.UNAUTHORIZED) {
                loginPrefs.clear();
                showAlertDialog("Error", ex.getMessage());
            }
        }

        @Override
        @NonNull
        public ProfileModel call() throws Exception {
            return loginAPI.getProfile();
        }
    }

    public void onUserLoginSuccess(@NonNull ProfileModel profile){
        analyticsRegistry.identifyUser(
                profile.id.toString(),
                profile.email,
                profile.username);
        setResult(RESULT_OK);
        finish();
        environment.getRouter().showMainDashboard(this);
    }
}
