package com.liskovsoft.smartyoutubetv.flavors.exoplayer.interceptors;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.webkit.WebResourceResponse;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.commands.GenericCommand;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.player.ExoPlayerFragment;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.wrappers.exoplayer.ExoPlayerWrapper;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.wrappers.externalplayer.ExternalPlayerWrapper;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parsers.OnMediaFoundCallback;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parsers.SimpleYouTubeInfoParser;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parsers.YouTubeInfoParser;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parsers.YouTubeMediaParser;
import com.liskovsoft.smartyoutubetv.fragments.TwoFragmentManager;
import com.liskovsoft.smartyoutubetv.interceptors.RequestInterceptor;
import com.liskovsoft.smartyoutubetv.prefs.SmartPreferences;

import java.io.InputStream;

public class ExoInterceptor extends RequestInterceptor {
    private final Context mContext;
    private static final String TAG = ExoInterceptor.class.getSimpleName();
    private final DelayedCommandCallInterceptor mDelayedInterceptor;
    private final BackgroundActionManager mManager;
    private final TwoFragmentManager mFragmentsManager;
    private final OnMediaFoundCallback mExoCallback;
    private final ExoNextInterceptor mNextInterceptor;
    private final HistoryInterceptor mHistoryInterceptor;
    private final SmartPreferences mPrefs;
    private final ActionsSender mSender;
    private String mCurrentUrl;
    public static final String URL_VIDEO_DATA = "get_video_info";
    public static final String URL_TV_TRANSPORT = "gen_204";

    public ExoInterceptor(Context context,
                          DelayedCommandCallInterceptor delayedInterceptor,
                          ExoNextInterceptor nextInterceptor,
                          HistoryInterceptor historyInterceptor) {
        super(context);
        
        mContext = context;
        mFragmentsManager = (TwoFragmentManager) context;
        mDelayedInterceptor = delayedInterceptor;
        mNextInterceptor = nextInterceptor;
        mHistoryInterceptor = historyInterceptor;
        mManager = new BackgroundActionManager();
        mPrefs = SmartPreferences.instance(mContext);
        mSender = new ActionsSender(mContext, this);
        
        boolean useExternalPlayer = !SmartPreferences.USE_EXTERNAL_PLAYER_NONE.equals(mPrefs.getUseExternalPlayer());

        if (useExternalPlayer) {
            mExoCallback = ExternalPlayerWrapper.create(mContext, this);
        } else {
            mExoCallback = new ExoPlayerWrapper(mContext, this);
        }
    }

    @Override
    public boolean test(String url) {
        return true;
    }

    @Override
    public WebResourceResponse intercept(String url) {
        Log.d(TAG, "Video intercepted: " + url);

        mCurrentUrl = url;

        mManager.init(url);

        // 'next' should not be fired at this point
        if (mManager.cancelPlayback()) {
            Log.d(TAG, "Video canceled: " + url);
            return null;
        }

        mExoCallback.onStart();

        // long running code
        new Thread(() -> {
            mExoCallback.onMetadata(mNextInterceptor.getMetadata(mManager.getVideoId(mCurrentUrl), mManager.getPlaylistId(mCurrentUrl)));
        }).start();

        // long running code
        new Thread(() -> {
            try {
                parseAndOpenExoPlayer(getUrlData(url));
            } catch (IllegalStateException e) {
                e.printStackTrace();
                MessageHelpers.showLongMessage(mContext, "Url doesn't exist " + url);
            }
        }).start();

        return null;
    }

    /**
     * For parsing details see {@link YouTubeMediaParser}
     */
    private void parseAndOpenExoPlayer(InputStream inputStream) {
        final YouTubeInfoParser dataParser = new SimpleYouTubeInfoParser(inputStream);
        Log.d(TAG, "Video manifest received");
        dataParser.parse(mExoCallback);
    }

    public void updateLastCommand(GenericCommand command) {
        mDelayedInterceptor.setCommand(command);
        // force call command without adding to the history (in case WebView)
        mDelayedInterceptor.forceRun(true);
    }

    public TwoFragmentManager getFragmentsManager() {
        return mFragmentsManager;
    }

    public BackgroundActionManager getBackgroundActionManager() {
        return mManager;
    }

    public String getCurrentUrl() {
        return mCurrentUrl;
    }

    public void closeVideo() {
        Intent intent = new Intent();
        intent.putExtra(ExoPlayerFragment.BUTTON_BACK, true);
        mSender.bindActions(intent);
        mManager.onCancel();
    }

    public void jumpToNextVideo() {
        Intent intent = new Intent();
        intent.putExtra(ExoPlayerFragment.BUTTON_NEXT, true);
        mSender.bindActions(intent);
        mManager.onContinue();
    }

    public HistoryInterceptor getHistoryInterceptor() {
        return mHistoryInterceptor;
    }
}
