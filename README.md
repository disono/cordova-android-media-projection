# Cordova Media Projection
Cordova Media Projection is to capture device screen in real time and show it on a Surface-View.

# Installation
Latest stable version from npm:
```sh
$ cordova plugin add cordova-android-media-projection
```

Bleeding edge version from Github:
```sh
$ cordova plugin add https://github.com/disono/cordova-android-media-projection
```

# Using the plugin
Call start() to start recording screen. Stopping the recording will return path(/storage/emulated/0/Download/video-xxxxxxxxx.mp4) of video record.
```sh
mediaProjection.start([successCallback], [failureCallback]);

mediaProjection.stop();
```

# Example
```sh
mediaProjection.play(function (video_path) {
    console.log('Save Recording Path: ' + video_path);
}, function (e) {
    console.error('Error: ' + e);
});
```

# License
Cordova Media Projection is licensed under the Apache License (ASL) license. For more information, see the LICENSE file in this repository.