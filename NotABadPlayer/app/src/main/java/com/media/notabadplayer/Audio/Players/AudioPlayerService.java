package com.media.notabadplayer.Audio.Players;

import java.util.ArrayList;
import java.util.List;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import android.util.Log;

import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.AudioPlayer;
import com.media.notabadplayer.Audio.AudioPlayerHistory;
import com.media.notabadplayer.Audio.AudioPlayerObserver;
import com.media.notabadplayer.Audio.AudioPlayerObservers;
import com.media.notabadplayer.Audio.Model.AudioPlayOrder;
import com.media.notabadplayer.Audio.Model.AudioPlaylistBuilder;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylist;
import com.media.notabadplayer.Audio.Model.BaseAudioPlaylistBuilderNode;
import com.media.notabadplayer.Audio.Model.BaseAudioTrack;
import com.media.notabadplayer.Audio.Model.MutableAudioPlaylist;
import com.media.notabadplayer.Audio.Model.SafeMutableAudioPlaylist;
import com.media.notabadplayer.Audio.Other.AudioPlayerIdleStopTimer;
import com.media.notabadplayer.Audio.Other.AudioPlayerIdleStopDelegate;
import com.media.notabadplayer.R;
import com.media.notabadplayer.Storage.GeneralStorage;

public class AudioPlayerService extends Service implements AudioPlayer, AudioPlayerIdleStopDelegate {
    // Use to access and communicate with the audio player directly (no need for IPC communication)
    class LocalBinder extends Binder {
        AudioPlayerService getService()
        {
            return AudioPlayerService.this;
        }
    }

    private final Object _lock = new Object();
    
    private final IBinder _binder = new LocalBinder();
    private AudioPlayerServiceNotificationCenter _notificationCenter;
    private final AudioPlayerIdleStopTimer _timer = new AudioPlayerIdleStopTimer();
    
    private android.media.MediaPlayer _player;
    private @Nullable SafeMutableAudioPlaylist __unsafePlaylist;
    private AudioPlayOrder _playOrder = AudioPlayOrder.FORWARDS;

    private boolean _muted;
    
    private final AudioPlayerService.Observers _observers = new AudioPlayerService.Observers();
    private final AudioPlayerService.PlayHistory _playHistory = new AudioPlayerService.PlayHistory();

    private BroadcastReceiver playerActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            onUserInteraction();

            String value = intent.getAction();
            
            if (value != null)
            {
                Log.v(AudioPlayerService.class.getCanonicalName(), "Responding to notification action: " + value);

                performBroadcastAction(value);
            }
        }
    };

    private BroadcastReceiver userInteractionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            onUserInteraction();
        }
    };
    
    private void initialize()
    {
        _notificationCenter = new AudioPlayerServiceNotificationCenter(this);
        
        _player = new android.media.MediaPlayer();
        _player.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(android.media.MediaPlayer mp) {
                _observers.onFinish();
                try {
                    playNextBasedOnPlayOrder();
                } catch (Exception e) {
                    
                }
            }
        });

        _player.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                _observers.onFinish();
                return true;
            }
        });

        _muted = false;

        // The player has to listen to command broadcast events
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_PLAY);
        filter.addAction(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_PAUSE);
        filter.addAction(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_PREVIOUS);
        filter.addAction(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_NEXT);
        registerReceiver(playerActionReceiver, filter);

        // Events we have to listen to, in order to alert the player timers
        filter = new IntentFilter();
        filter.addAction(getResources().getString(R.string.broadcast_activity_start));
        filter.addAction(getResources().getString(R.string.broadcast_activity_pause));
        filter.addAction(getResources().getString(R.string.broadcast_keybind_action));
        registerReceiver(userInteractionReceiver, filter);

        // Player timers
        _timer.delegate = this;
        _timer.start();
    }
    
    private @NonNull Context getContext()
    {
        return getApplicationContext();
    }

    // # AudioPlayer

    public final android.media.MediaPlayer getPlayer()
    {
        return _player;
    }

    @Override
    public boolean isInitialized() {
        return _player != null;
    }

    @Override
    public boolean isPlaying()
    {
        return _player.isPlaying();
    }

    @Override
    public boolean isCompletelyStopped()
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();
        return playlist == null || !playlist.isPlaying();
    }

    @Override
    public @Nullable BaseAudioPlaylist getPlaylist()
    {
        SafeMutableAudioPlaylist playlist = getSafeMutablePlaylist();
        return playlist != null ? playlist.copy() : null;
    }
    
    @Override
    public boolean hasPlaylist()
    {
        return getSafeMutablePlaylist() != null;
    }

    private @Nullable SafeMutableAudioPlaylist getSafeMutablePlaylist()
    {
        synchronized (_lock)
        {
            return __unsafePlaylist;
        }
    }

    private void setSafeMutablePlaylist(@NonNull SafeMutableAudioPlaylist playlist)
    {
        synchronized (_lock)
        {
            __unsafePlaylist = playlist;
        }
    }

    @Override
    public AudioPlayOrder getPlayOrder()
    {
        return _playOrder;
    }

    @Override
    public void setPlayOrder(AudioPlayOrder order)
    {
        _playOrder = order;

        _observers.onPlayOrderChange(order);
    }

    @Override
    public void playPlaylist(@NonNull BaseAudioPlaylist playlist) throws Exception
    {
        playPlaylist(playlist, false);
    }

    @Override
    public void playPlaylistAndPauseImmediately(@NonNull BaseAudioPlaylist playlist) throws Exception {
        playPlaylist(playlist, true);
    }

    private void playPlaylist(@NonNull BaseAudioPlaylist playlist, boolean pauseImmediately) throws Exception {
        MutableAudioPlaylist currentPlaylist = getSafeMutablePlaylist();
        BaseAudioTrack previousTrack = currentPlaylist != null ? currentPlaylist.getPlayingTrack() : null;

        playTrack(playlist.getPlayingTrack(), previousTrack, pauseImmediately);

        SafeMutableAudioPlaylist newPlaylist = SafeMutableAudioPlaylist.build(AudioPlaylistBuilder.buildMutableFromImmutable(playlist));
        setSafeMutablePlaylist(newPlaylist);
        newPlaylist.playCurrent();
    }

    private void playTrack(@NonNull BaseAudioTrack newTrack, @Nullable BaseAudioTrack previousTrack) throws Exception
    {
        playTrack(newTrack, previousTrack, false);
    }

    private synchronized void playTrack(@NonNull BaseAudioTrack newTrack, @Nullable BaseAudioTrack previousTrack, boolean pauseImmediately) throws Exception
    {
        // Synchronized: make sure only one client enters this at a time, to make sure that the player
        // and player service are synchronized
        
        boolean isPlaying = _player.isPlaying();
        
        Uri path = Uri.parse(Uri.decode(newTrack.getFilePath()));

        _player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            _player.reset();
            _player.setDataSource(getContext(), path);
            _player.prepare();
            _player.start();
            
            if (pauseImmediately) {
                _player.pause();
            }
        }
        catch (Exception e)
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot play track, " + e.toString());
            
            // If track cannot be played, go back to previous track
            if (previousTrack != null)
            {
                Uri pathOfPreviousTrack = Uri.parse(Uri.decode(previousTrack.getFilePath()));

                try {
                    _player.reset();
                    _player.setDataSource(getContext(), pathOfPreviousTrack);
                    _player.prepare();
                    _player.start();
                    
                    if (pauseImmediately || !isPlaying)
                    {
                        _player.pause();
                    }
                } catch (Exception e2) {
                    
                }
            }
            else
            {
                stop();
            }
            
            throw e;
        }

        Log.v(AudioPlayer.class.getCanonicalName(), "Playing track: " + newTrack.getTitle());

        _playHistory.addTrack(newTrack);

        _observers.onPlay(newTrack);
        
        _notificationCenter.showNotificationForPlayingTrack(newTrack, true);

        if (pauseImmediately) {
            _observers.onPause(newTrack);
        }
    }

    @Override
    public void resume()
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }

        try
        {
            if (!isPlaying())
            {
                _player.start();

                Log.v(AudioPlayer.class.getCanonicalName(), "Resume");
                
                _observers.onResume(playlist.getPlayingTrack());

                _notificationCenter.showNotificationForPlayingTrack(playlist.getPlayingTrack(), true);
            }
        }
        catch (Exception e)
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot resume, " + e.toString());
        }
    }

    @Override
    public void pause()
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }

        try
        {
            if (isPlaying())
            {
                _player.pause();

                Log.v(AudioPlayer.class.getCanonicalName(), "Pause");
                
                _observers.onPause(playlist.getPlayingTrack());

                _notificationCenter.showNotificationForPlayingTrack(playlist.getPlayingTrack(), false);
            }
        }
        catch (Exception e)
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot pause, " + e.toString());
        }
    }

    @Override
    public void stop()
    {
        if (!hasPlaylist())
        {
            return;
        }

        try
        {
            _player.seekTo(0);

            if (isPlaying())
            {
                _player.pause();

                Log.v(AudioPlayer.class.getCanonicalName(), "Stop");
                
                _observers.onStop();

                _notificationCenter.clear();
            }
        }
        catch (Exception e)
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot stop, " + e.toString());
        }
    }

    @Override
    public void pauseOrResume()
    {
        if (!hasPlaylist())
        {
            return;
        }

        if (!isPlaying())
        {
            resume();
        }
        else
        {
            pause();
        }
    }

    @Override
    public void playNext() throws Exception
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }

        BaseAudioTrack previousTrack = playlist.getPlayingTrack();

        playlist.goToNextPlayingTrack();

        if (!isCompletelyStopped())
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Playing next track...");

            try {
                playTrack(playlist.getPlayingTrack(), previousTrack);
            } catch (Exception e) {
                Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot play next, " + e.toString());
                playlist.goToTrack(previousTrack);
                _observers.onStop();
                throw e;
            }
        }
        else
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Stop playing, got to last track");

            stop();

            _observers.onStop();
        }
    }

    @Override
    public void playPrevious() throws Exception
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }

        BaseAudioTrack previousTrack = playlist.getPlayingTrack();

        playlist.goToPreviousPlayingTrack();

        if (!isCompletelyStopped())
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Playing previous track...");

            try {
                playTrack(playlist.getPlayingTrack(), previousTrack);
            } catch (Exception e) {
                Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot play previous, " + e.toString());
                playlist.goToTrack(previousTrack);
                _observers.onPause(previousTrack);
                throw e;
            }
        }
        else
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Stop playing, cannot go before first track");

            stop();

            _observers.onStop();
        }
    }

    @Override
    public void playNextBasedOnPlayOrder() throws Exception
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }

        BaseAudioTrack previousTrack = playlist.getPlayingTrack();

        playlist.goToTrackBasedOnPlayOrder(_playOrder);

        if (!isCompletelyStopped())
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Playing next track based on play order...");

            try {
                playTrack(playlist.getPlayingTrack(), previousTrack);
            } catch (Exception e) {
                Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot play next based on play order, " + e.toString());
                playlist.goToTrack(previousTrack);
                _observers.onPause(previousTrack);
                throw e;
            }
        }
        else
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Stop playing, got to last track");

            stop();

            _observers.onStop();
        }
    }

    @Override
    public void shuffle() throws Exception
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }

        BaseAudioTrack previousTrack = playlist.getPlayingTrack();

        playlist.goToTrackByShuffle();

        if (!isCompletelyStopped())
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Playing random track...");

            try {
                playTrack(playlist.getPlayingTrack(), previousTrack);
            } catch (Exception e) {
                Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot play random, " + e.toString());
                playlist.goToTrack(previousTrack);
                _observers.onPause(previousTrack);
                throw e;
            }
        }
        else
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Stop playing, got to last track");

            stop();

            _observers.onStop();
        }
    }

    @Override
    public void jumpBackwards(int msec)
    {
        if (!hasPlaylist())
        {
            return;
        }

        int duration = getDurationMSec();
        int currentPosition = getCurrentPositionMSec();
        int destination = currentPosition - msec;
        seekTo(MathUtils.clamp(destination, 0, duration));
    }

    @Override
    public void jumpForwards(int msec)
    {
        if (!hasPlaylist())
        {
            return;
        }

        int duration = getDurationMSec();
        int currentPosition = getCurrentPositionMSec();
        int destination = currentPosition + msec;
        seekTo(MathUtils.clamp(destination, 0, duration));
    }

    @Override
    public int getDurationMSec()
    {
        return _player.getDuration() / 1000;
    }

    @Override
    public int getCurrentPositionMSec()
    {
        return _player.getCurrentPosition() / 1000;
    }

    @Override
    public void seekTo(int msec)
    {
        if (!hasPlaylist())
        {
            return;
        }

        msec *= 1000;

        int destination = msec;

        try
        {
            if (destination < _player.getDuration())
            {
                _player.seekTo(msec);
            }
            else
            {
                _player.seekTo(0);
            }

            Log.v(AudioPlayer.class.getCanonicalName(), "Seek to " + String.valueOf(destination));
        }
        catch (Exception e)
        {
            Log.v(AudioPlayer.class.getCanonicalName(), "Error: cannot seek to, " + e.toString());
        }
    }

    @Override
    public int getVolume()
    {
        AudioManager manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
        double max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        double result = (manager.getStreamVolume(AudioManager.STREAM_MUSIC) / max) * 100;
        return (int)result;
    }

    @Override
    public void setVolume(int volume)
    {
        if (volume < 0)
        {
            volume = 0;
        }

        AudioManager manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);

        double v = (double)volume;
        double max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        double result = (v / 100.0) * max;
        result = result > max ? max : result;

        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)result,0);
    }

    @Override
    public void volumeUp()
    {
        AudioManager manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);

        int currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int incrementVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 10;
        int result = currentVolume + incrementVolume;

        manager.setStreamVolume(AudioManager.STREAM_MUSIC, result,0);
    }

    @Override
    public void volumeDown()
    {
        AudioManager manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);

        int currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int incrementVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 10;
        int result = currentVolume - incrementVolume > 0 ? currentVolume - incrementVolume : 0;

        manager.setStreamVolume(AudioManager.STREAM_MUSIC, result,0);
    }

    @Override
    public boolean isMuted()
    {
        return _muted;
    }

    @Override
    public void muteOrUnmute()
    {
        if (!_muted)
        {
            mute();
        }
        else
        {
            unmute();
        }
    }

    @Override
    public void mute()
    {
        if (!_muted)
        {
            _player.setVolume(0, 0);

            _muted = true;

            Log.v(AudioPlayer.class.getCanonicalName(), "Mute");
        }
    }

    @Override
    public void unmute()
    {
        if (_muted)
        {
            _player.setVolume(1, 1);

            _muted = false;

            Log.v(AudioPlayer.class.getCanonicalName(), "Unmute");
        }
    }

    @Override
    public @NonNull AudioPlayerObservers observers()
    {
        return _observers;
    }
    
    @Override
    public @NonNull AudioPlayerHistory playHistory()
    {
        return _playHistory;
    }

    // # Broadcast

    private void performBroadcastAction(@NonNull String value)
    {
        MutableAudioPlaylist playlist = getSafeMutablePlaylist();

        if (playlist == null)
        {
            return;
        }
        
        if (value.equals(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_PLAY))
        {
            resume();
        }

        if (value.equals(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_PAUSE))
        {
            pause();
        }

        if (value.equals(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_PREVIOUS))
        {
            if (!playlist.isPlayingFirstTrack())
            {
                try {
                    playPrevious();
                } catch (Exception e) {
                    
                }
            }
            else
            {
                pause();
            }
        }

        if (value.equals(AudioPlayerServiceNotificationCenter.BROADCAST_ACTION_NEXT))
        {
            if (!playlist.isPlayingLastTrack())
            {
                try {
                    playNext();
                } catch (Exception e) {

                }
            }
            else
            {
                pause();
            }
        }
    }

    // # Service

    @Override
    public IBinder onBind(Intent intent)
    {
        return _binder;
    }

    @Override
    public void onCreate() 
    {
        super.onCreate();
        
        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        Log.v(AudioPlayerService.class.getCanonicalName(), "Started!");
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        unregisterReceiver(playerActionReceiver);
        unregisterReceiver(userInteractionReceiver);

        stop();

        _timer.stop();
        
        Log.v(AudioPlayerService.class.getCanonicalName(), "Destroyed!");
        
        // Cancel the persistent notification.
        _notificationCenter.clear();
    }

    // # Observers
    public class Observers implements AudioPlayerObservers
    {
        private final Object lock = new Object();

        private ArrayList<AudioPlayerObserver> _observers = new ArrayList<>();

        ArrayList<AudioPlayerObserver> observersCopy() {
            synchronized (lock) {
                return new ArrayList<>(_observers);
            }
        }

        @Override
        public void attach(@NonNull AudioPlayerObserver observer)
        {
            synchronized (lock) {
                if (_observers.contains(observer))
                {
                    return;
                }

                _observers.add(observer);
            }

            fullyUpdateObserver(observer);
        }

        @Override
        public void detach(@NonNull AudioPlayerObserver observer)
        {
            synchronized (lock) {
                _observers.remove(observer);
            }
        }

        private void fullyUpdateObserver(AudioPlayerObserver observer)
        {
            observer.onPlayOrderChange(_playOrder);
        }

        private void onPlay(@NonNull BaseAudioTrack track)
        {
            ArrayList<AudioPlayerObserver> observers = observersCopy();

            for (int e = 0; e < observers.size(); e++) {observers.get(e).onPlayerPlay(track);}
        }

        private void onFinish()
        {
            ArrayList<AudioPlayerObserver> observers = observersCopy();

            for (int e = 0; e < observers.size(); e++) {observers.get(e).onPlayerFinish();}
        }

        private void onStop()
        {
            ArrayList<AudioPlayerObserver> observers = observersCopy();

            for (int e = 0; e < observers.size(); e++) {observers.get(e).onPlayerStop();}
        }

        private void onResume(@NonNull BaseAudioTrack track)
        {
            ArrayList<AudioPlayerObserver> observers = observersCopy();

            for (int e = 0; e < observers.size(); e++) {observers.get(e).onPlayerResume(track);}
        }

        private void onPause(@NonNull BaseAudioTrack track)
        {
            ArrayList<AudioPlayerObserver> observers = observersCopy();

            for (int e = 0; e < observers.size(); e++) {observers.get(e).onPlayerPause(track);}
        }

        private void onPlayOrderChange(AudioPlayOrder order)
        {
            ArrayList<AudioPlayerObserver> observers = observersCopy();

            for (int e = 0; e < observers.size(); e++) {observers.get(e).onPlayOrderChange(order);}
        }
    }

    // Handles any generic user input.
    // The player usually alerts its timers here, since some of them are dependant on knowing
    // when was the last time the user interacted with the app.
    void onUserInteraction()
    {
        _timer.onUserInteraction();
    }

    // # AudioPlayerServiceIdleTimerDelegate

    @Override
    public void handleIdle()
    {
        pause();
    }

    // # PlayHistory
    public class PlayHistory implements AudioPlayerHistory
    {
        private final Object lock = new Object();

        private final ArrayList<BaseAudioTrack> _playHistory = new ArrayList<>();

        @Override
        public @NonNull List<BaseAudioTrack> getPlayHistory()
        {
            synchronized (lock) {
                return new ArrayList<>(_playHistory);
            }
        }

        @Override
        public void setList(@NonNull List<BaseAudioTrack> playHistory)
        {
            synchronized (lock) {
                _playHistory.clear();
                _playHistory.addAll(playHistory);
            }
        }

        @Override
        public void playPreviousInHistory(@NonNull AudioInfo audioInfo) throws Exception
        {
            stop();

            MutableAudioPlaylist playlist = getSafeMutablePlaylist();

            if (playlist == null)
            {
                return;
            }

            BaseAudioTrack previousTrack;
            BaseAudioTrack previouslyPlayed;

            synchronized (lock) {
                if (_playHistory.size() <= 1)
                {
                    return;
                }

                _playHistory.remove(0);

                previousTrack = playlist.getPlayingTrack();
                previouslyPlayed = _playHistory.get(0);
            }

            BaseAudioPlaylist sourcePlaylist = previouslyPlayed.getSource().getSourcePlaylist(audioInfo, previouslyPlayed);
            MutableAudioPlaylist newPlaylist;

            if (sourcePlaylist == null)
            {
                // Create a playlist with just one track
                String playlistName = getContext().getResources().getString(R.string.playlist_name_previously_played);

                BaseAudioPlaylistBuilderNode node = AudioPlaylistBuilder.start();
                node.setName(playlistName);
                node.setTracksToOneTrack(previouslyPlayed);

                newPlaylist = node.buildMutable();
            }
            else
            {
                newPlaylist = AudioPlaylistBuilder.buildMutableFromImmutable(sourcePlaylist);
            }

            try {
                playPlaylist(newPlaylist);
            } catch (Exception e) {
                Log.v(Player.class.getCanonicalName(), "Error: cannot play previous from play history, " + e.toString());
                playlist.goToTrack(previousTrack);
                _observers.onStop();
                throw e;
            }
        }

        private void addTrack(@NonNull BaseAudioTrack newTrack)
        {
            int capacity = GeneralStorage.getShared().getPlayerPlayedHistoryCapacity();

            synchronized (lock) {
                // Make sure that the history tracks are unique
                for (BaseAudioTrack track : _playHistory)
                {
                    if (track.equals(newTrack))
                    {
                        _playHistory.remove(track);
                        break;
                    }
                }

                _playHistory.add(0, newTrack);

                // Do not exceed the play history capacity
                while (_playHistory.size() > capacity)
                {
                    _playHistory.remove(_playHistory.size()-1);
                }
            }
        }
    }
}
