jextract `
  --target-package com.nz.media.gst.panama `
--output "G:\info\workspace\LoLCopilot\gdx-video-engine\native\generated" `
  -I "G:\sdk\gstreamer\dev\1.0\msvc_x86_64\include\gstreamer-1.0" `
-I "G:\sdk\gstreamer\dev\1.0\msvc_x86_64\include\glib-2.0" `
  -I "G:\sdk\gstreamer\dev\1.0\msvc_x86_64\lib\glib-2.0\include" `
"G:\info\workspace\LoLCopilot\gdx-video-engine\native\gst\gst_min.h"

C:\Program Files\gstreamer\1.0\msvc_x86_64\bin
(1) Ajoute --header-class-name (optionnel mais pratique)
Ça te fait une classe d’entrypoint plus claire.
