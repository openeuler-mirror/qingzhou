package qingzhou.llm;

public interface ChatModelFactory {
    ChatModel.Builder newChatModelBuilder();

    Attachment buildImageAttachment(String base64);
}
