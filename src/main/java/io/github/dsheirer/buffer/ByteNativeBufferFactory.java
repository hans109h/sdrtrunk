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

package io.github.dsheirer.buffer;

import java.nio.ByteBuffer;

/**
 * Implements a factory for creating ByteNativeBuffer instances
 */
public class ByteNativeBufferFactory implements INativeBufferFactory
{
    private static final float DC_FILTER_GAIN = 0.007f; //Normalizes DC over period of ~142 (1 / .007) buffers
    private float mAverageDc = 0.0f;

    @Override
    public INativeBuffer getBuffer(ByteBuffer samples, long timestamp)
    {
        byte[] copy = new byte[samples.capacity()];
        samples.get(copy);
        calculateDc(copy);
        return new ByteNativeBuffer(copy, timestamp, mAverageDc);
    }

    /**
     * Calculates the average DC in the sample stream so that it can be subtracted from the samples when the
     * native buffer is used.
     * @param samples containing DC offset
     */
    private void calculateDc(byte[] samples)
    {
        double dcAccumulator = 0;

        for(byte sample: samples)
        {
            dcAccumulator += (sample & 0xFF);
        }

        dcAccumulator /= samples.length;
        dcAccumulator -= 127.0f;
        dcAccumulator /= 128.0f;
        dcAccumulator -= mAverageDc;
        mAverageDc += (dcAccumulator * DC_FILTER_GAIN);
    }
}
