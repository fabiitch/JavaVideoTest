package com.nz.media.backend;

public enum BackendState {
    NEW,          // instance créée, rien ouvert
    OPENING,      // open() appelé, init async en cours
    READY,        // pipeline prêt (preroll OK), commandes “safe”
    PLAYING,
    PAUSED,
    SEEKING,      // optionnel (si tu veux tracer une phase)
    STOPPED,      // optionnel (si tu as stop())
    ENDED,        // fin de média
    ERROR,
    CLOSED
}
