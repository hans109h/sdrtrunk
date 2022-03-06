/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.audio.convert;

import io.github.dsheirer.audio.AudioFormats;

import javax.sound.sampled.AudioFormat;

/**
 * Enumeration of (input) audio sample rates for mono audio.
 */
public enum AudioSampleRate
{
    SR_8000(AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO, "8000 Hz (default)"),
    SR_22050(AudioFormats.PCM_SIGNED_22050HZ_16BITS_MONO, "22050 Hz"),
    SR_44100(AudioFormats.PCM_SIGNED_44100HZ_16BITS_MONO, "44100 Hz");

    private AudioFormat mAudioFormat;
    private String mLabel;

    /**
     * Constructs an instance
     * @param audioFormat for the specified sample rate
     */
    AudioSampleRate(AudioFormat audioFormat, String label)
    {
        mAudioFormat = audioFormat;
        mLabel = label;
    }

    /**
     * Default sample rate
     */
    public static AudioSampleRate getDefault()
    {
        return SR_8000;
    }

    /**
     * Audio format for the specified sample rate entry
     */
    public AudioFormat getAudioFormat()
    {
        return mAudioFormat;
    }

    /**
     * Sample rate for the format
     */
    public double getSampleRate()
    {
        return getAudioFormat().getSampleRate();
    }


    @Override
    public String toString()
    {
        return mLabel;
    }
}
