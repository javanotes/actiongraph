package org.reactiveminds.actiongraph.store.mapdb;

import akka.dispatch.Envelope;
import com.squareup.tape2.QueueFile;
import org.reactiveminds.actiongraph.store.QueueStore;

import java.io.*;
import java.time.Duration;

class TapeQueue implements QueueStore {
    private final QueueFile queueFile;
    private String name;
    private EnvelopeSerializer serializer = new EnvelopeSerializer();
    private byte[] write(Envelope envelope){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try(DataOutputStream out = new DataOutputStream(bytes)){
            serializer.serialize(out, envelope);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
    private Envelope read(byte[] b){
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(b))){
            return serializer.deserialize(in, b.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    TapeQueue(String name, File parent){
        this.name = name.startsWith(".") ? name.substring(1) : name;
        try {
            queueFile = new QueueFile.Builder(new File(parent, this.name)).zero(true).build();
            System.out.println(queueFile.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean offer(Envelope envelope, Duration block) {
        try {
            queueFile.add(write(envelope));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    @Override
    public Envelope poll(boolean flush) {
        if(queueFile.isEmpty())
            return null;
        byte[] bytes;
        try {
            bytes = queueFile.peek();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if(flush){
            flush();
        }
        return read(bytes);
    }

    @Override
    public void flush() {
        if(queueFile.isEmpty())
            return;
        try {
            queueFile.remove();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int size() {
        return queueFile.size();
    }

    @Override
    public void clear() {
        try {
            queueFile.clear();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        queueFile.close();
    }
}
