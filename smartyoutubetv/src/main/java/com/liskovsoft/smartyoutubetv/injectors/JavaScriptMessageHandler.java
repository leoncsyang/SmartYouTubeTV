package com.liskovsoft.smartyoutubetv.injectors;

import android.app.Activity;
import android.content.Context;
import com.liskovsoft.browser.Browser;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.smartyoutubetv.CommonApplication;
import com.liskovsoft.smartyoutubetv.events.MicClickedEvent;
import com.liskovsoft.smartyoutubetv.misc.LangUpdater;
import com.liskovsoft.smartyoutubetv.prefs.SmartPreferences;
import com.liskovsoft.smartyoutubetv.fragments.FragmentManager;

public class JavaScriptMessageHandler {
    private final static String MESSAGE_APP_LOADED = "message_app_loaded";
    private final static String MESSAGE_MIC_CLICKED = "message_mic_clicked";
    private final static String MESSAGE_SYNC_LANG = "message_sync_lang";
    private final static String MESSAGE_AUTHORIZATION_HEADER = "message_authorization_header";
    private final static String MESSAGE_ENABLE_SCREENSAVER = "message_enable_screensaver";
    private final static String MESSAGE_DISABLE_SCREENSAVER = "message_disable_screensaver";
    private final static String MESSAGE_VIDEO_POSITION = "message_video_position";
    private final static String MESSAGE_DOUBLE_BACK_EXIT = "message_double_back_exit";
    private final static String MESSAGE_SEARCH_FIELD_FOCUSED = "message_search_field_focused";
    private final static String MESSAGE_AUTH_BODY = "message_auth_body";
    private final Context mContext;

    public JavaScriptMessageHandler(Context context) {
        mContext = context;
    }

    public void handleMessage(String message, String content) {
        switch (message) {
            case MESSAGE_MIC_CLICKED:
                Browser.getBus().post(new MicClickedEvent());
                break;
            case MESSAGE_APP_LOADED:
                ((FragmentManager) mContext).onAppLoaded();
                break;
            case MESSAGE_SYNC_LANG:
                LangUpdater updater = new LangUpdater(mContext);
                updater.setPreferredLocale(content);
                updater.update();
                break;
            case MESSAGE_AUTHORIZATION_HEADER:
                SmartPreferences prefs = SmartPreferences.instance(mContext);
                prefs.setAuthorizationHeader(content);
                break;
            case MESSAGE_ENABLE_SCREENSAVER:
                Helpers.enableScreensaver((Activity) mContext);
                break;
            case MESSAGE_DISABLE_SCREENSAVER:
                Helpers.disableScreensaver((Activity) mContext);
                break;
            case MESSAGE_VIDEO_POSITION:
                CommonApplication.getPreferences().setCurrentVideoPosition(Integer.parseInt(content));
                break;
            case MESSAGE_DOUBLE_BACK_EXIT:
                ((FragmentManager) mContext).onExitDialogShown();
                break;
            case MESSAGE_SEARCH_FIELD_FOCUSED:
                ((FragmentManager) mContext).onSearchFieldFocused();
                break;
            case MESSAGE_AUTH_BODY:
                GlobalPreferences.instance(mContext).setRawAuthData(content);
                break;
        }
    }
}
