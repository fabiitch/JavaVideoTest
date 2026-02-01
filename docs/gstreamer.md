## Install Gstreamer

DL gstreamer
https://gstreamer.freedesktop.org/download/#windows
MSVC 64-bit (VS 2019, Release CRT)
1.26.6 runtime installer

extract
msiexec /a "gstreamer-1.0-msvc-x86_64-1.26.7.msi" /qb TARGETDIR="C:\Temp\gstreamer-runtime"

GO dans C:\Temp

go C:\temp\gstreamer-runtime\PFiles64\gstreamer\1.0\msvc_x86_64\bin
test dans /bin
./gst-launch-1.0.exe videotestsrc ! autovideosink

copy dans java
copy de C:\temp\gstreamer-runtime\PFiles64\gstreamer\1.0\msvc_x86_64
Pas besoin de garder include/ → c’est pour le dev C/C++.
[share](/1.0/msvc_x86_64/share)
[bin](/1.0/msvc_x86_64/bin)
[etc](/1.0/msvc_x86_64/etc)
[include](/1.0/msvc_x86_64/include)
[lib](/1.0/msvc_x86_64/lib)
[libexec](/1.0/msvc_x86_64/libexec)


-------------------------

// test again sur gitbash

./gst-launch-1.0 playbin uri="file:///C:/Users/fabocc/Desktop/dossier_perso/unfearWorkspace/LoLCopilot/ScreenRecorder/src/test/resources/video/1080p/blue.mp4"

./gst-launch-1.0 filesrc location="C:/Users/fabocc/Desktop/dossier_perso/unfearWorkspace/LoLCopilot/ScreenRecorder/src/test/resources/video/1080p/blue.mp4" ! decodebin ! videoconvert ! videoscale ! 'video/x-raw,format=RGBA' ! autovideosink




https://gitlab.freedesktop.org/gstreamer/gstreamer/-/tree/main/subprojects/gst-libav


https://gstreamer.freedesktop.org/data/pkg/windows/1.26.4/msvc/


Commande gstreamerTest

gst-launch-1.0 -q d3d11screencapture cursor=true monitor=0 ! fakesink
