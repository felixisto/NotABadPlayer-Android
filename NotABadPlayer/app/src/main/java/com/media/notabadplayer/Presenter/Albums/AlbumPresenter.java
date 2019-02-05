package com.media.notabadplayer.Presenter.Albums;

import android.util.Log;

import com.media.notabadplayer.Audio.MediaAlbum;
import com.media.notabadplayer.Audio.MediaPlayerPlaylist;
import com.media.notabadplayer.Audio.MediaTrack;
import com.media.notabadplayer.Audio.MediaInfo;
import com.media.notabadplayer.View.BasePresenter;
import com.media.notabadplayer.View.BaseView;

import java.util.ArrayList;

public class AlbumPresenter implements BasePresenter {
    private BaseView _view;
    private MediaInfo _mediaInfo;
    
    private final MediaAlbum _album;
    
    private ArrayList<MediaTrack> _songs = new ArrayList<>();
    
    public AlbumPresenter(BaseView view, MediaInfo mediaInfo, MediaAlbum album)
    {
        _view = view;
        _mediaInfo = mediaInfo;
        _album = album;
    }

    @Override
    public void start()
    {
        _songs = _mediaInfo.getAlbumSongs(_album);
        _view.onAlbumSongsLoad(_songs);
    }

    @Override
    public void onAlbumClick(int index) 
    {
        
    }

    @Override
    public void onAlbumsItemClick(int index)
    {
        // Index zero is the header - ignore
        if (index == 0)
        {
            return;
        }
        
        // Index greater than zero is an song track
        index--;
        
        MediaTrack clickedTrack = _songs.get(index);
        MediaPlayerPlaylist playlist = new MediaPlayerPlaylist(_songs, clickedTrack.title);
        
        Log.v("AlbumPresenter", "Play playlist with specific song " + clickedTrack.title);
        
        _view.openPlayerScreen(playlist);
    }
}
