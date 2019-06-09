package com.media.notabadplayer.Presenter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.Players.Player;
import com.media.notabadplayer.Audio.Model.AudioPlaylist;
import com.media.notabadplayer.Constants.AppSettings;
import com.media.notabadplayer.Constants.AppState;
import com.media.notabadplayer.Controls.ApplicationAction;
import com.media.notabadplayer.Controls.ApplicationInput;
import com.media.notabadplayer.Controls.KeyBinds;
import com.media.notabadplayer.View.BaseView;

public class QuickPlayerPresenter implements BasePresenter
{
    private BaseView _view;

    private @NonNull AudioInfo _audioInfo;
    
    private boolean _running = false;

    public QuickPlayerPresenter(@NonNull AudioInfo audioInfo)
    {
        this._audioInfo = audioInfo;
    }

    @Override
    public void setView(@NonNull BaseView view)
    {
        if (_view != null)
        {
            throw new IllegalStateException("PlayerPresenter: view has already been set");
        }
        
        _view = view;
    }
    
    @Override
    public void start() 
    {
        if (_view == null)
        {
            throw new IllegalStateException("QuickPlayerPresenter: view has not been set");
        }
    }

    @Override
    public void fetchData()
    {

    }

    @Override
    public void onAppStateChange(AppState state)
    {
        _running = state.isRunning();
    }

    @Override
    public void onAlbumItemClick(int index)
    {

    }

    @Override
    public void onPlaylistItemClick(int index)
    {

    }

    @Override
    public void onOpenPlayer(@Nullable AudioPlaylist playlist)
    {
        if (!_running)
        {
            return;
        }
        
        AudioPlaylist currentlyPlayingPlaylist = Player.getShared().getPlaylist();

        if (currentlyPlayingPlaylist == null)
        {
            return;
        }

        Log.v(QuickPlayerPresenter.class.getCanonicalName(), "Open player screen");
        
        _view.openPlayerScreen(currentlyPlayingPlaylist);
    }

    @Override
    public void onPlayerButtonClick(ApplicationInput input)
    {
        if (!_running)
        {
            return;
        }
        
        KeyBinds.getShared().evaluateInput(input);
    }

    @Override
    public void onPlayOrderButtonClick()
    {
        if (!_running)
        {
            return;
        }

        KeyBinds.getShared().performAction(ApplicationAction.CHANGE_PLAY_ORDER);
    }
    
    @Override
    public void onOpenPlaylistButtonClick()
    {
        if (!_running)
        {
            return;
        }
        
        AudioPlaylist currentlyPlayingPlaylist = Player.getShared().getPlaylist();
        
        if (currentlyPlayingPlaylist != null)
        {
            _view.openPlaylistScreen(_audioInfo, currentlyPlayingPlaylist);
        }
    }

    @Override
    public void onPlayerVolumeSet(double value)
    {

    }

    @Override
    public void onPlaylistItemDelete(int index)
    {

    }

    @Override
    public void onSearchResultClick(int index)
    {

    }

    @Override
    public void onSearchQuery(@NonNull String searchValue)
    {

    }

    @Override
    public void onAppSettingsReset() 
    {

    }

    @Override
    public void onAppThemeChange(AppSettings.AppTheme themeValue) 
    {

    }

    @Override
    public void onAppTrackSortingChange(AppSettings.TrackSorting trackSorting) 
    {

    }

    @Override
    public void onShowVolumeBarSettingChange(AppSettings.ShowVolumeBar value) 
    {

    }

    @Override
    public void onOpenPlayerOnPlaySettingChange(AppSettings.OpenPlayerOnPlay value)
    {

    }

    @Override
    public void onKeybindChange(ApplicationAction action, ApplicationInput input) {

    }
}
