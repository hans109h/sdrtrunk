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

import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.sample.ConversionUtils;
import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MP3AudioConverter implements IAudioConverter
{
    private final static Logger mLog = LoggerFactory.getLogger( MP3AudioConverter.class );
    public static final int AUDIO_QUALITY = Lame.QUALITY_LOW;
    private LameEncoder mEncoder;
    private RealResampler mResampler;
    private ByteArrayOutputStream mMP3EncodedFramesStream = new ByteArrayOutputStream();
    private byte[] mOutputFramesBuffer;

    /**
     * Converts PCM 16-bit Little Endian audio packets to Mono, MP3 compressed audio.
     *
     * @param audioSampleRate for the desired input sample rate (resampled from default 8 kHz as needed)
     * @param setting to configure the LAME encoder
     */
    public MP3AudioConverter(AudioSampleRate audioSampleRate, MP3Setting setting)
    {
        mEncoder = LameFactory.getLameEncoder(audioSampleRate, setting);

        if(audioSampleRate != AudioSampleRate.SR_8000)
        {
            mResampler = LameFactory.getResampler(audioSampleRate);
        }

        mOutputFramesBuffer = new byte[mEncoder.getPCMBufferSize()];
    }

    public List<byte[]> convert(List<float[]> audioPackets)
    {
        List<byte[]> converted = new ArrayList<>();

        if(mResampler != null)
        {
            audioPackets = mResampler.resample(audioPackets);
        }

        mLog.debug("Resampled to [" + audioPackets.size() + "] audio packets");

        for(int x = 0; x < audioPackets.size(); x++)
        {
            byte[] bytesToEncode = ConversionUtils.convertToSigned16BitSamples(audioPackets.get(x)).array();
            mLog.debug("Bytes To Encode: " + bytesToEncode.length);
            int bytesToEncodePointer = 0;

            int inputChunkSize = FastMath.min(mOutputFramesBuffer.length, bytesToEncode.length);
            int outputChunkSize = 0;

            mLog.debug("Input Chunk Size: " + inputChunkSize);
            try
            {
                while(bytesToEncodePointer < bytesToEncode.length)
                {
                    outputChunkSize = mEncoder.encodeBuffer(bytesToEncode, bytesToEncodePointer, inputChunkSize, mOutputFramesBuffer);
                    bytesToEncodePointer += inputChunkSize;
                    inputChunkSize = FastMath.min(mOutputFramesBuffer.length, bytesToEncode.length - bytesToEncodePointer);

                    mLog.debug("Output Chunk Size: " + outputChunkSize);

                    if(outputChunkSize > 0)
                    {
                        converted.add(Arrays.copyOf(mOutputFramesBuffer, outputChunkSize));
                        mLog.debug("Output Chunk Size: " + outputChunkSize);
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("There was an error converting audio to MP3: " + e.getMessage());
            }
        }

        int finalChunkSize = mEncoder.encodeFinish(mOutputFramesBuffer);

        if(finalChunkSize > 0)
        {
            converted.add(Arrays.copyOf(mOutputFramesBuffer, finalChunkSize));
            mLog.debug("Final Output Chunk Size: " + finalChunkSize);
        }

        return converted;
    }

    @Override
    public List<byte[]> flush()
    {
        byte[] lastPartialFrame = new byte[mEncoder.getMP3BufferSize()];

        int length = mEncoder.encodeFinish(lastPartialFrame);

        byte[] frame = Arrays.copyOf(lastPartialFrame, length);

        if(frame.length == 0)
        {
            return Collections.emptyList();
        }

        return Collections.singletonList(frame);
    }
}
