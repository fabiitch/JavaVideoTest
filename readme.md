Java 25
try different backend to display/record video in java
swing = jvlc ok
javafx = no good (no pure gpu)

## Media Backend impl
- gstreamer RGBA
- gstreamer NV12 (YUV)
- gstreamer openGL interop
- OpenGL PBO => to test

# Media Renderer impl
- Libgdx = RGBA, NV12, OpenGl interop
- swing 
- javaFX => hard https://github.com/eclipse-efx/efxclipse-drift

# Screen Recorder impl
- gstreamer gpu zero copy
- ffmpeg cli

backend to try
gstreamer, ffmpeg, mpv, libvlc

        ┌──────────────┐
        │  VideoPlayer │ API
        └──────┬───────┘
               │ commands
               ▼
        ┌────────────────┐
        │  MediaThread   │  Command
        └──────┬─────────┘
               │ calls
               ▼
        ┌────────────────┐
        │ VideoBackend   │  natives decode/demux
        └──────┬─────────┘
               │ emits events
               ▼
        ┌────────────────┐
        │  EventBus      │  
        └──────┬─────────┘
               │
        ┌─────▼─────┐   ┌────────────┐   ┌────────────┐
        │ UIListener│   │ StateMach. │   │ MediaClock │
        └───────────┘   └────────────┘   └────────────┘


