package com.media.notabadplayer.View.Albums;

import java.util.ArrayList;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.media.notabadplayer.Audio.AudioAlbum;
import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.AudioPlaylist;
import com.media.notabadplayer.Audio.AudioTrack;
import com.media.notabadplayer.Constants.AppSettings;
import com.media.notabadplayer.Presenter.PlaylistPresenter;
import com.media.notabadplayer.R;
import com.media.notabadplayer.Presenter.BasePresenter;
import com.media.notabadplayer.View.BaseView;
import com.media.notabadplayer.View.Playlist.PlaylistFragment;

public class AlbumsFragment extends Fragment implements BaseView
{
    private BasePresenter _presenter;
    
    private GridView _table;
    private AlbumsTableAdapter _tableAdapter;
    private Parcelable _tableState;
    private GridSideIndexingView _tableSideIndexingView;
    private TextView _indexingTextCharacter;
    
    public AlbumsFragment()
    {
        
    }
    
    public static @NonNull AlbumsFragment newInstance(@NonNull BasePresenter presenter)
    {
        AlbumsFragment fragment = new AlbumsFragment();
        fragment._presenter = presenter;
        return fragment;
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_albums, container, false);
        
        _table = root.findViewById(R.id.primaryAreaGrid);
        _tableSideIndexingView = root.findViewById(R.id.tableSideIndexingView);
        _indexingTextCharacter = root.findViewById(R.id.indexingTextCharacter);
        
        initUI();
        
        return root;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        _presenter.start();

        if (_tableAdapter != null)
        {
            _table.setAdapter(_tableAdapter);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        
        if (_tableState != null)
        {
            _table.onRestoreInstanceState(_tableState);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        enableInteraction();
    }

    @Override
    public void onPause()
    {
        _tableState = _table.onSaveInstanceState();

        super.onPause();

        disableInteraction();
    }
    
    private void initUI()
    {
        _table.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (!_table.isClickable())
                {
                    return;
                }

                _presenter.onAlbumItemClick(position);
            }
        });
    }
    
    @Override
    public void enableInteraction()
    {
        _table.setClickable(true);
    }

    @Override
    public void disableInteraction()
    {
        _table.setClickable(false);
    }

    @Override
    public void openPlaylistScreen(@NonNull AudioInfo audioInfo, @NonNull AudioPlaylist playlist)
    {
        FragmentActivity a = getActivity();

        if (a == null)
        {
            return;
        }

        FragmentManager manager = a.getSupportFragmentManager();
        int backStackCount = manager.getBackStackEntryCount();

        String newEntryName = playlist.getName();
        String lastEntryName = backStackCount > 0 ? manager.getBackStackEntryAt(backStackCount-1).getName() : "";

        // Do nothing, if the last entry name is equal to the new entry name
        if (lastEntryName != null && lastEntryName.equals(newEntryName))
        {
            return;
        }

        while (manager.getBackStackEntryCount() > 0)
        {
            manager.popBackStackImmediate();
        }
        
        BasePresenter presenter = new PlaylistPresenter(playlist, audioInfo);
        PlaylistFragment view = PlaylistFragment.newInstance(presenter);
        presenter.setView(view);

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.setCustomAnimations(0, R.anim.fade_in, 0, R.anim.hold);
        transaction.addToBackStack(newEntryName);
        transaction.replace(R.id.mainLayout, view, newEntryName);
        transaction.commit();
    }
    
    @Override
    public void onMediaAlbumsLoad(@NonNull ArrayList<AudioAlbum> albums)
    {
        Context context = getContext();

        if (context == null)
        {
            return;
        }

        _tableAdapter = new AlbumsTableAdapter(context, albums, _tableSideIndexingView);
        _table.setAdapter(_tableAdapter);
        
        ArrayList<String> titles = new ArrayList<>();
        
        for (int e = 0; e < albums.size(); e++)
        {
            titles.add(albums.get(e).albumTitle);
        }
        
        _tableSideIndexingView.updateAlphabet(titles);
        _tableSideIndexingView.start(_table, _indexingTextCharacter);
    }

    @Override
    public void onPlaylistLoad(@NonNull AudioPlaylist playlist)
    {
        
    }
    
    @Override
    public void openPlayerScreen(@NonNull AudioPlaylist playlist)
    {

    }

    @Override
    public void updatePlayerScreen(@NonNull AudioPlaylist playlist)
    {

    }

    @Override
    public void searchQueryResults(@NonNull String searchQuery, @NonNull ArrayList<AudioTrack> songs, @Nullable String searchTip)
    {

    }

    @Override
    public void appSettingsReset()
    {

    }

    @Override
    public void appThemeChanged(AppSettings.AppTheme appTheme)
    {

    }

    @Override
    public void appTrackSortingChanged(AppSettings.TrackSorting trackSorting)
    {
        
    }

    @Override
    public void onShowVolumeBarSettingChange(AppSettings.ShowVolumeBar value)
    {

    }

    @Override
    public void onPlayerErrorEncountered(@NonNull Exception error)
    {

    }
}