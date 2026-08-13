package qingzhou.llm.impl;

import qingzhou.llm.Attachment;

public class ImageAttachment implements Attachment {
    public final String mimeType;
    public final String base64;

    ImageAttachment(String base64, String mimeType) {
        this.base64 = base64;
        this.mimeType = mimeType;
    }
}
