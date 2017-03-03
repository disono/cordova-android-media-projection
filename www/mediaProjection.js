'use strict';

var exec = require('cordova/exec');

var mediaProjection = {};

mediaProjection.start = function(success, failure) {
    // fire
    exec(
        success,
        failure,
        'MediaProjection',
        'start',
        []
    );
};

mediaProjection.stop = function(success, failure) {
    // fire
    exec(
        success,
        failure,
        'MediaProjection',
        'stop',
        []
    );
};

module.exports = mediaProjection;