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

package io.github.dsheirer.buffer.airspy;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteBuffer;

/**
 * Scalar implementation of airspy sample converter for un-packed samples.
 */
public class VectorUnpackedSampleConverter implements IAirspySampleConverter
{
    private static final VectorSpecies<Short> VECTOR_SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final float DC_FILTER_GAIN = 0.007f; //Normalizes DC over period of ~142 (1 / .007) buffers
    private float mAverageDc = 0.0f;

    @Override
    public short[] convert(ByteBuffer buffer)
    {
        long dcAccumulator = 0;
        short sample;
        short[] samples;
        byte b1, b2;

        samples = new short[buffer.capacity() / 2];

        int bytesOffset;
        int rawPointer = 0;
        ShortVector vector;

        int samplesOffset = 0;

        short[] bytes1 = new short[VECTOR_SPECIES.length()];
        short[] bytes2 = new short[VECTOR_SPECIES.length()];

        for(; samplesOffset < VECTOR_SPECIES.loopBound(samples.length); samplesOffset += VECTOR_SPECIES.length())
        {
            bytesOffset = 0;

            while(bytesOffset < VECTOR_SPECIES.length())
            {
                bytes1[bytesOffset] = (short)(buffer.get(rawPointer++) & 0xFF);
                bytes2[bytesOffset++] = (short)(buffer.get(rawPointer++) & 0x0F);
            }

            vector = ShortVector.fromArray(VECTOR_SPECIES, bytes1, 0)
                    .or(ShortVector.fromArray(VECTOR_SPECIES, bytes2, 0).lanewise(VectorOperators.LSHL, 8));
            dcAccumulator += vector.reduceLanes(VectorOperators.ADD);
            vector.intoArray(samples, samplesOffset);
        }

        for(; samplesOffset < samples.length; samplesOffset++)
        {
            b1 = buffer.get(rawPointer++);
            b2 = buffer.get(rawPointer++);

            sample = (short)(((b2 & 0x0F) << 8) | (b1 & 0xFF));
            samples[samplesOffset] = sample;
            dcAccumulator += sample;
        }

        //Calculate the average scaled DC offset so that it can be applied in the native buffer's converted samples
        float averageDcNow = ((float)dcAccumulator / (float)samples.length) - 2048.0f;
        averageDcNow *= AirspyBufferIterator.SCALE_SIGNED_12_BIT_TO_FLOAT;
        averageDcNow -= mAverageDc;
        mAverageDc += (averageDcNow * DC_FILTER_GAIN);

        return samples;
    }

    @Override
    public float getAverageDc()
    {
        return mAverageDc;
    }
}
