package com.media.notabadplayer.Presenter;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.media.notabadplayer.Audio.Model.AudioAlbum;
import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.Model.AudioPlaylistBuilder;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylist;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylistBuilderNode;
import com.media.notabadplayer.Audio.Model.BaseAudioTrack;
import com.media.notabadplayer.Audio.Model.OpenPlaylistOptions;
import com.media.notabadplayer.Constants.AppState;
import com.media.notabadplayer.Controls.ApplicationInput;
import com.media.notabadplayer.Constants.AppSettings;
import com.media.notabadplayer.Storage.AudioLibrary;
import com.media.notabadplayer.View.BaseView;

public class AlbumsPresenter implements BasePresenter, AudioLibrary.ChangesListener {
    private BaseView _view;
    
    private @NonNull AudioInfo _audioInfo;
    
    private @NonNull List<AudioAlbum> _albums = new ArrayList<>();

    private boolean _running = false;
    
    private boolean _fetchingData = false;
    
    public AlbumsPresenter(@NonNull AudioInfo audioInfo)
    {
        _audioInfo = audioInfo;
    }
    
    @Override
    public void setView(@NonNull BaseView view)
    {
        if (_view != null)
        {
            throw new IllegalStateException("AlbumsPresenter: view has already been set");
        }
        
        _view = view;
    }
    
    @Override
    public void start()
    {
        if (_view == null)
        {
            throw new IllegalStateException("AlbumsPresenter: view has not been set");
        }

        Log.v(AlbumsPresenter.class.getCanonicalName(), "Start.");

        AudioLibrary.getShared().registerLibraryChangesListener(this);

        fetchData();
    }

    @Override
    public void onDestroy()
    {
        Log.v(AlbumsPresenter.class.getCanonicalName(), "Destroyed.");

        AudioLibrary.getShared().unregisterLibraryChangesListener(this);

        _running = false;
    }

    @Override
    public void fetchData()
    {
        if (_fetchingData)
        {
            return;
        }
        
        Log.v(AlbumsPresenter.class.getCanonicalName(), "Fetching albums...");

        _fetchingData = true;

        final boolean running = _running;

        // Wait for the app start running
        // Then, update the view on the main thread
        Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable;

                if (running)
                {
                    final List<AudioAlbum> albums = _audioInfo.getAlbums();

                    myRunnable = new Runnable() {
                        @Override
                        public void run()
                        {
                            Log.v(AlbumsPresenter.class.getCanonicalName(), "Retrieved albums, updating view");

                            _fetchingData = false;

                            _albums = albums;

                            _view.onMediaAlbumsLoad(_albums);
                        }
                    };
                }
                else
                {
                    myRunnable = new Runnable() {
                        @Override
                        public void run()
                        {
                            Log.v(AlbumsPresenter.class.getCanonicalName(), "Presenter is not ready to fetch yet!");

                            _fetchingData = false;

                            _view.onFetchDataErrorEncountered(new RuntimeException("Presenter is not ready to fetch yet"));
                        }
                    };
                }

                mainHandler.post(myRunnable);
            }
        });

        handler.post(thread);
    }
    
    @Override
    public void onAppStateChange(AppState state)
    {
        _running = state.isRunning();
    }

    @Override
    public void onAlbumItemClick(int index)
    {
        if (!_running)
        {
            return;
        }
        
        if (index < 0 || index >= _albums.size())
        {
            Log.v(AlbumsPresenter.class.getCanonicalName(), "Error: Invalid album list index, cannot respond to event properly");
            return;
        }

        AudioAlbum album = _albums.get(index);
        
        Log.v(AlbumsPresenter.class.getCanonicalName(), "Open '" + album.albumTitle + "' album");

        List<BaseAudioTrack> tracks = _audioInfo.getAlbumTracks(album);

        BaseAudioPlaylistBuilderNode node = AudioPlaylistBuilder.start();
        node.setName(album.albumTitle);
        node.setTracks(tracks);

        // Try to build
        try {
            BaseAudioPlaylist playlist = node.build();
            _view.openPlaylistScreen(_audioInfo, playlist, OpenPlaylistOptions.buildDefault());
        } catch (Exception e) {
            Log.v(AlbumsPresenter.class.getCanonicalName(), "Error: Failed to create a playlist for the clicked album track");
        }
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

    @Override
    public void onMediaLibraryChanged()
    {
        if (_view != null)
        {
            _view.onDeviceLibraryChanged();
        }

        // Stay up to date with the latest library data
        fetchData();
    }
}
