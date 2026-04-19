package com.shixi.memory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileBasedChatMemory implements ChatMemory {

    private final Path baseDir;

    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    private final Map<String, Object> conversationLocks = new ConcurrentHashMap<>();

    private final ThreadLocal<Kryo> kryoHolder = ThreadLocal.withInitial(this::createKryo);

    public FileBasedChatMemory(Path baseDir) {
        this.baseDir = baseDir;
        try {
            Files.createDirectories(baseDir);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to create chat memory directory: " + baseDir, e);
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String normalizedId = normalize(conversationId);
        Object lock = lockFor(normalizedId);
        synchronized (lock) {
            List<Message> history = this.conversationHistory.computeIfAbsent(normalizedId, this::loadMessages);
            history.addAll(messages);
            persistMessages(normalizedId, history);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String normalizedId = normalize(conversationId);
        Object lock = lockFor(normalizedId);
        synchronized (lock) {
            List<Message> history = this.conversationHistory.computeIfAbsent(normalizedId, this::loadMessages);
            if (history.isEmpty()) {
                return List.of();
            }
            if (lastN <= 0 || history.size() <= lastN) {
                return new ArrayList<>(history);
            }
            return new ArrayList<>(history.subList(history.size() - lastN, history.size()));
        }
    }

    @Override
    public void clear(String conversationId) {
        String normalizedId = normalize(conversationId);
        Object lock = lockFor(normalizedId);
        synchronized (lock) {
            this.conversationHistory.remove(normalizedId);
            try {
                Files.deleteIfExists(filePath(normalizedId));
            }
            catch (IOException e) {
                throw new IllegalStateException("Failed to clear memory for conversation: " + normalizedId, e);
            }
        }
        this.conversationLocks.remove(normalizedId);
    }

    private Object lockFor(String conversationId) {
        return this.conversationLocks.computeIfAbsent(conversationId, key -> new Object());
    }

    private List<Message> loadMessages(String conversationId) {
        Path path = filePath(conversationId);
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }

        try (InputStream inputStream = Files.newInputStream(path);
             Input input = new Input(inputStream)) {
            List<StoredMessage> storedMessages = kryoHolder.get().readObject(input, ArrayList.class);
            return toMessages(storedMessages);
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to load memory for conversation: " + conversationId, e);
        }
    }

    private void persistMessages(String conversationId, List<Message> history) {
        Path target = filePath(conversationId);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        List<StoredMessage> storedMessages = toStoredMessages(history);

        try (OutputStream outputStream = Files.newOutputStream(temp);
             Output output = new Output(outputStream)) {
            kryoHolder.get().writeObject(output, storedMessages);
            output.flush();
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to persist memory for conversation: " + conversationId, e);
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException atomicMoveFailure) {
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException moveFailure) {
                throw new IllegalStateException("Failed to finalize memory file for conversation: " + conversationId,
                        moveFailure);
            }
        }
    }

    private List<StoredMessage> toStoredMessages(List<Message> messages) {
        List<StoredMessage> storedMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            MessageType type = message.getMessageType() != null ? message.getMessageType() : MessageType.USER;
            storedMessages.add(new StoredMessage(type.name(), message.getText()));
        }
        return storedMessages;
    }

    private List<Message> toMessages(List<StoredMessage> storedMessages) {
        if (storedMessages == null || storedMessages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>(storedMessages.size());
        for (StoredMessage storedMessage : storedMessages) {
            if (storedMessage == null) {
                continue;
            }
            messages.add(toMessage(storedMessage));
        }
        return messages;
    }

    private Message toMessage(StoredMessage storedMessage) {
        String text = storedMessage.text != null ? storedMessage.text : "";
        MessageType messageType;
        try {
            messageType = MessageType.valueOf(storedMessage.type);
        }
        catch (Exception e) {
            messageType = MessageType.USER;
        }

        return switch (messageType) {
            case SYSTEM -> new SystemMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            default -> new UserMessage(text);
        };
    }

    private Path filePath(String conversationId) {
        return this.baseDir.resolve(safeFileName(conversationId) + ".bin");
    }

    private String normalize(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "default";
        }
        return conversationId.trim();
    }

    private String safeFileName(String conversationId) {
        return conversationId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Kryo createKryo() {
        Kryo kryo = new Kryo();
        kryo.register(ArrayList.class);
        kryo.register(StoredMessage.class);
        return kryo;
    }

    public static class StoredMessage {

        private String type;

        private String text;

        public StoredMessage() {
        }

        public StoredMessage(String type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
