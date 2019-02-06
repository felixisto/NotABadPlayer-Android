package com.media.notabadplayer.View.Playlists;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.media.notabadplayer.Audio.AudioAlbum;
import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.AudioPlaylist;
import com.media.notabadplayer.Audio.AudioTrack;
import com.media.notabadplayer.Presenter.Main.MainPresenter;
import com.media.notabadplayer.R;
import com.media.notabadplayer.View.BasePresenter;
import com.media.notabadplayer.View.BaseView;

import java.util.ArrayList;

public class PlaylistsFragment extends Fragment implements BaseView {
    private MainPresenter _presenter;

    public PlaylistsFragment()
    {

    }

    public static PlaylistsFragment newInstance()
    {
        return new PlaylistsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_playlists, container, false);
        
        // Setup UI

        return root;
    }

    @Override
    public void setPresenter(BasePresenter presenter)
    {

    }

    @Override
    public void openAlbumScreen(AudioInfo audioInfo, String albumID, String albumArtist, String albumTitle, String albumCover) {
        
    }

    @Override
    public void onMediaAlbumsLoad(ArrayList<AudioAlbum> albums)
    {

    }

    @Override
    public void onAlbumSongsLoad(ArrayList<AudioTrack> songs)
    {

    }
    
    @Override
    public void openPlayerScreen(AudioPlaylist playlist)
    {

    }

    @Override
    public void searchQueryResults(ArrayList<AudioTrack> songs)
    {

    }
}
