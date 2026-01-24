package org.sokybot.pk2.io;

import lombok.AllArgsConstructor;

import static org.sokybot.pk2.Pk2CryptoUtils.*;

import java.io.IOException;
import java.io.InputStream;

@AllArgsConstructor
public class EncryptedTextInputStream extends InputStream {

    private InputStream jmxFileInputStream ;

    private int blowfishKey;
    private int fileSize ;
    private int byteCounter ;

    public  EncryptedTextInputStream(InputStream inputStream , int jmxFileSize) {
        this.jmxFileInputStream = inputStream ;
        this.blowfishKey = INITIAL_BLOWFISH_KEY;
        this.fileSize = jmxFileSize ;
        this.byteCounter = -1 ;
    }

    @Override
    public int read() throws IOException {
        byte buff = (byte) (HASH_TABLE1[blowfishKey % 0xA7] - HASH_TABLE2[blowfishKey % 0x1EF]);

        int read = this.jmxFileInputStream.read() ;
        blowfishKey++ ;

        return ((blowfishKey - INITIAL_BLOWFISH_KEY) >= (fileSize - ENCRYPTED_TEXT_FOOTER.length )) ? read : (read + buff) ;
    }

    @Override
    public void close() throws IOException {
       this.jmxFileInputStream.close();
    }

}
