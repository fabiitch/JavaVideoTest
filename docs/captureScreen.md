## 🖥️ 1. GDI / BitBlt (ancienne méthode)

Principe : capture via l’API GDI, BitBlt sur le DC de l’écran.

Avantages : ultra simple, fonctionne partout (toutes versions Windows).

Inconvénients : lent (CPU bound), pas optimisé pour le temps réel (beaucoup de copie mémoire).
👉 Suffisant pour un screenshot ou capture lente, mais pas pour du recording fluide (60 fps).

##  🎥 2. DirectX Desktop Duplication API (DXGI)

Principe : dispo depuis Windows 8, tu récupères les frames directement via la carte graphique.

Avantages : rapide, accès direct GPU → CPU, utilisé par OBS, ShadowPlay, etc.

Inconvénients : un peu plus technique, nécessite DirectX 11+.

Cas d’usage : recording sérieux (overlay, jeux, 60+ FPS).

##  📹 3. Windows Graphics Capture API (WinRT, Windows 10+)

Principe : API moderne, tu sélectionnes une fenêtre ou tout l’écran via GraphicsCaptureItem.

Avantages : perf proches de DXGI, API haut niveau, permet de capturer une fenêtre spécifique même si elle est partiellement masquée.

Inconvénients : Windows 10 minimum.
👉 C’est ce qu’OBS a ajouté récemment pour remplacer certaines parties de Desktop Duplication.

## ⚡ 4. Windows Media Foundation (MF)

Principe : framework pour encoder/décoder vidéo. Tu peux plugger la capture DXGI/GraphicsCapture dedans.

Avantages : pipeline complet pour encoder direct en H.264/HEVC sans passer par ffmpeg.

Inconvénients : un peu usine à gaz, mais utile si tu veux sortir des fichiers compressés direct.

## 🛠️ 5. FFmpeg / libav

Principe : wrapper multi-plateforme. Sur Windows, FFmpeg utilise souvent DXGI ou GDI selon options.

Avantages : rapide à mettre en place, beaucoup d’options d’encodage, portable.

Inconvénients : dépendance lourde, tu perds un peu de contrôle bas niveau.

## 🎮 6. API Vendor (NVIDIA NVFBC / AMD AMF / Intel QuickSync)

Principe : capture via GPU driver (NVFBC = Nvidia Frame Buffer Capture).

Avantages : très perf (quasi 0 overhead, encode hardware direct).

Inconvénients : souvent réservé aux apps “whitelisted” (genre OBS, ShadowPlay). Pas accessible facilement sans SDKs spéciaux.
