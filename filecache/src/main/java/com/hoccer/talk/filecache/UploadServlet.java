package com.hoccer.talk.filecache;

import com.google.appengine.api.blobstore.ByteRange;
import com.google.appengine.api.blobstore.RangeFormatException;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.filecache.transfer.CacheTransfer;
import com.hoccer.talk.filecache.transfer.CacheUpload;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/upload/*")
public class UploadServlet extends DownloadServlet {

    static Logger LOG = Logger.getLogger(UploadServlet.class);

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        CacheFile file = getFileForUpload(req, resp);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist");
            LOG.info("PUT " + req.getPathInfo() + " " + resp.getStatus() + " file not found, agent="
                    + CacheTransfer.getUserAgent(req)+" from "+CacheTransfer.getRemoteAddr(req));
            return;
        }

        ByteRange range = beginPut(file, req, resp);
        if(range == null) {
            LOG.info("PUT " + req.getPathInfo() + " " + resp.getStatus() + " invalid range, agent="
                    + CacheTransfer.getUserAgent(req)+" from "+CacheTransfer.getRemoteAddr(req));
            return;
        }

        LOG.info("PUT " + req.getPathInfo() + " " + resp.getStatus() + " found " + file.getFileId() + " range " + range.toContentRangeString());

        if(range.hasStart()) {
            CacheUpload upload = new CacheUpload(file, req, resp, range);

            try {
                LOG.info("PUT " + req.getPathInfo() + " --- upload started, agent "+upload.getUserAgent()+
                        " from "+upload.getRemoteAddr()+", account "+file.getAccountId()+", file "+ file.getFileId());
                upload.perform();
                LOG.info("PUT " + req.getPathInfo() + " --- upload finished, agent "+upload.getUserAgent()+
                        " from "+upload.getRemoteAddr()+", account "+file.getAccountId()+", file "+ file.getFileId());
            } catch (InterruptedException e) {
                LOG.info("PUT " + req.getPathInfo() + " --- upload interrupted, agent "+upload.getUserAgent()+
                        " from "+upload.getRemoteAddr()+", account "+file.getAccountId()+", file "+ file.getFileId());
                return;
            }
        }

        finishPut(file, req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("delete request: " + req.getPathInfo());

        CacheBackend backend = getCacheBackend();

        CacheFile file = getFileForUpload(req, resp);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return;
        }

        LOG.info("DELETE " + req.getPathInfo() + " found " + file.getFileId());

        file.delete();
    }

    private ByteRange beginPut(CacheFile file, HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        String headContentLength = req.getHeader("Content-Length");
        String headContentRange = req.getHeader("Content-Range");

        // content length is mandatory
        if(headContentLength == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length not specified");
            return null;
        }

        // parse content length
        int contentLength = Integer.parseInt(headContentLength);

        // verify the content length and try to determine file size
        if(file.getContentLength() == -1) {
            if(headContentRange == null) {
                file.setContentLength(contentLength);
            }
        } else {
            if(contentLength > file.getContentLength()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length to large for file");
                return null;
            }
        }

        // non-ranged requests get a simple OK
        if(headContentRange == null) {
            return new ByteRange(0, contentLength);
        }

        // parse the byte range
        ByteRange range = null;
        try {
            range = ByteRange.parseContentRange(headContentRange);
        } catch (RangeFormatException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad content range");
            return null;
        }

        // try again to determine the file size
        if(file.getContentLength() == -1) {
            if(range.hasTotal()) {
                file.setContentLength((int)range.getTotal());
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not determine file size");
                return null;
            }
        }

        if(range.hasStart()) {
            // fill in the end if the client didn't specify
            if(!range.hasEnd()) {
                range = new ByteRange(range.getStart(), file.getContentLength() - 1);
            }

            // verify that it makes sense
            if(range.getStart() > range.getEnd()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad range: start > end");
                return null;
            }
            if(range.getStart() < 0 || range.getEnd() < 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad range: start or end < 0");
                return null;
            }
            if(range.getStart() > file.getContentLength() || range.getEnd() > file.getContentLength()) {
                resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            // determine the length of the chunk
            long length = range.getEnd() - range.getStart() + 1;
            if(length != contentLength) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length does not match range");
                return null;
            }
        }

        return range;
    }

    private void finishPut(CacheFile file, HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentLength(0);
        LOG.debug("finishing put with range to limit " + file.getLimit() + " length " + file.getContentLength());
        if(file.getLimit() > 0) {
            resp.setHeader("Range", "bytes=0-" + (file.getLimit() - 1) + "/" + file.getContentLength());
        }
        if(file.getLimit() == file.getContentLength()) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(308); // "resume incomplete"
        }
    }

    @Override
    protected CacheFile getFileForDownload(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get all the various things we need
        CacheBackend backend = getCacheBackend();
        String pathInfo = req.getPathInfo();
        String uploadId = pathInfo.substring(1);
        // try to get by download id
        CacheFile file = backend.getByUploadId(uploadId);
        // if that fails try it as a file id
        if(file == null) {
            file = backend.getByFileId(uploadId, false);
        }
        // err if not found
        if(file == null) {
            return null;
        }
        // err if file is gone
        int fileState = file.getState();
        if(fileState == CacheFile.STATE_EXPIRED || fileState == CacheFile.STATE_DELETED) {
            return null;
        }
        // return
        return file;

    }

    protected CacheFile getFileForUpload(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get the various things we need
        CacheBackend backend = getCacheBackend();
        String pathInfo = req.getPathInfo();
        String uploadId = pathInfo.substring(1);
        // try to get by upload id
        CacheFile file = backend.getByUploadId(uploadId);
        // if not found try it as a file id
        if(file == null) {
            file = backend.getByFileId(uploadId, false);
        }
        // err if not found
        if(file == null) {
            return null;
        }
        // err if file is gone
        int fileState = file.getState();
        if(fileState == CacheFile.STATE_EXPIRED || fileState == CacheFile.STATE_DELETED) {
            return null;
        }
        // return
        return file;
    }

}
