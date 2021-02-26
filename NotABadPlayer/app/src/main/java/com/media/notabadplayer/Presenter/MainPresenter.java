package com.media.notabadplayer.Presenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.media.notabadplayer.Audio.Model.BaseAudioPlaylist;
import com.media.notabadplayer.Constants.AppSettings;
import com.media.notabadplayer.Constants.AppState;
import com.media.notabadplayer.Controls.ApplicationInput;
import com.media.notabadplayer.View.BaseView;

public class MainPresenter implements BasePresenter {
    private BaseView _view;
    
    public MainPresenter() 
    {
        
    }

    @Override
    public void setView(@NonNull BaseView view)
    {
        if (_view != null)
        {
            throw new IllegalStateException("MainPresenter: view has already been set");
        }
        
        _view = view;
    }
    
    @Override
    public void start()
    {
        if (_view == null)
        {
            throw new IllegalStateException("MainPresenter: view has not been set");
        }

        Log.v(MainPresenter.class.getCanonicalName(), "Start.");
    }

    @Override
    public void onDestroy()
    {

    }

    @Override
    public void fetchData()
    {

    }

    @Override
    public void onAppStateChange(AppState state)
    {

    }

    @Override
    public void onAlbumItemClick(int index)
    {
        
    }

    @Override
    public void onOpenPlayer(@Nullable BaseAudioPlaylist playlist)
    {

    }

    @Override
    public void onPlayerButtonClick(ApplicationInput input)
    {

    }

    @Override
    public void onPlayOrderButtonClick()
    {

    }

    @Override
    public void onOpenPlaylistButtonClick()
    {

    }

    @Override
    public void onPlayerVolumeSet(double value)
    {

    }

    @Override
    public boolean onMarkOrUnmarkContextTrackFavorite()
    {
        return false;
    }

    @Override
    public void onPlaylistItemClick(int index)
    {

    }

    @Override
    public void onPlaylistItemEdit(int index)
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
    public void onSearchQuery(@NonNull String searchValue, com.media.notabadplayer.Constants.SearchFilter filter)
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
    public void onKeybindChange(com.media.notabadplayer.Controls.ApplicationAction action, com.media.notabadplayer.Controls.ApplicationInput input)
    {

    }
}
