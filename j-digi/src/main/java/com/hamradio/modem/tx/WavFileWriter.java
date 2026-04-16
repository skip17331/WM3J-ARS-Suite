package com.hamradio.modem.tx;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WavFileWriter {

    private WavFileWriter() {
    }

    public static void writeMono16(Path path,
                                   float sampleRate,
                                   SampleSource source) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("SampleSource cannot be null");
        }
        if (sampleRate <= 0.0f) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }

        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        byte[] pcmData = readAllPcm16(source);
        int dataSize = pcmData.length;

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(path.toFile())))) {

            writeAscii(out, "RIFF");
            writeLittleEndianInt(out, 36 + dataSize);
            writeAscii(out, "WAVE");

            writeAscii(out, "fmt ");
            writeLittleEndianInt(out, 16);                 // PCM chunk size
            writeLittleEndianShort(out, (short) 1);       // PCM format
            writeLittleEndianShort(out, (short) 1);       // mono
            writeLittleEndianInt(out, Math.round(sampleRate));
            writeLittleEndianInt(out, Math.round(sampleRate) * 2); // byte rate
            writeLittleEndianShort(out, (short) 2);       // block align
            writeLittleEndianShort(out, (short) 16);      // bits/sample

            writeAscii(out, "data");
            writeLittleEndianInt(out, dataSize);
            out.write(pcmData);
        } finally {
            source.close();
        }
    }

    private static byte[] readAllPcm16(SampleSource source) throws IOException {
        float[] sampleBuffer = new float[1024];
        byte[] pcmChunk = new byte[1024 * 2];

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            while (!source.isFinished()) {
                int read = source.read(sampleBuffer, 0, sampleBuffer.length);
                if (read <= 0) {
                    break;
                }

                encodePcm16(sampleBuffer, read, pcmChunk);
                baos.write(pcmChunk, 0, read * 2);
            }
            return baos.toByteArray();
        }
    }

    private static void encodePcm16(float[] samples, int count, byte[] pcm) {
        for (int i = 0; i < count; i++) {
            float s = samples[i];
            if (s > 1.0f) s = 1.0f;
            if (s < -1.0f) s = -1.0f;

            short value = (short) Math.round(s * 32767.0f);
            pcm[i * 2] = (byte) (value & 0xFF);
            pcm[i * 2 + 1] = (byte) ((value >>> 8) & 0xFF);
        }
    }

    private static void writeAscii(DataOutputStream out, String s) throws IOException {
        out.writeBytes(s);
    }

    private static void writeLittleEndianInt(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
        out.writeByte((value >>> 16) & 0xFF);
        out.writeByte((value >>> 24) & 0xFF);
    }

    private static void writeLittleEndianShort(DataOutputStream out, short value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
    }
}
