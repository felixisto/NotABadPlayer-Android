package com.media.notabadplayer.Presenter.Lists;

import android.content.Context;
import android.support.annotation.NonNull;

import com.media.notabadplayer.Constants.AppSettings;
import com.media.notabadplayer.Controls.ApplicationAction;
import com.media.notabadplayer.Controls.ApplicationInput;
import com.media.notabadplayer.Presenter.BasePresenter;
import com.media.notabadplayer.View.BaseView;

public class ListsPresenter implements BasePresenter
{
    private @NonNull BaseView _view;

    public ListsPresenter(@NonNull BaseView view)
    {
        _view = view;
    }
    
    @Override
    public void start()
    {
        
    }

    @Override
    public void onAlbumClick(int index) 
    {

    }

    @Override
    public void onAlbumsItemClick(int index) 
    {

    }

    @Override
    public void onPlayerButtonClick(ApplicationInput input, @NonNull Context context)
    {

    }

    @Override
    public void onOpenPlaylistButtonClick(@NonNull Context context)
    {

    }

    @Override
    public void onPlayOrderButtonClick(@NonNull Context context)
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
    public void onAppSortingChange(AppSettings.AlbumSorting albumSorting, AppSettings.TrackSorting trackSorting)
    {

    }

    @Override
    public void onAppAppearanceChange(AppSettings.ShowStars showStars, AppSettings.ShowVolumeBar showVolumeBar) 
    {

    }

    @Override
    public void onKeybindChange(ApplicationAction action, ApplicationInput input) 
    {

    }
}