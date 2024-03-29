package com.hoccer.talk.filecache.transfer;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.filecache.model.CacheFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Active upload to the cache
 *
 * @author ingo
 */
public class CacheUpload extends CacheTransfer {

    ByteRange byteRange;

    public CacheUpload(CacheFile file,
                       HttpServletRequest req,
                       HttpServletResponse resp,
                       ByteRange range) {
        super(file, req, resp);
        byteRange = range;
    }

    public void perform() throws IOException, InterruptedException {
        // allocate a transfer buffer
        byte[] buffer = BufferCache.takeBuffer();

        // start the rate estimator
        transferBegin(Thread.currentThread());
        // start the transfer
        cacheFile.uploadStarts(this);

        try {
            // get the input stream
            InputStream inStream = httpRequest.getInputStream();
            // open the file
            RandomAccessFile outFile = cacheFile.openForRandomAccess("rw");

            // determine amount of data to send
            int totalRequested = ((int) byteRange.getEnd()) - ((int) byteRange.getStart()) + 1;

            // adjust file size to content length
            outFile.setLength(cacheFile.getContentLength());

            // seek to upload start position
            outFile.seek(byteRange.getStart());

            // perform the upload
            int totalTransferred = 0;
            int absolutePosition = (int) byteRange.getStart();
            while (totalTransferred < totalRequested) {
                // allow thread interruption
                // TODO: this makes no sense. When reading value from interrupted() the thread is already running again.
                // The interruption refers to an older thread instance which has already terminated.
                // See CacheFile::uploadFinished() -> aborts a running download for a given file
                // after uploading the file has finished successfully.
                if (Thread.interrupted()) {
                    throw new InterruptedException("Transfer thread interrupted");
                }

                // abort when the file reaches an invalid state
                if (!cacheFile.isAlive()) {
                    throw new InterruptedException("File no longer available");
                }

                // read a chunk from input stream
                int bytesRead = inStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }

                // write chunk out to file
                outFile.write(buffer, 0, bytesRead);

                // adjust position variables
                totalTransferred += bytesRead;
                absolutePosition += bytesRead;

                // inform rate estimator
                transferProgress(bytesRead);

                // update the files limit
                cacheFile.updateLimit(absolutePosition, outFile);
            }

            // do a final sync
            outFile.getFD().sync();

            // we are done, close the file
            outFile.close();

        } catch (IOException e) {
            cacheFile.uploadAborted(this);
            // rethrow to finish http request
            throw e;
        } catch (InterruptedException e) {
            cacheFile.uploadAborted(this);
            // rethrow to finish http request
            throw e;
        } finally {
            // always finish the rate estimator
            transferEnd();
            // return the transfer buffer
            BufferCache.returnBuffer(buffer);
        }

        // we are done, tell everybody
        cacheFile.uploadFinished(this);
    }

}
