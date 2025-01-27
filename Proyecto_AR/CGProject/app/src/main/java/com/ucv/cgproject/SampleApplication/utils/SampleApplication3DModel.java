/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.ucv.cgproject.SampleApplication.utils;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * This class is used to load 3D models from a .txt file
 *
 * As an example look at Buildings.txt
 * The first number in the text file corresponds to the number of vertices * 3 (x, y, z)
 * The lines are parsed until we have parsed all of the vertices
 * Then the process repeats for reading the normals and tex coords
 */
public class SampleApplication3DModel extends MeshObject
{
    
    private ByteBuffer verts;
    private ByteBuffer textCoords;
    private ByteBuffer norms;
    private int numVerts = 0;
    
    
    public void loadModel(AssetManager assetManager, String filename)
        throws IOException
    {
        InputStream is = null;
        try
        {
            is = assetManager.open(filename);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
            
            String line = reader.readLine();
            
            int floatsToRead = Integer.parseInt(line);
            numVerts = floatsToRead / 3;
            
            verts = ByteBuffer.allocateDirect(floatsToRead * 4);
            verts.order(ByteOrder.nativeOrder());
            for (int i = 0; i < floatsToRead; i++)
            {
                verts.putFloat(Float.parseFloat(reader.readLine()));
            }
            verts.rewind();
            
            line = reader.readLine();
            floatsToRead = Integer.parseInt(line);
            
            norms = ByteBuffer.allocateDirect(floatsToRead * 4);
            norms.order(ByteOrder.nativeOrder());
            for (int i = 0; i < floatsToRead; i++)
            {
                norms.putFloat(Float.parseFloat(reader.readLine()));
            }
            norms.rewind();
            
            line = reader.readLine();
            floatsToRead = Integer.parseInt(line);
            
            textCoords = ByteBuffer.allocateDirect(floatsToRead * 4);
            textCoords.order(ByteOrder.nativeOrder());
            for (int i = 0; i < floatsToRead; i++)
            {
                textCoords.putFloat(Float.parseFloat(reader.readLine()));
            }
            textCoords.rewind();
            
        } finally
        {
            if (is != null)
                is.close();
        }
    }
    
    
    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
    {
        Buffer result = null;
        switch (bufferType)
        {
            case BUFFER_TYPE_VERTEX:
                result = verts;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = textCoords;
                break;
            case BUFFER_TYPE_NORMALS:
                result = norms;
            default:
                break;
        }
        return result;
    }
    
    
    @Override
    public int getNumObjectVertex()
    {
        return numVerts;
    }
    
    
    @Override
    public int getNumObjectIndex()
    {
        return 0;
    }
    
}
