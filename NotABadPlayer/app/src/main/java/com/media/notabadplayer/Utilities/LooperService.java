package com.media.notabadplayer.Utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import android.os.Handler;
import android.os.Looper;
import com.google.common.base.Function;

public class LooperService {
    public static int LOOPER_INTERVAL_MSEC = 100;

    private static LooperService singleton;
    
    private final ClientsManager _clientsManager = new ClientsManager();

    private final Handler _mainHandler;
    private final Runnable _loopingRunnable;

    private final Handler _backgroundHandler;
    
    private LooperService()
    {
        _mainHandler = new Handler(Looper.getMainLooper());
        _loopingRunnable = new Runnable() {
            @Override
            public void run() {
                loop();
                loopAgain();
            }
        };

        _backgroundHandler = new Handler();
        
        loopAgain();
    }

    synchronized public static LooperService getShared()
    {
        if (singleton == null)
        {
            singleton = new LooperService();
        }

        return singleton;
    }

    public void subscribe(final LooperClient client)
    {
        _clientsManager.subscribe(client);
    }

    public void unsubscribe(final LooperClient client)
    {
        _clientsManager.unsubscribe(client);
    }
    
    public void runOnMain(final Function<Void, Void> callback)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                callback.apply(null);
            }
        };
        
        _mainHandler.postDelayed(runnable, 1);
    }

    public void runOnBackground(final Function<Void, Void> callback)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                callback.apply(null);
            }
        };

        _backgroundHandler.postDelayed(runnable, 1);
    }

    private void loop()
    {
        _clientsManager.loop();
    }

    private void loopAgain()
    {
        _mainHandler.postDelayed(_loopingRunnable, LOOPER_INTERVAL_MSEC);
    }
    
    class ClientsManager 
    {
        private ArrayList<LooperClient> _clients = new ArrayList<>();
        
        private final Object mainLock = new Object();
        
        void subscribe(final LooperClient client)
        {
            synchronized (mainLock)
            {
                if (!_clients.contains(client))
                {
                    _clients.add(client);
                }
            }
        }

        void unsubscribe(final LooperClient client)
        {
            synchronized (mainLock)
            {
                _clients.remove(client);
            }
        }
        
        void loop()
        {
            // This must be called on the main thread
            Collection<LooperClient> clients;

            synchronized (mainLock)
            {
                clients = CollectionUtilities.copy(_clients);
            }
            
            for (LooperClient client : clients)
            {
                client.loop();
            }
        }
    }
}
