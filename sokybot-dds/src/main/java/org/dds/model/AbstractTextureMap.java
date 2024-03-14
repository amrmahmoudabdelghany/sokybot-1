/**
 * 
 */
package org.dds.model;


import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.activation.UnsupportedDataTypeException;

import org.dds.compression.DXTBufferCompressor;
import org.dds.ddsutil.PixelFormats;

import io.github.memo33.jsquish.Squish;
import io.github.memo33.jsquish.Squish.CompressionType;




/**
 * Abstract TextureMap
 * @author danielsenff
 *
 */
public abstract class AbstractTextureMap implements TextureMap {

	public AbstractTextureMap() {}
	
	@Override
	public ByteBuffer[] getDXTCompressedBuffer(final int pixelformat) 
			throws UnsupportedDataTypeException {
		
		CompressionType compressionType = PixelFormats.getSquishCompressionFormat(pixelformat);
		return this.getDXTCompressedBuffer(compressionType );
	}
	
	/**
	 * @param bi
	 * @param compressionType
	 * @return
	 */
	@Override
	public ByteBuffer compress(final BufferedImage bi, 
			final Squish.CompressionType compressionType) {
		DXTBufferCompressor compi = new DXTBufferCompressor(bi, compressionType);
		return compi.getByteBuffer();
	}

}
