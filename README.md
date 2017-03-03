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

mediaProjection.stop([successCallback], [failureCallback]);
```

# Example
```sh
mediaProjection.start();

mediaProjection.stop(function (video_path) {
    console.log('Save Recording Path: ' + video_path);
}, function (e) {
    console.error('Error: ' + e);
});
```

# Note
```sh
1. It cannot be used on API version lower than 21
2. Update your config.xml and add this <preference name="android-minSdkVersion" value="21" />
3. Minimum supported Gradle version is 3.3 (Update the GradleBuilder.js find the distributionUrl and replace with var distributionUrl = process.env['CORDOVA_ANDROID_GRADLE_DISTRIBUTION_URL'] || 'https\\://services.gradle.org/distributions/gradle-3.3-all.zip';)
```

# License
Cordova Media Projection is licensed under the Apache License (ASL) license. For more information, see the LICENSE file in this repository.