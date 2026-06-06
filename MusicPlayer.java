class MusicPlayer {
 
    private static Clip clip = null;
    private static boolean muted = false;
    private static float currentVolumeGain = 1.0f; // Stores user intent volume
    private static String currentTrackPath = null;
 
    /** Starts looping the given WAV file. Stops any current track first. */
    public static synchronized void play(String wavPath) {
        currentTrackPath = wavPath;
        stop();
        Thread thread = new Thread(() -> {
            try {
                File file = new File(wavPath);
                if (!file.exists()) {
                    System.err.println("[MusicPlayer] File not found: " + wavPath);
                    return;
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                synchronized (MusicPlayer.class) {
                    clip = AudioSystem.getClip();
                    clip.open(ais);
                    
                    // Apply volume constraint before starting based on mute state
                    applyVolume(muted ? 0f : currentVolumeGain);
                    
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    clip.start();
                }
            } catch (UnsupportedAudioFileException e) {
                System.err.println("[MusicPlayer] Unsupported format (needs 16-bit PCM WAV): " + e.getMessage());
            } catch (LineUnavailableException e) {
                System.err.println("[MusicPlayer] Audio line unavailable: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("[MusicPlayer] IO error: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
 
    /** Stops playback immediately. */
    public static synchronized void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
 
    /**
     * Sets volume. 0.0f = silent, 1.0f = full.
     */
    public static synchronized void setVolume(float gain) {
        currentVolumeGain = Math.max(0f, Math.min(1f, gain));
        if (!muted) {
            applyVolume(currentVolumeGain);
        }
    }

    /** Internal helper that changes the data line mixer volume control directly */
    private static void applyVolume(float targetGain) {
        if (clip == null) return;
        try {
            FloatControl vol = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = targetGain == 0f ? vol.getMinimum() : 20f * (float) Math.log10(targetGain);
            vol.setValue(Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), dB)));
        } catch (IllegalArgumentException e) {
            System.err.println("[MusicPlayer] Volume control not supported.");
        }
    }

    /** Toggles the system mute state safely */
    public static synchronized void toggleMute() {
        muted = !muted;
        applyVolume(muted ? 0f : currentVolumeGain);
    }

    /** Returns current mute configuration status */
    public static synchronized boolean isMuted() {
        return muted;
    }
}