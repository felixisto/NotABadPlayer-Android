package com.media.notabadplayer.Storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;

import com.media.notabadplayer.Audio.AudioAlbum;
import com.media.notabadplayer.Audio.AudioTrack;
import com.media.notabadplayer.Audio.AudioTrackSource;
import com.media.notabadplayer.Utilities.MediaSorting;

public class AudioInfo {
    private Context _context;
    private ArrayList<AudioAlbum> _albums = new  ArrayList<>();
    private HashMap<String, ArrayList<AudioTrack>> _albumSongs = new HashMap<>();
    
    public AudioInfo(Context context)
    {
        _context = context;
    }
    
    synchronized private void load()
    {
        if (_albums.size() > 0)
        {
            return;
        }
        
        Cursor cursor = _context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                null, null, null, null);
        
        if (cursor == null)
        {
            return;
        }
        
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
        int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
        int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums._ID);
        int albumArtColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
        
        while (cursor.moveToNext())
        {
            long albumID = cursor.getLong(albumIdColumn);
            String title = cursor.getString(titleColumn);
            String artist = cursor.getString(artistColumn);
            String cover = cursor.getString(albumArtColumn);
            
            AudioAlbum a = new AudioAlbum(String.valueOf(albumID), artist, title, cover != null ? cover : "");
            _albums.add(a);
        }
        
        cursor.close();
    }
    
    synchronized public @NonNull ArrayList<AudioAlbum> getAlbums()
    {
        if (_albums.size() > 0)
        {
            return _albums;
        }
        
        load();
        
        MediaSorting.sortAlbumsByTitle(_albums);
        
        return _albums;
    }
    
    synchronized public AudioAlbum getAlbumByID(String identifier)
    {
        ArrayList<AudioAlbum> albums = getAlbums();
        
        for (AudioAlbum album: albums)
        {
            if (album.albumID.equals(identifier))
            {
                return album;
            }
        }
        
        return null;
    }
    
    synchronized public @NonNull ArrayList<AudioTrack> getAlbumTracks(AudioAlbum album)
    {
        if (!_albumSongs.containsKey(album.albumID))
        {
            _albumSongs.put(album.albumID, new ArrayList<AudioTrack>());
        }
        else
        {
            return _albumSongs.get(album.albumID);
        }
        
        ArrayList<AudioTrack> albumTracks = _albumSongs.get(album.albumID);
        
        if (albumTracks == null)
        {
            return new ArrayList<>();
        }
        
        String projection[] = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK
        };
        
        String selection = "is_music != 0";
        
        if (Integer.parseInt(album.albumID) > 0) 
        {
            selection = selection + " and album_id = " + album.albumID;
        }
        
        String orderBy = null;
        
        Cursor cursor = _context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, orderBy);
        
        if (cursor == null)
        {
            return new ArrayList<>();
        }

        int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumTitleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        
        while (cursor.moveToNext())
        {
            String filePath = cursor.getString(dataColumn);
            String title = cursor.getString(titleColumn);
            String artist = cursor.getString(artistColumn);
            String albumTitle = cursor.getString(albumTitleColumn);
            int trackNum = cursor.getInt(trackNumColumn);
            double duration = cursor.getLong(durationColumn) / 1000.0;
            
            albumTracks.add(new AudioTrack(filePath, 
                            title,
                            artist,
                            albumTitle,
                            album.albumCover,
                            trackNum, 
                            duration,
                            AudioTrackSource.create(album)));
        }

        cursor.close();
        
        return albumTracks;
    }
    
    synchronized public @NonNull ArrayList<AudioTrack> searchForTracks(String query)
    {
        ArrayList<AudioTrack> albumTracks = new ArrayList<>();
        
        String projection[] = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK
        };
        
        String selection = MediaStore.Audio.Media.TITLE + " LIKE ?";
        String[] selectionArgs = new String[] {""};

        String[] words = query.split(" ");
        
        for (int i = 0; i < words.length; i++) 
        {
            selectionArgs[0] += "%" + words[i] + "%";
        }
        
        String orderBy = null;

        Cursor cursor = _context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, orderBy);
        
        if (cursor == null)
        {
            return new ArrayList<>();
        }
        
        int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumTitleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumID = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        
        while (cursor.moveToNext())
        {
            String filePath = cursor.getString(dataColumn);
            String title = cursor.getString(titleColumn);
            String artist = cursor.getString(artistColumn);
            String albumTitle = cursor.getString(albumTitleColumn);
            int trackNum = cursor.getInt(trackNumColumn);
            double duration = cursor.getLong(durationColumn) / 1000.0;
            String albumCover = "";
            AudioAlbum album = getAlbumByID(cursor.getString(albumID));
            albumCover = album.albumCover;
            
            albumTracks.add(new AudioTrack(filePath,
                            title, 
                            artist, 
                            albumTitle,
                            albumCover, 
                            trackNum,
                            duration,
                            AudioTrackSource.create(album)));
        }

        cursor.close();
        
        return albumTracks;
    }

    synchronized public AudioTrack findTrackByPath(Uri path)
    {
        String projection[] = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK
        };

        Cursor cursor = _context.getContentResolver().query(
                path,
                projection, null, null, null);
        
        if (cursor == null)
        {
            return null;
        }
        
        cursor.moveToNext();

        int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumTitleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumID = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        
        String filePath = cursor.getString(dataColumn);
        String title = cursor.getString(titleColumn);
        String artist = cursor.getString(artistColumn);
        String albumTitle = cursor.getString(albumTitleColumn);
        int trackNum = cursor.getInt(trackNumColumn);
        double duration = cursor.getLong(durationColumn) / 1000.0;
        String albumCover = "";
        AudioAlbum album = getAlbumByID(cursor.getString(albumID));
        albumCover = album.albumCover;
        
        cursor.close();
        
        return new AudioTrack(filePath,
                              title,
                              artist,
                              albumTitle,
                              albumCover,
                              trackNum,
                              duration,
                              AudioTrackSource.create(album));
    }
}
