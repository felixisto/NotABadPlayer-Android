package com.media.notabadplayer.Presenter.Lists;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.Model.AudioPlaylistBuilder;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylist;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylistBuilderNode;
import com.media.notabadplayer.Audio.Model.BaseAudioTrack;
import com.media.notabadplayer.Audio.Model.OpenPlaylistOptions;
import com.media.notabadplayer.Audio.Players.Player;
import com.media.notabadplayer.Constants.AppState;
import com.media.notabadplayer.R;
import com.media.notabadplayer.Storage.AudioLibrary;
import com.media.notabadplayer.Storage.GeneralStorage;
import com.media.notabadplayer.View.Lists.ListsView;

public class ListsPresenterImpl implements ListsPresenter
{
    private ListsView _view;
    
    private @NonNull AudioInfo _audioInfo;
    
    private List<BaseAudioPlaylist> _playlists = new ArrayList<>();
    
    private final String _historyPlaylistName;
    private final String _recentlyAddedPlaylistName;
    private final String _favoritesPlaylistName;

    private boolean _running = false;
    
    private boolean _fetchingData = false;
    
    public ListsPresenterImpl(@NonNull Context context, @NonNull AudioInfo audioInfo)
    {
        _audioInfo = audioInfo;
        _historyPlaylistName = context.getResources().getString(R.string.playlist_name_history);
        _recentlyAddedPlaylistName = context.getResources().getString(R.string.playlist_name_recently_added);
        _favoritesPlaylistName = context.getResources().getString(R.string.playlist_name_favorites);
    }

    // ListsPresenter

    @Override
    public void start()
    {
        if (_view == null)
        {
            throw new IllegalStateException("ListsPresenter: view has not been set");
        }

        Log.v(ListsPresenterImpl.class.getCanonicalName(), "Start.");
    }

    @Override
    public void setView(@NonNull ListsView view)
    {
        if (_view != null)
        {
            throw new IllegalStateException("ListsPresenter: view has already been set");
        }
        
        _view = view;
    }

    @Override
    public void onDestroy()
    {
        Log.v(ListsPresenterImpl.class.getCanonicalName(), "Destroyed.");

        _running = false;
    }

    @Override
    public void fetchData()
    {
        if (_fetchingData)
        {
            return;
        }

        Log.v(ListsPresenterImpl.class.getCanonicalName(), "Fetching user playlists...");

        final Player player = Player.getShared();
        
        _fetchingData = true;
        
        // Wait for the app start running
        // Then, update the view on the main thread
        Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable;
                
                if (_running && player.isInitialized())
                {
                    List<BaseAudioPlaylist> lists = GeneralStorage.getShared().getUserPlaylists();

                    BaseAudioPlaylist recentlyAdded = getRecentlyAddedPlaylist();

                    if (recentlyAdded != null)
                    {
                        lists.add(0, recentlyAdded);
                    }

                    BaseAudioPlaylist recentlyPlayed = player.playHistory.getPlayHistoryAsPlaylist(_historyPlaylistName);
                    
                    if (recentlyPlayed != null)
                    {
                        lists.add(0, recentlyPlayed);
                    }

                    BaseAudioPlaylist favorites = getFavoritesPlaylist();

                    if (favorites != null)
                    {
                        lists.add(0, favorites);
                    }

                    final List<BaseAudioPlaylist> data = lists;
                    
                    myRunnable = new Runnable() {
                        @Override
                        public void run()
                        {
                            Log.v(ListsPresenterImpl.class.getCanonicalName(), "Retrieved user playlists, updating view");

                            _fetchingData = false;
                            
                            _playlists = data;
                            
                            updatePlaylistsData();
                        }
                    };
                }
                else
                {
                    myRunnable = new Runnable() {
                        @Override
                        public void run()
                        {
                            Log.v(ListsPresenterImpl.class.getCanonicalName(), "Presenter is not ready to fetch yet!");

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
    public void onPlaylistItemClick(int index)
    {
        if (!_running)
        {
            return;
        }

        if (index < 0 || index >= _playlists.size())
        {
            return;
        }

        BaseAudioPlaylist playlist = _playlists.get(index);

        Log.v(ListsPresenterImpl.class.getCanonicalName(), "Open playlist '" + playlist.getName() + "'");

        OpenPlaylistOptions appropriateOptions = getAppropriateOpenOptions(playlist);

        try {
            _view.openPlaylistScreen(_audioInfo, playlist, appropriateOptions);
        } catch (Exception e) {
            Log.v(ListsPresenterImpl.class.getCanonicalName(), "Failed to open playlist screen, error: " + e);
        }
    }

    @Override
    public void onPlaylistItemEdit(int index)
    {
        if (!_running)
        {
            return;
        }

        if (index < 0 || index >= _playlists.size())
        {
            return;
        }

        BaseAudioPlaylist playlist = _playlists.get(index);
        
        if (playlist.isTemporary()) 
        {
            return;
        }
        
        Log.v(ListsPresenterImpl.class.getCanonicalName(), "Edit playlist '" + playlist.getName() + "'");
        
        _view.openCreatePlaylistScreen(playlist);
    }
    
    @Override
    public void onPlaylistItemDelete(int index)
    {
        if (!_running)
        {
            return;
        }
        
        if (index < _playlists.size())
        {
            _playlists.remove(index);

            if (_playlists.size() > 0)
            {
                ArrayList<BaseAudioPlaylist> playlists = new ArrayList<>(_playlists);

                // Remove the temporary lists before saving (recently played/added playlists)
                for (BaseAudioPlaylist playlist : _playlists)
                {
                    if (playlist.isTemporary())
                    {
                        playlists.remove(playlist);
                    }
                }

                // Save to storage
                GeneralStorage.getShared().saveUserPlaylists(playlists);
                
                updatePlaylistsData();
            }
        }
    }

    private void updatePlaylistsData()
    {
        _view.onUserPlaylistsLoad(_playlists);
    }

    private @Nullable BaseAudioPlaylist getRecentlyAddedPlaylist()
    {
        List<BaseAudioTrack> recentlyAddedTracks = AudioLibrary.getShared().getRecentlyAddedTracks();

        if (recentlyAddedTracks.size() > 0)
        {
            BaseAudioPlaylistBuilderNode node = AudioPlaylistBuilder.start();
            node.setName(_recentlyAddedPlaylistName);
            node.setTracks(recentlyAddedTracks);
            node.setIsTemporaryPlaylist(true);

            try {
                return node.build();
            } catch (Exception exc) {

            }
        }

        return null;
    }

    private @Nullable BaseAudioPlaylist getFavoritesPlaylist()
    {
        List<BaseAudioTrack> favoriteTracks = AudioLibrary.getShared().getFavoriteTracks();

        if (favoriteTracks.size() > 0)
        {
            BaseAudioPlaylistBuilderNode node = AudioPlaylistBuilder.start();
            node.setName(_favoritesPlaylistName);
            node.setTracks(favoriteTracks);
            node.setIsTemporaryPlaylist(true);

            try {
                return node.build();
            } catch (Exception exc) {

            }
        }

        return null;
    }
    
    private @NonNull OpenPlaylistOptions getAppropriateOpenOptions(@NonNull BaseAudioPlaylist playlist) 
    {
        if (!playlist.isTemporary()) {
            return OpenPlaylistOptions.buildDefault();
        }
        
        if (playlist.getName().equals(_favoritesPlaylistName)) 
        {
            return OpenPlaylistOptions.buildFavorites();
        }

        if (playlist.getName().equals(_recentlyAddedPlaylistName))
        {
            return OpenPlaylistOptions.buildRecentlyAdded();
        }

        if (playlist.getName().equals(_historyPlaylistName))
        {
            return OpenPlaylistOptions.buildRecentlyPlayed();
        }

        return OpenPlaylistOptions.buildDefault();
    }
}
