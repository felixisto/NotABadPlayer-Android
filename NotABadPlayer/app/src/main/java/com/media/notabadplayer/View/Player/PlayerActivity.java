package com.media.notabadplayer.View.Player;

import java.util.List;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

import com.media.notabadplayer.Audio.Model.AudioAlbum;
import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylist;
import com.media.notabadplayer.Audio.Model.BaseAudioTrack;
import com.media.notabadplayer.Audio.Model.OpenPlaylistOptions;
import com.media.notabadplayer.Audio.Utilities.AudioPlayerNoiseSuppression;
import com.media.notabadplayer.Constants.AppSettings;
import com.media.notabadplayer.Controls.ApplicationInput;
import com.media.notabadplayer.Controls.KeyBinds;
import com.media.notabadplayer.Presenter.PlayerPresenter;
import com.media.notabadplayer.R;
import com.media.notabadplayer.Storage.GeneralStorage;
import com.media.notabadplayer.Utilities.AppThemeUtility;
import com.media.notabadplayer.Utilities.Serializing;
import com.media.notabadplayer.Presenter.BasePresenter;
import com.media.notabadplayer.View.BaseView;

public class PlayerActivity extends AppCompatActivity implements BaseView
{
    private BasePresenter _presenter;
    
    private BaseView _fragment;
    
    private AudioPlayerNoiseSuppression _noiseSuppression;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        
        // Never restore this activity, navigate back to the main activity
        if (savedInstanceState != null)
        {
            finish();
            return;
        }

        // Audio model - retrieve from intent
        String intentData = getIntent().getStringExtra("playlist");
        BaseAudioPlaylist playlist = (BaseAudioPlaylist) Serializing.deserializeObject(intentData);

        if (playlist == null)
        {
            Log.v(PlayerActivity.class.getCanonicalName(), "Error: player cannot start with a null playlist.");

            finish();
            return;
        }
        
        // App theme
        AppThemeUtility.setTheme(this, GeneralStorage.getShared().getAppThemeValue());
        
        // Content
        setContentView(R.layout.activity_player);
        
        // UI
        initUI(playlist);
        
        // Noise suppression
        _noiseSuppression = new AudioPlayerNoiseSuppression();
        registerReceiver(_noiseSuppression, new IntentFilter(ACTION_AUDIO_BECOMING_NOISY));

        // Transition animation
        overridePendingTransition(R.anim.player_slide_up, R.anim.hold);
    }
    
    @Override
    protected void onDestroy()
    {
        if (_noiseSuppression != null)
        {
            unregisterReceiver(_noiseSuppression);
        }
        
        super.onDestroy();
    }

    @Override
    public void finish()
    {
        super.finish();

        // Transition animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) 
        {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            
            View view = findViewById(android.R.id.content);
            
            if (view != null)
            {
                Drawable background = view.getBackground();

                if (background instanceof ColorDrawable)
                {
                    window.setStatusBarColor(((ColorDrawable) background).getColor());
                } 
            }
        }
        
        overridePendingTransition(0, R.anim.player_slide_down);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP))
        {
            KeyBinds.getShared().evaluateInput(ApplicationInput.PLAYER_VOLUME_UP_BUTTON);
            return true;
        }
        
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
        {
            KeyBinds.getShared().evaluateInput(ApplicationInput.PLAYER_VOLUME_DOWN_BUTTON);
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    private void initUI(@NonNull BaseAudioPlaylist playlist)
    {
        _presenter = new PlayerPresenter(playlist);
        _fragment = PlayerFragment.newInstance(_presenter);
        _presenter.setView(_fragment);
        
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.player, (Fragment)_fragment).commit();
    }

    @Override
    public void openPlaylistScreen(@NonNull AudioInfo audioInfo, @NonNull BaseAudioPlaylist playlist, @NonNull OpenPlaylistOptions options)
    {

    }
    
    @Override
    public void onMediaAlbumsLoad(@NonNull List<AudioAlbum> albums)
    {

    }

    @Override
    public void onPlaylistLoad(@NonNull BaseAudioPlaylist playlist)
    {

    }

    @Override
    public void onUserPlaylistsLoad(@NonNull List<BaseAudioPlaylist> playlists)
    {

    }
    
    @Override
    public void openPlayerScreen(@NonNull BaseAudioPlaylist playlist)
    {

    }

    @Override
    public void updatePlayerScreen(@NonNull BaseAudioPlaylist playlist)
    {

    }

    @Override
    public void updateSearchQueryResults(@NonNull String searchQuery, com.media.notabadplayer.Constants.SearchFilter filter, @NonNull List<BaseAudioTrack> songs, @Nullable String searchState)
    {

    }

    @Override
    public void openCreatePlaylistScreen(@Nullable BaseAudioPlaylist playlistToEdit)
    {

    }

    @Override
    public void onAppSettingsLoad(com.media.notabadplayer.Storage.GeneralStorage storage)
    {

    }
    
    @Override
    public void onResetAppSettings()
    {

    }

    @Override
    public void onAppThemeChanged(AppSettings.AppTheme appTheme)
    {

    }

    @Override
    public void onAppTrackSortingChanged(AppSettings.TrackSorting trackSorting)
    {

    }

    @Override
    public void onShowVolumeBarSettingChange(AppSettings.ShowVolumeBar value)
    {

    }

    @Override
    public void onDeviceLibraryChanged()
    {

    }

    @Override
    public void onFetchDataErrorEncountered(@NonNull Exception error)
    {

    }

    @Override
    public void onPlayerErrorEncountered(@NonNull Exception error)
    {

    }
}
