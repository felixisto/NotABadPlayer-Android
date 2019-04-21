package com.media.notabadplayer.Audio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;

import com.media.notabadplayer.Storage.GeneralStorage;

public class AudioTrackSource implements Serializable
{
    private final String _value;
    private final boolean _isAlbumSource;

    public static AudioTrackSource createAlbumSource(@NonNull String albumID)
    {
        return new AudioTrackSource(albumID, true);
    }
    
    public static AudioTrackSource createPlaylistSource(@NonNull String playlistName)
    {
        return new AudioTrackSource(playlistName, false);
    }
    
    private AudioTrackSource(String value, boolean isAlbumSource)
    {
        this._value = value;
        this._isAlbumSource = isAlbumSource;
    }
    
    public boolean isAlbum()
    {
        return _isAlbumSource;
    }
    
    public boolean isPlaylist()
    {
        return !isAlbum();
    }

    public @Nullable AudioPlaylist getSourcePlaylist(@NonNull AudioInfo audioInfo, @Nullable AudioTrack playingTrack)
    {
        if (isAlbum())
        {
            AudioAlbum album = audioInfo.getAlbumByID(_value);
            
            if (album == null)
            {
                return null;
            }
            
            return new AudioPlaylist(album.albumTitle, audioInfo.getAlbumTracks(album), playingTrack);
        }
        
        if (isPlaylist())
        {
            ArrayList<AudioPlaylist> playlists = GeneralStorage.getShared().getUserPlaylists();
            
            if (playlists == null)
            {
                return null;
            }
            
            for (int e = 0; e < playlists.size(); e++)
            {
                if (_value.equals(playlists.get(e).getName()))
                {
                    AudioPlaylist pl = playlists.get(e);
                    return new AudioPlaylist(pl.getName(), pl.getTracks(), playingTrack);
                }
            }
            
            return null;
        }
        
        return null;
    }
}
