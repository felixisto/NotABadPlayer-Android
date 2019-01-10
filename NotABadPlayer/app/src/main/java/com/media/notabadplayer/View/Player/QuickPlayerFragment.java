package com.media.notabadplayer.View.Player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.media.notabadplayer.Audio.AudioPlayer;
import com.media.notabadplayer.Audio.MediaPlayerObserver;
import com.media.notabadplayer.Audio.MediaTrack;
import com.media.notabadplayer.Controlls.ApplicationInput;
import com.media.notabadplayer.Controlls.KeyBinds;
import com.media.notabadplayer.R;
import com.media.notabadplayer.View.BasePresenter;
import com.media.notabadplayer.View.BaseView;

import java.util.ArrayList;

public class QuickPlayerFragment extends Fragment implements BaseView, MediaPlayerObserver {
    AudioPlayer _player = AudioPlayer.getShared();
    
    private Handler _handler = new Handler();
    
    private View _header;
    private ImageView _imageCover;
    private TextView _labelTitle;
    private SeekBar _mediaBar;
    private TextView _labelDurationCurrent;
    private TextView _labelDurationTotal;
    private Button _buttonBack;
    private Button _buttonPlay;
    private Button _buttonForward;
    
    public QuickPlayerFragment()
    {

    }

    public static QuickPlayerFragment newInstance()
    {
        return new QuickPlayerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        _player.attachObserver(this);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_quick_player, container, false);
        
        // Setup UI
        _header = root.findViewById(R.id.header);
        _imageCover = root.findViewById(R.id.cover);
        _labelTitle = root.findViewById(R.id.labelTitle);
        _mediaBar = root.findViewById(R.id.mediaBar);
        _labelDurationCurrent = root.findViewById(R.id.durationCurrent);
        _labelDurationTotal = root.findViewById(R.id.durationTotal);
        _buttonBack = root.findViewById(R.id.mediaButtonBack);
        _buttonPlay = root.findViewById(R.id.mediaButtonPlay);
        _buttonForward = root.findViewById(R.id.mediaButtonForward);
        
        // Init UI
        initUI();
        
        return root;
    }
    
    private void initUI()
    {
        _header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
        
        _mediaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser)
                {
                    return;
                }
            
                _player.seekTo(progress);
            }
        
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            
            }
        
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            
            }
        });
    
        _buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            
                KeyBinds.getShared().respondToInput(ApplicationInput.QUICK_PLAYER_PLAY_BUTTON);
            }
        });
    
        _buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            
                KeyBinds.getShared().respondToInput(ApplicationInput.QUICK_PLAYER_PREVIOUS_BUTTON);
            }
        });
    
        _buttonForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            
                KeyBinds.getShared().respondToInput(ApplicationInput.QUICK_PLAYER_NEXT_BUTTON);
            }
        });
        
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null)
                {
                    updateUIState();
                
                    _handler.postDelayed(this, 100);
                }
            }
        });
    }
    
    private void updateUIState()
    {
        int currentPosition = _player.getPlayer().getCurrentPosition() / 1000;
        _mediaBar.setProgress(currentPosition);
        _labelDurationCurrent.setText(MediaTrack.secondsToString(currentPosition));
    }

    @Override
    public void setPresenter(BasePresenter presenter)
    {

    }

    @Override
    public void openAlbumScreen(com.media.notabadplayer.Audio.MediaInfo mediaInfo, String albumID, String albumTitle, String albumCover) {
        
    }

    @Override
    public void onMediaAlbumsLoad(ArrayList<com.media.notabadplayer.Audio.AlbumInfo> albums)
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

    @Override
    public void onPlayerPlay(MediaTrack current)
    {
        _buttonPlay.setBackgroundResource(R.drawable.media_pause);
        _imageCover.setImageURI(Uri.parse(Uri.decode(current.artCover)));
        _labelTitle.setText(current.title);
        _mediaBar.setMax((int)current.durationInSeconds);
        _labelDurationTotal.setText(current.duration);
    }
    
    @Override
    public void onPlayerFinish(MediaTrack track)
    {
        _buttonPlay.setBackgroundResource(R.drawable.media_play);
    }
    
    @Override
    public void onPlayerStop()
    {

    }
    
    @Override
    public void onPlayerPause(MediaTrack track)
    {
        _buttonPlay.setBackgroundResource(R.drawable.media_play);
    }
    
    @Override
    public void onPlayerResume(MediaTrack track)
    {
        _buttonPlay.setBackgroundResource(R.drawable.media_pause);
    }
    
    @Override
    public void onPlayerVolumeChanged()
    {

    }
}