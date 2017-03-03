'use strict';

var exec = require('cordova/exec');

var mediaProjection = {};

mediaProjection.start = function(success, failure) {
    // fire
    exec(
        success,
        failure,
        'CordovaMediaProjection',
        'start',
        []
    );
};

mediaProjection.stop = function(success, failure) {
    // fire
    exec(
        success,
        failure,
        'CordovaMediaProjection',
        'stop',
        []
    );
};

module.exports = mediaProjection;