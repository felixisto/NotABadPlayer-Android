package com.media.notabadplayer.View.Player;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.media.notabadplayer.Audio.AlbumInfo;
import com.media.notabadplayer.Audio.MediaTrack;
import com.media.notabadplayer.Audio.MediaInfo;
import com.media.notabadplayer.Controlls.ApplicationInput;
import com.media.notabadplayer.Controlls.KeyBinds;
import com.media.notabadplayer.Presenter.Player.PlayerPresenter;
import com.media.notabadplayer.R;
import com.media.notabadplayer.View.BasePresenter;
import com.media.notabadplayer.View.BaseView;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity implements BaseView
{
    private BasePresenter _presenter;
    
    private BaseView _fragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initUI();
        
        MediaTrack track = MediaTrack.createFromString(getIntent().getStringExtra("track"));
        
        _presenter = new PlayerPresenter(_fragment, getApplicationContext(), track);
        _fragment.setPresenter(_presenter);
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP))
        {
            KeyBinds.getShared().respondToInput(ApplicationInput.VOLUME_UP_BUTTON);
            return true;
        }

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
        {
            KeyBinds.getShared().respondToInput(ApplicationInput.VOLUME_DOWN_BUTTON);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    private void initUI()
    {
        _fragment = PlayerFragment.newInstance();
        
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.player, (Fragment)_fragment).commit();
    }
    
    @Override
    public void setPresenter(BasePresenter presenter)
    {
        
    }
    
    @Override
    public void openAlbumScreen(MediaInfo mediaInfo, String albumID, String albumTitle, String albumCover)
    {

    }
    
    @Override
    public void onMediaAlbumsLoad(ArrayList<AlbumInfo> albums)
    {

    }
    
    @Override
    public void onAlbumSongsLoad(ArrayList<MediaTrack> songs)
    {

    }
    
    @Override
    public void openPlayerScreen(MediaTrack track)
    {

    }
}