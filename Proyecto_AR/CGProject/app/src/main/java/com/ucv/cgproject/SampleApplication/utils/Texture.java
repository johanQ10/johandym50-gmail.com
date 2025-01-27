/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.ucv.cgproject.SampleApplication.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.vuforia.Image;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Support class for the Vuforia sample applications
 * Exposes functionality for loading a texture from the APK.
 */
public class Texture
{
    private static final String LOGTAG = "Vuforia_Texture";
    
    public int mWidth;          // The width of the texture.
    public int mHeight;         // The height of the texture.
    private  int mChannels;       // The number of channels.
    public ByteBuffer mData;    // The pixel data.
    public final int[] mTextureID = new int[1];
    

    public static Texture loadTextureFromApk(String fileName, AssetManager assets) {
        InputStream inputStream;
        try {
            inputStream = assets.open(fileName, AssetManager.ACCESS_BUFFER);
            
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            Bitmap bitMap = BitmapFactory.decodeStream(bufferedStream);
            
            int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
            bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0, bitMap.getWidth(), bitMap.getHeight());
            
            return loadTextureFromIntBuffer(data, bitMap.getWidth(), bitMap.getHeight());
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }
    
    
    private static Texture loadTextureFromIntBuffer(int[] data, int width, int height) {
        // Convert:
        int numPixels = width * height;
        byte[] dataBytes = new byte[numPixels * 4];
        
        for (int p = 0; p < numPixels; ++p) {
            int colour = data[p];
            dataBytes[p * 4] = (byte) (colour >>> 16); // R
            dataBytes[p * 4 + 1] = (byte) (colour >>> 8); // G
            dataBytes[p * 4 + 2] = (byte) colour; // B
            dataBytes[p * 4 + 3] = (byte) (colour >>> 24); // A
        }
        
        Texture texture = new Texture();
        texture.mWidth = width;
        texture.mHeight = height;
        texture.mChannels = 4;
        
        texture.mData = ByteBuffer.allocateDirect(dataBytes.length).order(ByteOrder.nativeOrder());
        int rowSize = texture.mWidth * texture.mChannels;
        for (int r = 0; r < texture.mHeight; r++)
            texture.mData.put(dataBytes, rowSize * (texture.mHeight - 1 - r), rowSize);
        
        texture.mData.rewind();

        return texture;
    }


    public static Texture loadTextureFromImage(Image image) {
        Texture texture = new Texture();
        texture.mWidth = image.getWidth();
        texture.mHeight = image.getHeight();
        texture.mChannels = 4;
        texture.mData = image.getPixels();

        texture.mData.rewind();

        return texture;
    }
}
