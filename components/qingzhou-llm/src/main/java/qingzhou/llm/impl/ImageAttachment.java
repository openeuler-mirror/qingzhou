package qingzhou.llm.impl;

import qingzhou.llm.Attachment;

class ImageAttachment implements Attachment {
    final String base64;

    ImageAttachment(String base64) {
        this.base64 = base64;
    }
}
