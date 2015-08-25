package com.hoccer.talk.filecache.transfer;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.filecache.model.CacheFile;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Date;

/**
 * Active download from the cache
 *
 * @author ingo
 */
public class CacheDownload extends CacheTransfer {

    static Logger LOG = Logger.getLogger(CacheDownload.class);

    ByteRange byteRange;
    private int mTimeout;

    public CacheDownload(CacheFile file, ByteRange range,
                         HttpServletRequest req,
                         HttpServletResponse resp,
                         int timeout) {
        super(file, req, resp);
        byteRange = range;
        mTimeout = timeout;
    }

    public void perform() throws IOException, InterruptedException {
        LOG.debug("CacheDownload.perform");
        // allocate a transfer buffer
        byte[] buffer = BufferCache.takeBuffer();

        // set content type
        httpResponse.setContentType(cacheFile.getContentType());

        // start the download
        transferBegin(Thread.currentThread());
        cacheFile.downloadStarts(this);

        try {
            // open/get streams
            OutputStream outStream = httpResponse.getOutputStream();
            RandomAccessFile inFile = cacheFile.openForRandomAccess("r");

            // determine amount of data to send
            int totalRequested = ((int) byteRange.getEnd()) - ((int) byteRange.getStart()) + 1;

            // seek forward to the requested range
            inFile.seek(byteRange.getStart());

            // loop until done
            int totalTransferred = 0;
            int absolutePosition = (int) byteRange.getStart();
            int absoluteEnd = absolutePosition + totalRequested;
            while (totalTransferred < totalRequested) {
                // abort on thread interrupt
                // TODO: this makes no sense. When reading value from interrupted() the thread is already running again.
                // The interruption refers to an older thread instance which has already terminated.
                // See CacheFile::uploadFinished() -> aborts a running download for a given file
                // after uploading the file has finished successfully.
                if (Thread.interrupted()) {
                    throw new InterruptedException("Transfer thread interrupted");
                }

                // abort when file becomes invalid
                if (!cacheFile.isAlive()) {
                    throw new InterruptedException("File no longer available");
                }

                // determine how much to transfer
                int bytesWanted = Math.min(totalRequested - totalTransferred, buffer.length);

                // determine current limit
                int limit = cacheFile.getLimit();
                int absoluteLimit = Math.min(limit, absoluteEnd);

                // wait for availability
                final long SECONDS = 1000;
                final long TIMEOUT_SECONDS = mTimeout * SECONDS;
                Date timeoutDate = new Date(new Date().getTime() + TIMEOUT_SECONDS);

                while ((absoluteLimit != cacheFile.getContentLength()) && (limit < (absolutePosition + bytesWanted))) {
                    LOG.debug("CacheDownload.perform:entering while (limit="+limit+" < "+bytesWanted+"=(absolutePosition="+absolutePosition+"+bytesWanted="+bytesWanted+")), Thread.interrupted()="+Thread.interrupted());
                    if (new Date().after(timeoutDate)) {
                        LOG.debug("CacheDownload.perform: reached timeOutdate ="+timeoutDate);
                        throw new InterruptedException("Timeout");
                    } else {
                        LOG.debug("CacheDownload.perform: not yet reached timeOutdate ="+timeoutDate);
                    }
                    Thread.sleep(100);
                    if (!cacheFile.waitForData(absoluteLimit + bytesWanted, TIMEOUT_SECONDS)) {
                        throw new InterruptedException("File no longer available");
                    }
                    limit = cacheFile.getLimit();
                    absoluteLimit = Math.min(limit, absoluteEnd);
                 }
                LOG.debug("CacheDownload.perform: passed while (limit="+limit+" < "+bytesWanted+"=(absolutePosition="+absolutePosition+"+bytesWanted="+bytesWanted+")), Thread.interrupted()="+Thread.interrupted());

                // read data from file
                int bytesRead = inFile.read(buffer, 0, bytesWanted);
                if (bytesRead == -1) {
                    LOG.debug("failed to read from file, bytesread= " + bytesRead);
                    break; // XXX
                }
                LOG.debug("writing " + bytesRead + "to output stream");
                // write to http output stream
                outStream.write(buffer, 0, bytesRead);

                // account for what we did
                totalTransferred += bytesRead;
                absolutePosition += bytesRead;
                transferProgress(bytesRead);
            }
            LOG.debug("closing inFile");

            // close file stream
            inFile.close();

        } catch (InterruptedException e) {
            cacheFile.downloadAborted(this);
            // rethrow to finish the http request
            throw e;
        } catch (IOException e) {
            // notify the file of the abort
            cacheFile.downloadAborted(this);
            // rethrow to finish the http request
            throw e;
        } finally {
            // always finish the rate estimator
            transferEnd();
            // return the transfer buffer
            BufferCache.returnBuffer(buffer);
        }
        LOG.debug("download finished");

        // we are done, tell everybody
        cacheFile.downloadFinished(this);
    }

}
