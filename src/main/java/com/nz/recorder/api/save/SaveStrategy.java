package com.nz.recorder.api.save;

/**
 * Save behavior.
 * FILE means the recorder writes a container (mp4/mkv) to a path.
 * STREAM is a placeholder for a future streaming sink (rtmp/webrtc/etc).
 * NO_SAVE means "recording exists only in memory / nowhere" (useful with frame taps or ring-buffer without dump).
 */
public sealed interface SaveStrategy permits NoSave, FileSave {}
