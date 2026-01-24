package org.sokybot.pk2;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public final class JMXFileInputStream extends InputStream {

    private FileChannel fileChannel;
   // private FileLock fileLock;

    private ByteBuffer buffer;

    private long position;
    private int chunkSize = 1024; //1-kb
    private int remaining = 0;

    protected JMXFileInputStream(String filePath, long pos, int size) throws IOException {
        this.fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
       // this.fileLock = this.fileChannel.lock(pos, size, true);
        this.position = pos;
        this.remaining = size;
     //   log.debug("InputStream constructed for {} at position {} with size {}" , filePath , pos , size);
    }



    private void allocateChunk() throws IOException {

        int s = Math.min(this.chunkSize, this.remaining);

        this.buffer = this.fileChannel.map(FileChannel.MapMode.READ_ONLY, this.position, s);

       // log.debug("Allocating Chunk at {} with size {}" , this.position , s);
        this.remaining -= this.buffer.capacity();
        this.position += s;


    }

    private boolean canReadMore() {
        return this.remaining > 0;
    }

    @Override
    public void close() throws IOException {
       // this.fileLock.close();
        this.fileChannel.close();
    }

    @Override
    public int read() throws IOException {
        if ((this.buffer == null || this.buffer.remaining() == 0) && canReadMore())
            allocateChunk();

        return  (this.buffer != null && this.buffer.hasRemaining()) ? this.buffer.get()& 0xff : -1;
    }
}
