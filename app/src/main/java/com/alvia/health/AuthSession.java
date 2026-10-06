package com.alvia.health;
/** Deliberately memory-only: process restart requires authentication again. */
final class AuthSession {
    private static volatile boolean unlocked;
    private static long externalUntil;
    static volatile boolean restore;
    static volatile boolean profileAccess;
    static synchronized boolean unlocked(){if(externalUntil>0&&android.os.SystemClock.elapsedRealtime()>externalUntil)lock();return unlocked;}
    static synchronized void unlock(){unlocked=true;externalUntil=0;}
    static synchronized void lock(){unlocked=false;externalUntil=0;}
    /** Short, memory-only return window for a user-initiated system screen. */
    static synchronized void beginExternalFlow(){if(unlocked())externalUntil=android.os.SystemClock.elapsedRealtime()+5*60*1000;}
    static synchronized void cancelExternalFlow(){externalUntil=0;}
    static synchronized void background(){if(externalUntil==0||!unlocked())lock();}
    static synchronized void foreground(){unlocked();externalUntil=0;}
}
