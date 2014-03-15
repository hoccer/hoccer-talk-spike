package com.hoccer.talk.model;

import com.hoccer.talk.crypto.AESCryptor;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

/**
 * Message objects represent messages including the
 * receiver-independent envelope
 * 
 * @author ingo
 */
@DatabaseTable(tableName="message")
public class TalkMessage {

    public static String FIELD_MESSAGE_ID  = "messageId";
    public static String FIELD_MESSAGE_TAG = "messageTag";
    public static String FIELD_SENDER_ID   = "senderId";
    public static String FIELD_BODY        = "body";
    public static String FIELD_ATTACHMENT  = "attachment";

    private String _id;

    @DatabaseField(generatedId = true)
    int dbMessageId;

    /** Server-assigned message id */
    @DatabaseField
	String messageId;

    /** Sender-assigned message tag/id */
    @DatabaseField
    String messageTag;

    /** Client id of the sender */
    @DatabaseField
	String senderId;

    /** Message salt */
    @DatabaseField
    String salt;

    /** Message body */
    @DatabaseField
	String body;

    /** Message attachment */
    @DatabaseField
    String attachment;

    /** Attachment file id (for housekeeping) */
    @DatabaseField
    String attachmentFileId;

    /** Time the message was sent */
    @DatabaseField
    Date timeSent;

    /** Total number of deliveries for this message */
    @DatabaseField
    int numDeliveries;


    public TalkMessage() {
    }

    // computes SHA256-Hmac - Message must be already encrypted or not yet decrypted
    public byte[] computeHMAC()  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            Charset utf8 = Charset.forName("UTF-8");

            // messageTag
            // md.update(getMessageTag().getBytes(utf8));

            // senderId
            md.update(getSenderId().getBytes(utf8));

            // timeSent
            String timeSent = String.valueOf(getTimeSent().getTime());
            System.out.println("timeSent=" +timeSent);
            md.update(timeSent.getBytes(utf8));

            // body
            md.update(getBody().getBytes(utf8));

            // attachment
            if(getAttachment() != null) {
                md.update(getAttachment().getBytes(utf8));
                md.update(getAttachmentFileId().getBytes(utf8));
            }
            // salt
            if (getSalt() != null && getSalt().length()>0) {
                md.update(getSalt().getBytes(utf8));
            }

            byte[] digest = md.digest();
            return digest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

    public String getMessageTag() {
        return messageTag;
    }

    public void setMessageTag(String messageTag) {
        this.messageTag = messageTag;
    }

    public String generateMessageTag() {
        this.messageTag = UUID.randomUUID().toString();
        return this.messageTag;
    }

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getAttachmentFileId() {
        return attachmentFileId;
    }

    public void setAttachmentFileId(String attachmentFileId) {
        this.attachmentFileId = attachmentFileId;
    }

    public Date getTimeSent() {
        return timeSent;
    }

    public void setTimeSent(Date timeSent) {
        this.timeSent = timeSent;
    }

    public int getNumDeliveries() {
        return numDeliveries;
    }

    public void setNumDeliveries(int numDeliveries) {
        this.numDeliveries = numDeliveries;
    }

}
