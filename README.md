# NotABadPlayer-Android

Created: 2019 March

Android's default music player sucks so much that I remade it with some extra features.

Platform: Android SDK 14+ (Android 4.0 IceCream Sandwich)

Usage: Media player

Technologies: Android SDK 21, Android's MediaStore, Guava (com.google.guava:guava:26.0-android)

Architectural design:

* MVP (Model=data, View=interface, Presenter=data/interface bridge, state controller)

* App consists of multiple activities and fragments, the main activity is seperate from the launch activity (launch only asks for read/write permissions), activities are forced to rely on singletons in order to their job. Its complete nonsense, single activity apps are better, there should be no need to rely on singletons to make your application work properly

* Activities communicate trough serialization objects (Java Base64 serialization style)

* Every view subclasses a BaseView interface (with one exception, CreatePlaylistActivity, because it has only one possible action)

* Every BaseView has a BasePresenter, a presenter which holds the model, makes the decision making

* The views are always responsible for UI navigation

* Upon UI interaction, views alert their presenters, which perform some logic based on the input

* BaseView and BasePresenter have a lot of duplicate methods - view usually has "performAction" type of methods,
presenter has "onEvent" type of methods, it looks messy, but surprisingly it gets the job done, UI is seperated from the logic

* So many empty interface methods of BaseView and BasePresenter... extremely slow and boring to write

* The fragments are always the observers (not the presenters), they observe the state of the audio player

Design patterns:

* Delegate - presenters and their views are delegates, both handling requests and forwarding them to each other, views are the responders to user input, who forward those events to the presenters, who, based on decision making, may or may not forward an action to their views

* Observer - some interface is updated trough a Looper singleton service, that notifies their observers when the timer elapses a specific interval; audio player notifies their observers when the audio state changes (start, pause, etc...)

* Singleton - used to easily refer to services such as the Audio Player (a wrapper of the built in android player), the Looper (repeated interval update for its clients), and the user storage used to store general info such at the app settings

* Decorator - the Audio Player wraps the android built in audio player - the Media Player

* Command - keybind actions

General design:

* CPU and energy efficient, memory ineffecient since the audio information is retrieved once and reused when trying to use the audio player

* Virtually no exceptions are thrown, the try-catch blocks usually just print errors/warnings to log

* Media storage (the library, that is, the albums/tracks found on the device) is read with the Android API MediaStore and is cached for CPU/energy efficiency

* Always single process app, if you open it from another Android app it opens a new window instead of adding an activity to the stack of the caller app

* Simple lifecycle for the components of the app: for activities/fragments. Fragments rely on onActivityCreate() to start their presenters and loopers (if they are any); AudioPlayer singleton is used to represent the player of the app and the AudioInfo singleton is used to represent the media library (audio albums and their tracks information)

* No Android Services used. Supposedly one should be used for the audio player, just in case the app gets killed from low memory, but I find that unlikely to happen because of the low memory footprint of the app; not only that but I don't like the idea of the app being killed while the audio player keeps ringing in the background

* Supports one orientation only: portrait

# Features

Bind all kinds of user actions like making the next/previous buttons jump backwards and forwards.

3 app themes, different sorting options,optional volume bar on the player screen.

CPU & energy efficient.

Includes standart player features like creating playlists, searching for tracks, controlling the audio player even when not on the player screen (a quick player is available, attached to the bottom of the screen).

Includes slighty more fancy features like jumping back to the previously played song, regardless to which album or list it belonged to.

Portrait mode only.

# Screens

Albums screen (quick screen at the bottom)

![alt text](https://github.com/felixisto/NotABadPlayer-Android/blob/master/About/1.jpg)

Player screen

![alt text](https://github.com/felixisto/NotABadPlayer-Android/blob/master/About/2.jpg)

Playlist screen

![alt text](https://github.com/felixisto/NotABadPlayer-Android/blob/master/About/3.jpg)

Search screen

![alt text](https://github.com/felixisto/NotABadPlayer-Android/blob/master/About/4.jpg)

Settings screen - keybind options

![alt text](https://github.com/felixisto/NotABadPlayer-Android/blob/master/About/8.jpg)

Dark app theme

![alt text](https://github.com/felixisto/NotABadPlayer-Android/blob/master/About/7.jpg)
