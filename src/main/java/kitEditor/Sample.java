package kitEditor;

import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;

class Sample {
    private final String name;
    private short[] originalSamples;
    private short[] processedSamples;
    private int readPos;
    private int volumeDb = 0;

    // Noise level picked by ear. Tested by generating a slow DC slope,
    // dithering and truncating to 4-bit. With this noise level, there
    // are no noticeable transitions between Game Boy volumes.
    // final double noiseLevel = 1400;
    private int ditherDb = -30;

    public Sample(short[] iBuf, String iName) {
        if (iBuf != null) {
            for (int j : iBuf) {
                assert (j >= Short.MIN_VALUE);
                assert (j <= Short.MAX_VALUE);
            }
            processedSamples = iBuf;
        }
        name = iName;
    }

    public String getName() {
        return name;
    }

    public int lengthInSamples() {
        return processedSamples.length;
    }

    public short[] workSampleData() {
        return (originalSamples != null ? originalSamples : processedSamples).clone();
    }

    public int lengthInBytes() {
        int l = lengthInSamples() / 2;
        l -= l % 0x10;
        return l;
    }

    public void seekStart() {
        readPos = 0;
    }

    public short read() {
        return processedSamples[readPos++];
    }

    public boolean canAdjustVolume() {
        return originalSamples != null;
    }

    // ------------------

    static Sample createFromNibbles(byte[] nibbles, String name) {
        short[] buf = new short[nibbles.length * 2];
        for (int nibbleIt = 0; nibbleIt < nibbles.length; ++nibbleIt) {
            buf[2 * nibbleIt] = (byte) (nibbles[nibbleIt] & 0xf0);
            buf[2 * nibbleIt + 1] = (byte) ((nibbles[nibbleIt] & 0xf) << 4);
        }
        for (int bufIt = 0; bufIt < buf.length; ++bufIt) {
            short s = (byte)(buf[bufIt] - 0x80);
            s *= 256;
            buf[bufIt] = s;
        }
        return new Sample(buf, name);
    }

    // ------------------

    public static Sample createFromWav(File file, boolean dither) throws IOException, UnsupportedAudioFileException {
        Sample s = new Sample(null, file.getName());
        s.originalSamples = readSamples(file);
        s.processSamples(dither);
        return s;
    }

    public static Sample createFromOriginalSamples(short[] pcm, String name, int volume, int dither) {
        Sample sample = new Sample(null, name);
        sample.setVolumeDb(volume);
        sample.setDitherDb(dither);
        sample.originalSamples = pcm;
        sample.processSamples(true);
        return sample;
    }

    public void processSamples(boolean dither) {
        short[] samples = originalSamples.clone();
        normalize(samples);
        if (dither) {
            dither(samples);
        }
        blendWaveFrames(samples);
        processedSamples = samples;
    }

    /* Due to Game Boy audio bug, the first sample in a frame is played
     * back using the same value as the last completed sample in previous
     * frame. To reduce error, average these samples.
     */
    private static void blendWaveFrames(short[] samples) {
        for (int i = 0x20; i < samples.length; i += 0x20) {
            int n = 2; // Tested on DMG-01 with 440 Hz sine wave.
            short avg = (short) ((samples[i] + samples[i - n]) / 2);
            samples[i] = avg;
            samples[i - n] = avg;
        }
    }

    private static short[] readSamples(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        AudioFormat outFormat = new AudioFormat(11468, 16, 1, true, false);
        AudioInputStream convertedAis = AudioSystem.getAudioInputStream(outFormat, ais);
        ArrayList<Short> samples = new ArrayList<>();
        while (true) {
            byte[] buf = new byte[2];
            if (convertedAis.read(buf) < 2) {
                break;
            }
            short sample = buf[1];
            sample *= 256;
            sample += (short)buf[0] & 0xff;
            samples.add(sample);
        }
        short[] shortBuf = new short[samples.size()];
        for (int i = 0; i < shortBuf.length; ++i) {
            shortBuf[i] = samples.get(i);
        }
        return shortBuf;
    }

    private static void dither(short[] samples) {
        PinkNoise pinkNoise = new PinkNoise(1);
        for (int i = 0; i < samples.length; ++i) {
            int s = samples[i];
            double noiseLevel = Short.MAX_VALUE * Math.pow(10, ditherDb / 20.0);
            s += pinkNoise.nextValue() * noiseLevel;
            s = Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, s));
            samples[i] = (short)s;
        }
    }

    private void normalize(short[] samples) {
        double peak = Double.MIN_VALUE;
        for (Short sample : samples) {
            double s = sample;
            s = s < 0 ? s / Short.MIN_VALUE : s / Short.MAX_VALUE;
            peak = Math.max(s, peak);
        }
        if (peak == 0) {
            return;
        }
        double volumeAdjust = Math.pow(10, volumeDb / 20.0);
        for (int i = 0; i < samples.length; ++i) {
            samples[i] = (short)((samples[i] * volumeAdjust) / peak);
        }
    }

    public int ditherDb() {
        return ditherDb;
    }

    public void setDitherDb(int value) {
        ditherDb = value;
    }

    public int volumeDb() {
        return volumeDb;
    }

    public void setVolumeDb(int value) {
        volumeDb = value;
    }
}
